package ru.rbpo.museum.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.rbpo.museum.exception.BadRequestException;
import ru.rbpo.museum.exception.ConflictException;
import ru.rbpo.museum.exception.NotFoundException;
import ru.rbpo.museum.model.Exhibit;
import ru.rbpo.museum.model.ExhibitCondition;
import ru.rbpo.museum.model.ExhibitPlacement;
import ru.rbpo.museum.model.Exhibition;
import ru.rbpo.museum.model.ExhibitionHall;
import ru.rbpo.museum.model.ExhibitionStatus;
import ru.rbpo.museum.model.MountingStatus;
import ru.rbpo.museum.repository.ExhibitPlacementRepository;
import ru.rbpo.museum.repository.ExhibitRepository;
import ru.rbpo.museum.repository.ExhibitionRepository;

@Service
@Transactional(readOnly = true)
public class ExhibitPlacementService {

    private final ExhibitPlacementRepository placementRepository;
    private final ExhibitRepository exhibitRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final HallCapacityService hallCapacityService;
    private final Clock clock;

    public ExhibitPlacementService(ExhibitPlacementRepository placementRepository, ExhibitRepository exhibitRepository,
                                   ExhibitionRepository exhibitionRepository, HallCapacityService hallCapacityService,
                                   Clock clock) {
        this.placementRepository = placementRepository;
        this.exhibitRepository = exhibitRepository;
        this.exhibitionRepository = exhibitionRepository;
        this.hallCapacityService = hallCapacityService;
        this.clock = clock;
    }

    public List<ExhibitPlacement> findAll() {
        return placementRepository.findAll();
    }

    public ExhibitPlacement findById(Long id) {
        return placementRepository.findById(id).orElseThrow(() -> new NotFoundException("Размещение экспоната", id));
    }

    /** Планирует размещение экспоната на выставке в зале (статус PLANNED). */
    @Transactional
    public ExhibitPlacement create(Long exhibitId, Long exhibitionId, Long hallId) {
        Exhibit exhibit = findExhibit(exhibitId);
        Exhibition exhibition = findExhibition(exhibitionId);
        ensureExhibitionAcceptsChanges(exhibition);
        if (placementRepository.existsByExhibitIdAndExhibitionId(exhibitId, exhibitionId)) {
            throw new ConflictException("Экспонат уже размещён на этой выставке");
        }
        ExhibitionHall hall = hallCapacityService.lockHall(hallId);
        ensureHallAvailable(hall);
        hallCapacityService.ensureCapacity(hall, exhibition, exhibit.getRequiredArea(), null);

        ExhibitPlacement placement = new ExhibitPlacement();
        placement.setExhibit(exhibit);
        placement.setExhibition(exhibition);
        placement.setHall(hall);
        placement.setMountingStatus(MountingStatus.PLANNED);
        Instant now = Instant.now(clock);
        placement.setCreatedAt(now);
        placement.setUpdatedAt(now);
        return placementRepository.save(placement);
    }

    /**
     * Изменяет запланированное размещение. Через PATCH доступны только статусы PLANNED и IN_PROGRESS;
     * монтаж, перенос и демонтаж выполняются бизнес-операциями.
     */
    @Transactional
    public ExhibitPlacement update(Long id, Long exhibitId, Long exhibitionId, Long hallId,
                                   MountingStatus mountingStatus) {
        ExhibitPlacement placement = findById(id);
        if (placement.getMountingStatus() == MountingStatus.MOUNTED
                || placement.getMountingStatus() == MountingStatus.DISMANTLED) {
            throw new ConflictException("Смонтированное или демонтированное размещение изменять нельзя");
        }
        if (mountingStatus == MountingStatus.MOUNTED || mountingStatus == MountingStatus.DISMANTLED) {
            throw new BadRequestException("Статус " + mountingStatus
                    + " устанавливается бизнес-операциями (mount, закрытие выставки)");
        }
        Exhibit exhibit = exhibitId != null ? findExhibit(exhibitId) : placement.getExhibit();
        Exhibition exhibition = exhibitionId != null ? findExhibition(exhibitionId) : placement.getExhibition();
        ensureExhibitionAcceptsChanges(exhibition);
        boolean sameAssignment = exhibit.getId().equals(placement.getExhibit().getId())
                && exhibition.getId().equals(placement.getExhibition().getId());
        if (!sameAssignment && placementRepository.existsByExhibitIdAndExhibitionId(exhibit.getId(), exhibition.getId())) {
            throw new ConflictException("Экспонат уже размещён на этой выставке");
        }
        Long targetHallId = hallId != null ? hallId : placement.getHall().getId();
        ExhibitionHall hall = hallCapacityService.lockHall(targetHallId);
        if (!hall.getId().equals(placement.getHall().getId())) {
            ensureHallAvailable(hall);
        }
        hallCapacityService.ensureCapacity(hall, exhibition, exhibit.getRequiredArea(), id);

        placement.setExhibit(exhibit);
        placement.setExhibition(exhibition);
        placement.setHall(hall);
        if (mountingStatus != null) {
            placement.setMountingStatus(mountingStatus);
        }
        placement.setUpdatedAt(Instant.now(clock));
        return placement;
    }

