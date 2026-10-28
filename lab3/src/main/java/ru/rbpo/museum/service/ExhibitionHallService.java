package ru.rbpo.museum.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.rbpo.museum.exception.ConflictException;
import ru.rbpo.museum.exception.NotFoundException;
import ru.rbpo.museum.model.ExhibitPlacement;
import ru.rbpo.museum.model.ExhibitionHall;
import ru.rbpo.museum.model.ExhibitionStatus;
import ru.rbpo.museum.model.MountingStatus;
import ru.rbpo.museum.repository.ExhibitPlacementRepository;
import ru.rbpo.museum.repository.ExhibitionHallRepository;

@Service
@Transactional(readOnly = true)
public class ExhibitionHallService {

    private final ExhibitionHallRepository hallRepository;
    private final ExhibitPlacementRepository placementRepository;
    private final HallCapacityService hallCapacityService;
    private final Clock clock;

    public ExhibitionHallService(ExhibitionHallRepository hallRepository, ExhibitPlacementRepository placementRepository,
                                 HallCapacityService hallCapacityService, Clock clock) {
        this.hallRepository = hallRepository;
        this.placementRepository = placementRepository;
        this.hallCapacityService = hallCapacityService;
        this.clock = clock;
    }

    public List<ExhibitionHall> findAll() {
        return hallRepository.findAll();
    }

    public ExhibitionHall findById(Long id) {
        return hallRepository.findById(id).orElseThrow(() -> new NotFoundException("Экспозиционный зал", id));
    }

    @Transactional
    public ExhibitionHall create(ExhibitionHall hall) {
        ensureNameFree(hall.getName(), null);
        if (hall.getAvailable() == null) {
            hall.setAvailable(Boolean.TRUE);
        }
        Instant now = Instant.now(clock);
        hall.setId(null);
        hall.setCreatedAt(now);
        hall.setUpdatedAt(now);
        return hallRepository.save(hall);
    }

    @Transactional
    public ExhibitionHall update(Long id, ExhibitionHall changes) {
        ExhibitionHall hall = hallCapacityService.lockHall(id);
        if (changes.getName() != null) {
            ensureNameFree(changes.getName(), id);
            hall.setName(changes.getName());
        }
        if (changes.getArea() != null && changes.getArea() < hall.getArea()) {
            // Площадь можно уменьшить, только если размещённые экспонаты по-прежнему помещаются.
            for (ExhibitPlacement placement : activePlacements(id)) {
                double occupied = hallCapacityService.occupiedArea(hall, placement.getExhibition(), null);
                if (occupied > changes.getArea()) {
                    throw new ConflictException("Нельзя уменьшить площадь зала до " + changes.getArea()
                            + " м²: экспонаты выставки '" + placement.getExhibition().getTitle() + "' занимают "
                            + occupied + " м²");
                }
            }
        }
        if (changes.getArea() != null) {
            hall.setArea(changes.getArea());
        }
        if (Boolean.FALSE.equals(changes.getAvailable()) && hasExhibitsOnDisplay(id)) {
            throw new ConflictException("Нельзя закрыть зал, в котором идёт открытая выставка");
        }
        if (changes.getAvailable() != null) {
            hall.setAvailable(changes.getAvailable());
        }
        hall.setUpdatedAt(Instant.now(clock));
        return hall;
    }

    /** Удаляет зал вместе с размещениями. Зал с экспонатами открытой выставки удалить нельзя. */
    @Transactional
    public void delete(Long id) {
        ExhibitionHall hall = findById(id);
        if (hasExhibitsOnDisplay(id)) {
            throw new ConflictException("Нельзя удалить зал, в котором идёт открытая выставка");
        }
        placementRepository.deleteAll(placementRepository.findByHallId(id));
        hallRepository.delete(hall);
    }

    private boolean hasExhibitsOnDisplay(Long hallId) {
        return placementRepository.existsByHallIdAndMountingStatusAndExhibitionStatus(
                hallId, MountingStatus.MOUNTED, ExhibitionStatus.ACTIVE);
    }

    private List<ExhibitPlacement> activePlacements(Long hallId) {
        return placementRepository.findByHallId(hallId).stream()
                .filter(placement -> placement.getMountingStatus() != MountingStatus.DISMANTLED)
                .filter(placement -> HallCapacityService.OCCUPYING_STATUSES.contains(placement.getExhibition().getStatus()))
                .toList();
    }

    private void ensureNameFree(String name, Long ownerId) {
        hallRepository.findByName(name)
                .filter(existing -> !existing.getId().equals(ownerId))
                .ifPresent(existing -> {
                    throw new ConflictException("Зал с названием '" + name + "' уже существует");
                });
    }
}
