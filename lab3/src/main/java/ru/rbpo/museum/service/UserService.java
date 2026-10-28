package ru.rbpo.museum.service;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.rbpo.museum.exception.ConflictException;
import ru.rbpo.museum.exception.NotFoundException;
import ru.rbpo.museum.model.Exhibition;
import ru.rbpo.museum.model.ExhibitionStatus;
import ru.rbpo.museum.model.User;
import ru.rbpo.museum.repository.ExhibitionRepository;
import ru.rbpo.museum.repository.UserRepository;

@Service
@Transactional(readOnly = true)
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

    @Transactional
    public User create(User user) {
        user.setUsername(normalize(user.getUsername()));
        user.setEmail(normalize(user.getEmail()));
        ensureUnique(user.getUsername(), user.getEmail(), null);
        Instant now = Instant.now(clock);
        user.setId(null);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }

    @Transactional
    public User update(Long id, User changes) {
        User user = findById(id);
        String username = changes.getUsername() != null ? normalize(changes.getUsername()) : null;
        String email = changes.getEmail() != null ? normalize(changes.getEmail()) : null;
        ensureUnique(username, email, id);
        if (username != null) {
            user.setUsername(username);
        }
        if (email != null) {
            user.setEmail(email);
        }
        if (changes.getFullName() != null) {
            user.setFullName(changes.getFullName());
        }
        user.setUpdatedAt(Instant.now(clock));
        return user;
    }

    /**
     * Удаляет пользователя и снимает его с роли куратора выставок.
     * Куратора открытой выставки удалить нельзя: у работающей выставки должен быть куратор.
     */
    @Transactional
    public void delete(Long id) {
        User user = findById(id);
        if (exhibitionRepository.existsByCuratorIdAndStatusIn(id, EnumSet.of(ExhibitionStatus.ACTIVE))) {
            throw new ConflictException("Пользователь курирует открытую выставку; сначала назначьте другого куратора");
        }
        Instant now = Instant.now(clock);
        for (Exhibition exhibition : exhibitionRepository.findByCuratorId(id)) {
            exhibition.setCurator(null);
            exhibition.setUpdatedAt(now);
        }
        userRepository.delete(user);
    }

    private void ensureUnique(String username, String email, Long ownerId) {
        if (username != null) {
            userRepository.findByUsername(username)
                    .filter(existing -> !existing.getId().equals(ownerId))
                    .ifPresent(existing -> {
                        throw new ConflictException("Имя пользователя '" + username + "' уже занято");
                    });
        }
        if (email != null) {
            userRepository.findByEmail(email)
                    .filter(existing -> !existing.getId().equals(ownerId))
                    .ifPresent(existing -> {
                        throw new ConflictException("Email '" + email + "' уже используется");
                    });
        }
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
