package ru.rbpo.museum.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import ru.rbpo.museum.exception.ConflictException;
import ru.rbpo.museum.exception.NotFoundException;
import ru.rbpo.museum.model.Exhibit;
import ru.rbpo.museum.model.ExhibitPlacement;
import ru.rbpo.museum.model.ExhibitionHall;
import ru.rbpo.museum.model.MountingStatus;
import ru.rbpo.museum.repository.ExhibitPlacementRepository;
import ru.rbpo.museum.repository.ExhibitRepository;
import ru.rbpo.museum.repository.ExhibitionHallRepository;
import ru.rbpo.museum.repository.ExhibitionRepository;

@Service
public class ExhibitPlacementService {

    private final ExhibitPlacementRepository placementRepository;
    private final ExhibitRepository exhibitRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final ExhibitionHallRepository hallRepository;
    private final Clock clock;

    public ExhibitPlacementService(ExhibitPlacementRepository placementRepository, ExhibitRepository exhibitRepository,
                                   ExhibitionRepository exhibitionRepository, ExhibitionHallRepository hallRepository,
                                   Clock clock) {
        this.placementRepository = placementRepository;
        this.exhibitRepository = exhibitRepository;
        this.exhibitionRepository = exhibitionRepository;
        this.hallRepository = hallRepository;
        this.clock = clock;
    }

    public List<ExhibitPlacement> findAll() {
        return placementRepository.findAll();
    }

    public ExhibitPlacement findById(Long id) {
        return placementRepository.findById(id).orElseThrow(() -> new NotFoundException("Размещение экспоната", id));
    }

    public ExhibitPlacement create(ExhibitPlacement placement) {
        if (placement.getMountingStatus() == null) {
            placement.setMountingStatus(MountingStatus.PLANNED);
        }
        validate(placement, null, true);
        Instant now = Instant.now(clock);
        placement.setId(null);
        placement.setCreatedAt(now);
        placement.setUpdatedAt(now);
        return placementRepository.save(placement);
    }

    public ExhibitPlacement update(Long id, ExhibitPlacement changes) {
        ExhibitPlacement placement = findById(id);
        ExhibitPlacement candidate = new ExhibitPlacement();
        candidate.setExhibitId(Objects.requireNonNullElse(changes.getExhibitId(), placement.getExhibitId()));
        candidate.setExhibitionId(Objects.requireNonNullElse(changes.getExhibitionId(), placement.getExhibitionId()));
        candidate.setHallId(Objects.requireNonNullElse(changes.getHallId(), placement.getHallId()));
        candidate.setMountingStatus(Objects.requireNonNullElse(changes.getMountingStatus(), placement.getMountingStatus()));
        boolean hallChanged = !candidate.getHallId().equals(placement.getHallId());
        validate(candidate, id, hallChanged);

        placement.setExhibitId(candidate.getExhibitId());
        placement.setExhibitionId(candidate.getExhibitionId());
        placement.setHallId(candidate.getHallId());
        placement.setMountingStatus(candidate.getMountingStatus());
        placement.setUpdatedAt(Instant.now(clock));
        return placementRepository.save(placement);
    }

    public void delete(Long id) {
        findById(id);
        placementRepository.deleteById(id);
    }

    /**
     * Суммарная площадь экспонатов, размещённых в зале на выставке (демонтированные не учитываются).
     *
     * @param excludePlacementId размещение, которое не нужно учитывать (например, изменяемое)
     */
    public double occupiedArea(Long hallId, Long exhibitionId, Long excludePlacementId) {
        return placementRepository.findByHallIdAndExhibitionId(hallId, exhibitionId).stream()
                .filter(placement -> !placement.getId().equals(excludePlacementId))
                .filter(placement -> placement.getMountingStatus() != MountingStatus.DISMANTLED)
                .map(placement -> exhibitRepository.findById(placement.getExhibitId()))
                .flatMap(Optional::stream)
                .mapToDouble(Exhibit::getRequiredArea)
                .sum();
    }

    /** Проверяет, что в зале хватит места для экспоната указанной площади. */
    public void ensureHallCapacity(Long hallId, Long exhibitionId, double requiredArea, Long excludePlacementId) {
        ExhibitionHall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new NotFoundException("Экспозиционный зал", hallId));
        double occupied = occupiedArea(hallId, exhibitionId, excludePlacementId);
        if (occupied + requiredArea > hall.getArea()) {
            throw new ConflictException("Недостаточно площади в зале '" + hall.getName() + "': свободно "
                    + (hall.getArea() - occupied) + " м², требуется " + requiredArea + " м²");
        }
    }

    private void validate(ExhibitPlacement placement, Long placementId, boolean checkHallAvailability) {
        Exhibit exhibit = exhibitRepository.findById(placement.getExhibitId())
                .orElseThrow(() -> new NotFoundException("Экспонат", placement.getExhibitId()));
        if (!exhibitionRepository.existsById(placement.getExhibitionId())) {
            throw new NotFoundException("Выставка", placement.getExhibitionId());
        }
        ExhibitionHall hall = hallRepository.findById(placement.getHallId())
                .orElseThrow(() -> new NotFoundException("Экспозиционный зал", placement.getHallId()));
        if (checkHallAvailability && !hall.getAvailable()) {
            throw new ConflictException("Зал '" + hall.getName() + "' недоступен для размещения экспонатов");
        }
        boolean alreadyPlaced = placementRepository.findByExhibitionId(placement.getExhibitionId()).stream()
                .anyMatch(other -> other.getExhibitId().equals(placement.getExhibitId())
                        && !other.getId().equals(placementId));
        if (alreadyPlaced) {
            throw new ConflictException("Экспонат уже размещён на этой выставке");
        }
        if (placement.getMountingStatus() != MountingStatus.DISMANTLED) {
            ensureHallCapacity(hall.getId(), placement.getExhibitionId(), exhibit.getRequiredArea(), placementId);
        }
    }
}
