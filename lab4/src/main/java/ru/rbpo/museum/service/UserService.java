package ru.rbpo.museum.service;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.rbpo.museum.exception.ConflictException;
import ru.rbpo.museum.exception.NotFoundException;
import ru.rbpo.museum.exception.WeakPasswordException;
import ru.rbpo.museum.model.Exhibition;
import ru.rbpo.museum.model.ExhibitionStatus;
import ru.rbpo.museum.model.Role;
import ru.rbpo.museum.model.User;
import ru.rbpo.museum.model.UserStatus;
import ru.rbpo.museum.repository.ExhibitionRepository;
import ru.rbpo.museum.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final Clock clock;

    public UserService(UserRepository userRepository, ExhibitionRepository exhibitionRepository,
                       PasswordEncoder passwordEncoder, PasswordPolicy passwordPolicy, Clock clock) {
        this.userRepository = userRepository;
        this.exhibitionRepository = exhibitionRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.clock = clock;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("Пользователь", id));
    }

    /** Самостоятельная регистрация: роль всегда VISITOR, повысить её может только администратор. */
    @Transactional
    public User register(User user, String rawPassword) {
        user.setRole(Role.VISITOR);
        return create(user, rawPassword);
    }

    /** Создание учётной записи (администратором — с любой ролью). */
    @Transactional
    public User create(User user, String rawPassword) {
        user.setUsername(normalize(user.getUsername()));
        user.setEmail(normalize(user.getEmail()));
        ensureUnique(user.getUsername(), user.getEmail(), null);
        user.setPasswordHash(encodeChecked(rawPassword, user.getUsername(), user.getEmail()));
        if (user.getRole() == null) {
            user.setRole(Role.VISITOR);
        }
        user.setStatus(UserStatus.ACTIVE);
        user.resetLoginFailures();
        Instant now = Instant.now(clock);
        user.setId(null);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }

    /**
     * Изменение учётной записи администратором.
     * Администратор не может менять собственные роль и статус, чтобы случайно не лишиться доступа.
     */
    @Transactional
    public User update(Long id, User changes, String rawPassword, Long actingUserId) {
        User user = findById(id);
        boolean self = id.equals(actingUserId);
        if (self && (changes.getRole() != null && changes.getRole() != user.getRole()
                || changes.getStatus() != null && changes.getStatus() != user.getStatus())) {
            throw new ConflictException("Нельзя изменить собственные роль или статус");
        }
        String username = changes.getUsername() != null ? normalize(changes.getUsername()) : null;
        String email = changes.getEmail() != null ? normalize(changes.getEmail()) : null;
        ensureUnique(username, email, id);

        if (changes.getRole() != null && changes.getRole() != user.getRole()) {
            if (user.getRole() == Role.CURATOR && exhibitionRepository.existsByCuratorIdAndStatusIn(
                    id, EnumSet.of(ExhibitionStatus.PLANNED, ExhibitionStatus.ACTIVE))) {
                throw new ConflictException("Пользователь курирует незавершённые выставки; сначала назначьте им другого куратора");
            }
            user.setRole(changes.getRole());
        }
        if (changes.getStatus() == UserStatus.BLOCKED && exhibitionRepository.existsByCuratorIdAndStatusIn(
                id, EnumSet.of(ExhibitionStatus.ACTIVE))) {
            throw new ConflictException("Нельзя заблокировать куратора открытой выставки");
        }
        if (changes.getStatus() != null) {
            user.setStatus(changes.getStatus());
            if (changes.getStatus() == UserStatus.ACTIVE) {
                user.resetLoginFailures();
            }
        }
        if (username != null) {
            user.setUsername(username);
        }
        if (email != null) {
            user.setEmail(email);
        }
        if (changes.getFullName() != null) {
            user.setFullName(changes.getFullName());
        }
        if (rawPassword != null) {
            user.setPasswordHash(encodeChecked(rawPassword, user.getUsername(), user.getEmail()));
            user.resetLoginFailures();
        }
        user.setUpdatedAt(Instant.now(clock));
        return user;
    }

    /**
     * Удаляет пользователя и снимает его с роли куратора выставок.
     * Нельзя удалить себя и куратора открытой выставки.
     */
    @Transactional
    public void delete(Long id, Long actingUserId) {
        User user = findById(id);
        if (id.equals(actingUserId)) {
            throw new ConflictException("Нельзя удалить собственную учётную запись");
        }
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

    private String encodeChecked(String rawPassword, String username, String email) {
        List<String> violations = passwordPolicy.validate(rawPassword, username, email);
        if (!violations.isEmpty()) {
            throw new WeakPasswordException(violations);
        }
        return passwordEncoder.encode(rawPassword);
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
