package ru.rbpo.museum.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ru.rbpo.museum.model.ExhibitionHall;

@Repository
public interface ExhibitionHallRepository extends JpaRepository<ExhibitionHall, Long> {

    /**
     * Загружает зал с блокировкой строки (SELECT ... FOR UPDATE), чтобы параллельные
     * размещения не превысили площадь зала.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from ExhibitionHall h where h.id = :id")
    Optional<ExhibitionHall> findByIdForUpdate(Long id);

    Optional<ExhibitionHall> findByName(String name);
}
