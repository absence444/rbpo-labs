package ru.rbpo.museum.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import ru.rbpo.museum.model.Exhibition;

@Repository
public class ExhibitionRepository extends InMemoryRepository<Exhibition> {

    public List<Exhibition> findByCuratorId(Long curatorId) {
        return findAllMatching(exhibition -> curatorId.equals(exhibition.getCuratorId()));
    }
}
