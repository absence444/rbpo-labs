package ru.rbpo.museum.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ru.rbpo.museum.model.Exhibition;
import ru.rbpo.museum.model.ExhibitionStatus;

@Repository
public interface ExhibitionRepository extends JpaRepository<Exhibition, Long> {

    List<Exhibition> findByCuratorId(Long curatorId);

    boolean existsByCuratorIdAndStatusIn(Long curatorId, Collection<ExhibitionStatus> statuses);
}
