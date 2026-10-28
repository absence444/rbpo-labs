package ru.rbpo.museum.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.rbpo.museum.exception.BadRequestException;
import ru.rbpo.museum.exception.ConflictException;
import ru.rbpo.museum.exception.NotFoundException;
import ru.rbpo.museum.model.ExhibitPlacement;
import ru.rbpo.museum.model.Exhibition;
import ru.rbpo.museum.model.ExhibitionHall;
import ru.rbpo.museum.model.ExhibitionStatus;
import ru.rbpo.museum.model.MountingStatus;
import ru.rbpo.museum.model.User;
import ru.rbpo.museum.repository.ExhibitPlacementRepository;
import ru.rbpo.museum.repository.ExhibitionRepository;
import ru.rbpo.museum.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class ExhibitionService {

    private final ExhibitionRepository exhibitionRepository;
    private final UserRepository userRepository;
    private final ExhibitPlacementRepository placementRepository;
    private final HallCapacityService hallCapacityService;
    private final Clock clock;

    public ExhibitionService(ExhibitionRepository exhibitionRepository, UserRepository userRepository,
                             ExhibitPlacementRepository placementRepository, HallCapacityService hallCapacityService,
                             Clock clock) {
        this.exhibitionRepository = exhibitionRepository;
        this.userRepository = userRepository;
        this.placementRepository = placementRepository;
        this.hallCapacityService = hallCapacityService;
        this.clock = clock;
    }

    public List<Exhibition> findAll() {
        return exhibitionRepository.findAll();
    }

    public Exhibition findById(Long id) {
        return exhibitionRepository.findById(id).orElseThrow(() -> new NotFoundException("Выставка", id));
    }

    /** Новая выставка всегда создаётся в статусе PLANNED; статус меняется только бизнес-операциями. */
    @Transactional
    public Exhibition create(Exhibition exhibition, Long curatorId) {
        ensureDatesValid(exhibition.getStartDate(), exhibition.getEndDate());
        if (exhibition.getEndDate().isBefore(LocalDate.now(clock))) {
            throw new BadRequestException("Нельзя запланировать выставку, которая уже закончилась");
        }
        if (curatorId != null) {
            exhibition.setCurator(findCurator(curatorId));
        }
        Instant now = Instant.now(clock);
        exhibition.setId(null);
        exhibition.setStatus(ExhibitionStatus.PLANNED);
        exhibition.setCreatedAt(now);
        exhibition.setUpdatedAt(now);
        return exhibitionRepository.save(exhibition);
    }

    @Transactional
    public Exhibition update(Long id, Exhibition changes, Long curatorId) {
        Exhibition exhibition = findById(id);
        ensureEditable(exhibition);
        boolean datesChanged = changes.getStartDate() != null || changes.getEndDate() != null;
        if (datesChanged) {
            if (exhibition.getStatus() != ExhibitionStatus.PLANNED) {
                throw new ConflictException("Даты можно менять только у запланированной выставки");
            }
            LocalDate startDate = changes.getStartDate() != null ? changes.getStartDate() : exhibition.getStartDate();
            LocalDate endDate = changes.getEndDate() != null ? changes.getEndDate() : exhibition.getEndDate();
            ensureDatesValid(startDate, endDate);
            exhibition.setStartDate(startDate);
            exhibition.setEndDate(endDate);
            // Новый период может пересечься с другими выставками в тех же залах.
            for (ExhibitPlacement placement : activePlacements(id)) {
                ExhibitionHall hall = hallCapacityService.lockHall(placement.getHall().getId());
                hallCapacityService.ensureCapacity(hall, exhibition, placement.getExhibit().getRequiredArea(),
                        placement.getId());
            }
        }
        if (curatorId != null) {
            exhibition.setCurator(findCurator(curatorId));
        }
        if (changes.getTitle() != null) {
            exhibition.setTitle(changes.getTitle());
        }
        exhibition.setUpdatedAt(Instant.now(clock));
        return exhibition;
    }

    /** Удаляет выставку вместе с размещениями. Открытую выставку удалить нельзя — сначала её нужно закрыть. */
    @Transactional
    public void delete(Long id) {
        Exhibition exhibition = findById(id);
        if (exhibition.getStatus() == ExhibitionStatus.ACTIVE) {
            throw new ConflictException("Нельзя удалить открытую выставку; сначала закройте её");
        }
        placementRepository.deleteAll(placementRepository.findByExhibitionId(id));
        exhibitionRepository.delete(exhibition);
    }

    /**
     * Бизнес-операция «Открыть выставку».
     * <p>Допустима только для запланированной выставки с назначенным куратором, если сегодняшняя дата входит
     * в период проведения, на выставке есть экспонаты и все они смонтированы.
     */
    @Transactional
    public Exhibition open(Long id) {
        Exhibition exhibition = findById(id);
        if (exhibition.getStatus() != ExhibitionStatus.PLANNED) {
            throw new ConflictException("Открыть можно только запланированную выставку (текущий статус: "
                    + exhibition.getStatus() + ")");
        }
        if (exhibition.getCurator() == null) {
            throw new ConflictException("Нельзя открыть выставку без куратора");
        }
        LocalDate today = LocalDate.now(clock);
        if (today.isBefore(exhibition.getStartDate()) || today.isAfter(exhibition.getEndDate())) {
            throw new ConflictException("Выставку можно открыть только в период проведения: "
                    + exhibition.getStartDate() + " — " + exhibition.getEndDate());
        }
        List<ExhibitPlacement> placements = activePlacements(id);
        if (placements.isEmpty()) {
            throw new ConflictException("На выставке нет ни одного экспоната");
        }
        long notMounted = placements.stream()
                .filter(placement -> placement.getMountingStatus() != MountingStatus.MOUNTED)
                .count();
        if (notMounted > 0) {
            throw new ConflictException("Не смонтировано экспонатов: " + notMounted);
        }
        Instant now = Instant.now(clock);
        exhibition.setStatus(ExhibitionStatus.ACTIVE);
        exhibition.setOpenedAt(now);
        exhibition.setUpdatedAt(now);
        return exhibition;
    }

    /**
     * Бизнес-операция «Закрыть выставку».
     * <p>Допустима только для открытой выставки. Все экспонаты демонтируются (залы освобождаются),
     * при досрочном закрытии дата окончания переносится на сегодня.
     */
    @Transactional
    public Exhibition close(Long id) {
        Exhibition exhibition = findById(id);
        if (exhibition.getStatus() != ExhibitionStatus.ACTIVE) {
            throw new ConflictException("Закрыть можно только открытую выставку (текущий статус: "
                    + exhibition.getStatus() + ")");
        }
        Instant now = Instant.now(clock);
        dismantleAll(id, now);
        LocalDate today = LocalDate.now(clock);
        if (today.isBefore(exhibition.getEndDate())) {
            exhibition.setEndDate(today);
        }
        exhibition.setStatus(ExhibitionStatus.COMPLETED);
        exhibition.setClosedAt(now);
        exhibition.setUpdatedAt(now);
        return exhibition;
    }

    /**
     * Бизнес-операция «Отменить выставку».
     * <p>Допустима только для запланированной выставки; размещения демонтируются, площадь залов освобождается.
     */
    @Transactional
    public Exhibition cancel(Long id) {
        Exhibition exhibition = findById(id);
        if (exhibition.getStatus() != ExhibitionStatus.PLANNED) {
            throw new ConflictException("Отменить можно только запланированную выставку (текущий статус: "
                    + exhibition.getStatus() + ")");
        }
        Instant now = Instant.now(clock);
        dismantleAll(id, now);
        exhibition.setStatus(ExhibitionStatus.CANCELLED);
        exhibition.setUpdatedAt(now);
        return exhibition;
    }

    private void dismantleAll(Long exhibitionId, Instant now) {
        for (ExhibitPlacement placement : activePlacements(exhibitionId)) {
            placement.setMountingStatus(MountingStatus.DISMANTLED);
            placement.setMountedAt(null);
            placement.setUpdatedAt(now);
        }
    }

    private List<ExhibitPlacement> activePlacements(Long exhibitionId) {
        return placementRepository.findByExhibitionId(exhibitionId).stream()
                .filter(placement -> placement.getMountingStatus() != MountingStatus.DISMANTLED)
                .toList();
    }

    private User findCurator(Long curatorId) {
        return userRepository.findById(curatorId)
                .orElseThrow(() -> new NotFoundException("Пользователь (куратор)", curatorId));
    }

    private static void ensureEditable(Exhibition exhibition) {
        if (exhibition.getStatus() == ExhibitionStatus.COMPLETED || exhibition.getStatus() == ExhibitionStatus.CANCELLED) {
            throw new ConflictException("Завершённую или отменённую выставку изменять нельзя");
        }
    }

    private static void ensureDatesValid(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("Дата окончания выставки не может быть раньше даты начала");
        }
    }
}
