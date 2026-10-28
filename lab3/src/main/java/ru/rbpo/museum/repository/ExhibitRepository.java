package ru.rbpo.museum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ru.rbpo.museum.model.Exhibit;

@Repository
public interface ExhibitRepository extends JpaRepository<Exhibit, Long> {
}
