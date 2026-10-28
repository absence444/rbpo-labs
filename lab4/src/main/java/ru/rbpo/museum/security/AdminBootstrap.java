package ru.rbpo.museum.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import ru.rbpo.museum.model.Role;
import ru.rbpo.museum.model.User;
import ru.rbpo.museum.repository.UserRepository;
import ru.rbpo.museum.service.UserService;

/**
 * Создаёт первого администратора из переменных окружения (ADMIN_USERNAME/ADMIN_EMAIL/ADMIN_PASSWORD),
 * если в БД ещё нет ни одного администратора. Пароль в репозитории не хранится.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final SecurityProperties properties;
    private final UserRepository userRepository;
    private final UserService userService;

    public AdminBootstrap(SecurityProperties properties, UserRepository userRepository, UserService userService) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureAdminExists();
    }

    public void ensureAdminExists() {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        SecurityProperties.Admin admin = properties.admin();
        if (admin == null || isBlank(admin.username()) || isBlank(admin.password()) || isBlank(admin.email())) {
            log.warn("Администратор не создан: задайте ADMIN_USERNAME, ADMIN_EMAIL и ADMIN_PASSWORD");
            return;
        }
        User user = new User();
        user.setUsername(admin.username());
        user.setEmail(admin.email());
        user.setFullName("Администратор");
        user.setRole(Role.ADMIN);
        userService.create(user, admin.password());
        log.info("Создан администратор '{}'", user.getUsername());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
