package ru.rbpo.museum.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import ru.rbpo.museum.model.BaseEntity;

/** Базовое хранилище сущностей в памяти (коллекция), id генерируется автоматически. */
public abstract class InMemoryRepository<T extends BaseEntity> {

    private final Map<Long, T> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public List<T> findAll() {
        return findAllMatching(entity -> true);
    }

    public Optional<T> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public boolean existsById(Long id) {
        return id != null && storage.containsKey(id);
    }

    public T save(T entity) {
        if (entity.getId() == null) {
            entity.setId(sequence.incrementAndGet());
        }
        storage.put(entity.getId(), entity);
        return entity;
    }

    public void deleteById(Long id) {
        storage.remove(id);
    }

    protected List<T> findAllMatching(Predicate<T> filter) {
        return storage.values().stream()
                .filter(filter)
                .sorted(Comparator.comparing(BaseEntity::getId))
                .toList();
    }
}
