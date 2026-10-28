package ru.rbpo.museum.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import ru.rbpo.museum.model.User;

@Repository
public class UserRepository extends InMemoryRepository<User> {

    public Optional<User> findByUsername(String username) {
        return findAllMatching(user -> user.getUsername().equalsIgnoreCase(username)).stream().findFirst();
    }
}
