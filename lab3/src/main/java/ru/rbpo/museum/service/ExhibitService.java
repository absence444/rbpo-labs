package ru.rbpo.museum.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.rbpo.museum.exception.ConflictException;
import ru.rbpo.museum.exception.NotFoundException;
import ru.rbpo.museum.model.Exhibit;
import ru.rbpo.museum.model.ExhibitCondition;
import ru.rbpo.museum.model.ExhibitPlacement;
import ru.rbpo.museum.model.ExhibitionHall;
import ru.rbpo.museum.model.ExhibitionStatus;
import ru.rbpo.museum.model.MountingStatus;
import ru.rbpo.museum.repository.ExhibitPlacementRepository;
import ru.rbpo.museum.repository.ExhibitRepository;

@Service
@Transactional(readOnly = true)
public class ExhibitService {

    private final ExhibitRepository exhibitRepository;
    private final ExhibitPlacementRepository placementRepository;
    private final HallCapacityService hallCapacityService;
    private final Clock clock;

    public ExhibitService(ExhibitRepository exhibitRepository, ExhibitPlacementRepository placementRepository,
                          HallCapacityService hallCapacityService, Clock clock) {
        this.exhibitRepository = exhibitRepository;
        this.placementRepository = placementRepository;
        this.hallCapacityService = hallCapacityService;
        this.clock = clock;
    }

    public List<Exhibit> findAll() {
        return exhibitRepository.findAll();
    }

    public Exhibit findById(Long id) {
        return exhibitRepository.findById(id).orElseThrow(() -> new NotFoundException("Экспонат", id));
    }

    @Transactional
    public Exhibit create(Exhibit exhibit) {
        Instant now = Instant.now(clock);
        exhibit.setId(null);
        exhibit.setCreatedAt(now);
        exhibit.setUpdatedAt(now);
        return exhibitRepository.save(exhibit);
    }

    @Transactional
    public Exhibit update(Long id, Exhibit changes) {
        Exhibit exhibit = findById(id);
        if (changes.getRequiredArea() != null && !changes.getRequiredArea().equals(exhibit.getRequiredArea())) {
            // Новая площадь не должна переполнить залы, где экспонат уже размещён.
            for (ExhibitPlacement placement : activePlacements(id)) {
                ExhibitionHall hall = hallCapacityService.lockHall(placement.getHall().getId());
                hallCapacityService.ensureCapacity(hall, placement.getExhibition(), changes.getRequiredArea(),
                        placement.getId());
            }
            exhibit.setRequiredArea(changes.getRequiredArea());
        }
        if (changes.getName() != null) {
            exhibit.setName(changes.getName());
        }
        if (changes.getCondition() != null) {
            exhibit.setCondition(changes.getCondition());
        }
        exhibit.setUpdatedAt(Instant.now(clock));
        return exhibit;
    }

    /** Удаляет экспонат вместе с размещениями. Экспонат, выставленный на открытой выставке, удалить нельзя. */
    @Transactional
    public void delete(Long id) {
        Exhibit exhibit = findById(id);
        ensureNotOnDisplay(id, "удалить");
        placementRepository.deleteAll(placementRepository.findByExhibitId(id));
        exhibitRepository.delete(exhibit);
    }

    /**
     * Бизнес-операция «Отправить экспонат на реставрацию».
     * <p>Недопустима, если экспонат выставлен на открытой выставке. Состояние экспоната меняется на
     * NEEDS_RESTORATION, а его размещения на запланированных выставках демонтируются (освобождают площадь залов).
     */
    @Transactional
    public Exhibit sendToRestoration(Long id) {
        Exhibit exhibit = findById(id);
        if (exhibit.getCondition() == ExhibitCondition.NEEDS_RESTORATION) {
            throw new ConflictException("Экспонат уже ожидает реставрации");
        }
        ensureNotOnDisplay(id, "отправить на реставрацию");
        Instant now = Instant.now(clock);
        for (ExhibitPlacement placement : activePlacements(id)) {
            placement.setMountingStatus(MountingStatus.DISMANTLED);
            placement.setMountedAt(null);
            placement.setUpdatedAt(now);
        }
        exhibit.setCondition(ExhibitCondition.NEEDS_RESTORATION);
        exhibit.setUpdatedAt(now);
        return exhibit;
    }

    /** Недемонтированные размещения экспоната на незавершённых выставках. */
    private List<ExhibitPlacement> activePlacements(Long exhibitId) {
        return placementRepository.findByExhibitId(exhibitId).stream()
                .filter(placement -> placement.getMountingStatus() != MountingStatus.DISMANTLED)
                .filter(placement -> HallCapacityService.OCCUPYING_STATUSES.contains(placement.getExhibition().getStatus()))
                .toList();
    }

    private void ensureNotOnDisplay(Long exhibitId, String action) {
        if (placementRepository.existsByExhibitIdAndMountingStatusAndExhibitionStatus(
                exhibitId, MountingStatus.MOUNTED, ExhibitionStatus.ACTIVE)) {
            throw new ConflictException("Нельзя " + action + " экспонат, выставленный на открытой выставке");
        }
    }
}
