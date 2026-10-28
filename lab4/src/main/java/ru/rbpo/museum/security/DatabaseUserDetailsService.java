package ru.rbpo.museum.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.rbpo.museum.repository.UserRepository;

/** Загружает учётные записи из БД для HTTP Basic аутентификации. */
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final Clock clock;

    public DatabaseUserDetailsService(UserRepository userRepository, Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByUsername(username.trim().toLowerCase(Locale.ROOT))
                .map(user -> SecurityUser.from(user, Instant.now(clock)))
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
    }
}
