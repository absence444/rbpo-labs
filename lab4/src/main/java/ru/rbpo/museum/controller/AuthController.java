package ru.rbpo.museum.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ru.rbpo.museum.dto.AuthDto;
import ru.rbpo.museum.dto.UserDto;
import ru.rbpo.museum.model.User;
import ru.rbpo.museum.security.SecurityUser;
import ru.rbpo.museum.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /** Выдаёт CSRF-токен (также устанавливается cookie XSRF-TOKEN). Доступно без аутентификации. */
    @GetMapping("/csrf")
    public AuthDto.CsrfResponse csrf(CsrfToken token) {
        return new AuthDto.CsrfResponse(token.getHeaderName(), token.getToken());
    }

    /** Регистрация посетителя. Требует CSRF-токен; роль всегда VISITOR. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto.Response register(@Valid @RequestBody AuthDto.RegisterRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        return UserController.toResponse(userService.register(user, request.password()));
    }

    /** Профиль текущего пользователя. */
    @GetMapping("/me")
    public UserDto.Response me(@AuthenticationPrincipal SecurityUser principal) {
        return UserController.toResponse(userService.findById(principal.getId()));
    }
}
