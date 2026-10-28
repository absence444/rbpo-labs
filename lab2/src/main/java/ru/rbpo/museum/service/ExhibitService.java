package ru.rbpo.museum.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import ru.rbpo.museum.exception.NotFoundException;
import ru.rbpo.museum.model.Exhibit;
import ru.rbpo.museum.model.ExhibitPlacement;
import ru.rbpo.museum.repository.ExhibitPlacementRepository;
import ru.rbpo.museum.repository.ExhibitRepository;

@Service
public class ExhibitService {

    private final ExhibitRepository exhibitRepository;
    private final ExhibitPlacementRepository placementRepository;
    private final ExhibitPlacementService placementService;
    private final Clock clock;

    public ExhibitService(ExhibitRepository exhibitRepository, ExhibitPlacementRepository placementRepository,
                          ExhibitPlacementService placementService, Clock clock) {
        this.exhibitRepository = exhibitRepository;
        this.placementRepository = placementRepository;
        this.placementService = placementService;
        this.clock = clock;
    }

    public List<Exhibit> findAll() {
        return exhibitRepository.findAll();
    }

    public Exhibit findById(Long id) {
        return exhibitRepository.findById(id).orElseThrow(() -> new NotFoundException("Экспонат", id));
    }

    public Exhibit create(Exhibit exhibit) {
        Instant now = Instant.now(clock);
        exhibit.setId(null);
        exhibit.setCreatedAt(now);
        exhibit.setUpdatedAt(now);
        return exhibitRepository.save(exhibit);
    }

    public Exhibit update(Long id, Exhibit changes) {
        Exhibit exhibit = findById(id);
        if (changes.getName() != null) {
            exhibit.setName(changes.getName());
        }
        if (changes.getCondition() != null) {
            exhibit.setCondition(changes.getCondition());
        }
        if (changes.getRequiredArea() != null) {
            // Изменение площади не должно переполнить залы, где экспонат уже размещён.
            for (ExhibitPlacement placement : placementRepository.findByExhibitId(id)) {
                placementService.ensureHallCapacity(placement.getHallId(), placement.getExhibitionId(),
                        changes.getRequiredArea(), placement.getId());
            }
            exhibit.setRequiredArea(changes.getRequiredArea());
        }
        exhibit.setUpdatedAt(Instant.now(clock));
        return exhibitRepository.save(exhibit);
    }

    /** Удаляет экспонат вместе со всеми его размещениями. */
    public void delete(Long id) {
        findById(id);
        placementRepository.findByExhibitId(id).forEach(placement -> placementRepository.deleteById(placement.getId()));
        exhibitRepository.deleteById(id);
    }
}
