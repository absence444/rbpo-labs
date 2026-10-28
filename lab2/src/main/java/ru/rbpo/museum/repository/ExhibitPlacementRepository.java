package ru.rbpo.museum.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import ru.rbpo.museum.model.ExhibitPlacement;

@Repository
public class ExhibitPlacementRepository extends InMemoryRepository<ExhibitPlacement> {

    public List<ExhibitPlacement> findByExhibitId(Long exhibitId) {
        return findAllMatching(placement -> exhibitId.equals(placement.getExhibitId()));
    }

    public List<ExhibitPlacement> findByExhibitionId(Long exhibitionId) {
        return findAllMatching(placement -> exhibitionId.equals(placement.getExhibitionId()));
    }

    public List<ExhibitPlacement> findByHallId(Long hallId) {
        return findAllMatching(placement -> hallId.equals(placement.getHallId()));
    }

    public List<ExhibitPlacement> findByHallIdAndExhibitionId(Long hallId, Long exhibitionId) {
        return findAllMatching(placement -> hallId.equals(placement.getHallId())
                && exhibitionId.equals(placement.getExhibitionId()));
    }
}
