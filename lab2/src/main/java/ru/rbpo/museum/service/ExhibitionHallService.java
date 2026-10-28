package ru.rbpo.museum.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import ru.rbpo.museum.exception.ConflictException;
import ru.rbpo.museum.exception.NotFoundException;
import ru.rbpo.museum.model.ExhibitionHall;
import ru.rbpo.museum.repository.ExhibitPlacementRepository;
import ru.rbpo.museum.repository.ExhibitionHallRepository;

@Service
public class ExhibitionHallService {

    private final ExhibitionHallRepository hallRepository;
    private final ExhibitPlacementRepository placementRepository;
    private final ExhibitPlacementService placementService;
    private final Clock clock;

    public ExhibitionHallService(ExhibitionHallRepository hallRepository, ExhibitPlacementRepository placementRepository,
                                 ExhibitPlacementService placementService, Clock clock) {
        this.hallRepository = hallRepository;
        this.placementRepository = placementRepository;
        this.placementService = placementService;
        this.clock = clock;
    }

    public List<ExhibitionHall> findAll() {
        return hallRepository.findAll();
    }

    public ExhibitionHall findById(Long id) {
        return hallRepository.findById(id).orElseThrow(() -> new NotFoundException("Экспозиционный зал", id));
    }

    public ExhibitionHall create(ExhibitionHall hall) {
        if (hall.getAvailable() == null) {
            hall.setAvailable(Boolean.TRUE);
        }
        Instant now = Instant.now(clock);
        hall.setId(null);
        hall.setCreatedAt(now);
        hall.setUpdatedAt(now);
        return hallRepository.save(hall);
    }

    public ExhibitionHall update(Long id, ExhibitionHall changes) {
        ExhibitionHall hall = findById(id);
        if (changes.getName() != null) {
            hall.setName(changes.getName());
        }
        if (changes.getArea() != null) {
            double maxUsed = placementRepository.findByHallId(id).stream()
                    .map(placement -> placement.getExhibitionId())
                    .distinct()
                    .mapToDouble(exhibitionId -> placementService.occupiedArea(id, exhibitionId, null))
                    .max()
                    .orElse(0);
            if (changes.getArea() < maxUsed) {
                throw new ConflictException("Нельзя уменьшить площадь зала до " + changes.getArea()
                        + " м²: размещённые экспонаты занимают " + maxUsed + " м²");
            }
            hall.setArea(changes.getArea());
        }
        if (changes.getAvailable() != null) {
            hall.setAvailable(changes.getAvailable());
        }
        hall.setUpdatedAt(Instant.now(clock));
        return hallRepository.save(hall);
    }

    /** Удаляет зал вместе со всеми размещениями экспонатов в нём. */
    public void delete(Long id) {
        findById(id);
        placementRepository.findByHallId(id).forEach(placement -> placementRepository.deleteById(placement.getId()));
        hallRepository.deleteById(id);
    }
}