    /** Удаляет размещение. Экспонат, выставленный на открытой выставке, снять удалением нельзя. */
    @Transactional
    public void delete(Long id) {
        ExhibitPlacement placement = findById(id);
        if (placement.getMountingStatus() == MountingStatus.MOUNTED
                && placement.getExhibition().getStatus() == ExhibitionStatus.ACTIVE) {
            throw new ConflictException("Нельзя удалить размещение экспоната на открытой выставке");
        }
        placementRepository.delete(placement);
    }

    /**
     * Бизнес-операция «Смонтировать экспонат».
     * <p>Допустима для размещения в статусе PLANNED/IN_PROGRESS, если выставка запланирована или открыта,
     * зал доступен, а экспонат не требует реставрации.
     */
    @Transactional
    public ExhibitPlacement mount(Long id) {
        ExhibitPlacement placement = findById(id);
        if (placement.getMountingStatus() == MountingStatus.MOUNTED) {
            throw new ConflictException("Экспонат уже смонтирован");
        }
        if (placement.getMountingStatus() == MountingStatus.DISMANTLED) {
            throw new ConflictException("Размещение демонтировано; создайте новое размещение");
        }
        ensureExhibitionAcceptsChanges(placement.getExhibition());
        if (placement.getExhibit().getCondition() == ExhibitCondition.NEEDS_RESTORATION) {
            throw new ConflictException("Экспонат '" + placement.getExhibit().getName() + "' требует реставрации");
        }
        ExhibitionHall hall = hallCapacityService.lockHall(placement.getHall().getId());
        ensureHallAvailable(hall);
        Instant now = Instant.now(clock);
        placement.setMountingStatus(MountingStatus.MOUNTED);
        placement.setMountedAt(now);
        placement.setUpdatedAt(now);
        return placement;
    }

    /**
     * Бизнес-операция «Перенести экспонат в другой зал».
     * <p>Недопустима для демонтированных размещений и для экспонатов, выставленных на открытой выставке.
     * Целевой зал должен быть доступен и вмещать экспонат; после переноса требуется повторный монтаж.
     */
    @Transactional
    public ExhibitPlacement move(Long id, Long targetHallId) {
        ExhibitPlacement placement = findById(id);
        if (placement.getMountingStatus() == MountingStatus.DISMANTLED) {
            throw new ConflictException("Демонтированное размещение перенести нельзя");
        }
        Exhibition exhibition = placement.getExhibition();
        ensureExhibitionAcceptsChanges(exhibition);
        if (placement.getMountingStatus() == MountingStatus.MOUNTED && exhibition.getStatus() == ExhibitionStatus.ACTIVE) {
            throw new ConflictException("Нельзя переносить экспонат во время работы выставки");
        }
        if (placement.getHall().getId().equals(targetHallId)) {
            throw new BadRequestException("Экспонат уже находится в этом зале");
        }
        ExhibitionHall target = hallCapacityService.lockHall(targetHallId);
        ensureHallAvailable(target);
        hallCapacityService.ensureCapacity(target, exhibition, placement.getExhibit().getRequiredArea(), id);

        placement.setHall(target);
        placement.setMountingStatus(MountingStatus.PLANNED);
        placement.setMountedAt(null);
        placement.setUpdatedAt(Instant.now(clock));
        return placement;
    }

    private Exhibit findExhibit(Long id) {
        return exhibitRepository.findById(id).orElseThrow(() -> new NotFoundException("Экспонат", id));
    }

    private Exhibition findExhibition(Long id) {
        return exhibitionRepository.findById(id).orElseThrow(() -> new NotFoundException("Выставка", id));
    }

    private static void ensureExhibitionAcceptsChanges(Exhibition exhibition) {
        if (exhibition.getStatus() != ExhibitionStatus.PLANNED && exhibition.getStatus() != ExhibitionStatus.ACTIVE) {
            throw new ConflictException("Выставка '" + exhibition.getTitle() + "' завершена или отменена");
        }
    }

    private static void ensureHallAvailable(ExhibitionHall hall) {
        if (!hall.getAvailable()) {
            throw new ConflictException("Зал '" + hall.getName() + "' недоступен для размещения экспонатов");
        }
    }
}
