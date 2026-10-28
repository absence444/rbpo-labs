package ru.rbpo.museum.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.rbpo.museum.repository.UserRepository;

/**
 * Защита от подбора пароля: после {@code maxFailedAttempts} неверных паролей подряд учётная запись
 * временно блокируется на {@code lockDuration}. Успешный вход сбрасывает счётчик.
 */
@Component
public class LoginAttemptListener {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptListener.class);

    private final UserRepository userRepository;
    private final SecurityProperties properties;
    private final Clock clock;

    public LoginAttemptListener(UserRepository userRepository, SecurityProperties properties, Clock clock) {
        this.userRepository = userRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @EventListener
    @Transactional
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = String.valueOf(event.getAuthentication().getName()).trim().toLowerCase(Locale.ROOT);
        userRepository.findByUsername(username).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            if (attempts >= properties.maxFailedAttempts()) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(Instant.now(clock).plus(properties.lockDuration()));
                log.warn("Учётная запись id={} временно заблокирована после {} неудачных попыток входа",
                        user.getId(), attempts);
            } else {
                user.setFailedLoginAttempts(attempts);
            }
        });
    }

    @EventListener
    @Transactional
    public void onSuccess(AuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof SecurityUser principal) {
            userRepository.findById(principal.getId())
                    .filter(user -> user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null)
                    .ifPresent(user -> user.resetLoginFailures());
        }
    }
}
