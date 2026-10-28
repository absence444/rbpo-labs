package ru.rbpo.museum.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import ru.rbpo.museum.exception.ConflictException;
import ru.rbpo.museum.exception.NotFoundException;
import ru.rbpo.museum.model.Exhibition;
import ru.rbpo.museum.model.User;
import ru.rbpo.museum.repository.ExhibitionRepository;
import ru.rbpo.museum.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final Clock clock;

    public UserService(UserRepository userRepository, ExhibitionRepository exhibitionRepository, Clock clock) {
        this.userRepository = userRepository;
        this.exhibitionRepository = exhibitionRepository;
        this.clock = clock;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("Пользователь", id));
    }

    public User create(User user) {
        ensureUsernameFree(user.getUsername(), null);
        Instant now = Instant.now(clock);
        user.setId(null);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }

    public User update(Long id, User changes) {
        User user = findById(id);
        if (changes.getUsername() != null) {
            ensureUsernameFree(changes.getUsername(), id);
            user.setUsername(changes.getUsername());
        }
        if (changes.getFullName() != null) {
            user.setFullName(changes.getFullName());
        }
        if (changes.getEmail() != null) {
            user.setEmail(changes.getEmail());
        }
        user.setUpdatedAt(Instant.now(clock));
        return userRepository.save(user);
    }

    /** Удаляет пользователя и снимает его с роли куратора во всех выставках. */
    public void delete(Long id) {
        findById(id);
        Instant now = Instant.now(clock);
        for (Exhibition exhibition : exhibitionRepository.findByCuratorId(id)) {
            exhibition.setCuratorId(null);
            exhibition.setUpdatedAt(now);
            exhibitionRepository.save(exhibition);
        }
        userRepository.deleteById(id);
    }

    private void ensureUsernameFree(String username, Long ownerId) {
        userRepository.findByUsername(username)
                .filter(existing -> !existing.getId().equals(ownerId))
                .ifPresent(existing -> {
                    throw new ConflictException("Имя пользователя '" + username + "' уже занято");
                });
    }
}
