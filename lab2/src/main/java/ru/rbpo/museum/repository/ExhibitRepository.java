package ru.rbpo.museum.repository;

import org.springframework.stereotype.Repository;

import ru.rbpo.museum.model.Exhibit;

@Repository
public class ExhibitRepository extends InMemoryRepository<Exhibit> {
}
