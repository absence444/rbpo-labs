package ru.rbpo.museum.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ru.rbpo.museum.model.ExhibitPlacement;
import ru.rbpo.museum.model.ExhibitionStatus;
import ru.rbpo.museum.model.MountingStatus;

@Repository
public interface ExhibitPlacementRepository extends JpaRepository<ExhibitPlacement, Long> {

    List<ExhibitPlacement> findByExhibitId(Long exhibitId);

    List<ExhibitPlacement> findByExhibitionId(Long exhibitionId);

    List<ExhibitPlacement> findByHallId(Long hallId);

    boolean existsByExhibitIdAndExhibitionId(Long exhibitId, Long exhibitionId);

    boolean existsByExhibitIdAndMountingStatusAndExhibitionStatus(Long exhibitId, MountingStatus mountingStatus,
                                                                   ExhibitionStatus exhibitionStatus);

    boolean existsByHallIdAndMountingStatusAndExhibitionStatus(Long hallId, MountingStatus mountingStatus,
                                                                ExhibitionStatus exhibitionStatus);

    /**
     * Площадь зала, занятая экспонатами незавершённых выставок, период которых пересекается с [from, to].
     * Демонтированные размещения и размещение {@code excludePlacementId} не учитываются.
     */
    @Query("""
            select coalesce(sum(p.exhibit.requiredArea), 0)
            from ExhibitPlacement p
            where p.hall.id = :hallId
              and p.id <> :excludePlacementId
              and p.mountingStatus <> ru.rbpo.museum.model.MountingStatus.DISMANTLED
              and p.exhibition.status in :statuses
              and p.exhibition.startDate <= :to
              and p.exhibition.endDate >= :from
            """)
    double sumOccupiedArea(Long hallId, LocalDate from, LocalDate to, Collection<ExhibitionStatus> statuses,
                           Long excludePlacementId);
}
