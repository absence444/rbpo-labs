package ru.rbpo.museum.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ru.rbpo.museum.dto.UserDto;
import ru.rbpo.museum.model.User;
import ru.rbpo.museum.security.SecurityUser;
import ru.rbpo.museum.service.UserService;

/** Управление учётными записями — только администратор. */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAuthority('USER_MANAGE')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserDto.Response> findAll() {
        return userService.findAll().stream().map(UserController::toResponse).toList();
    }

    @GetMapping("/{id}")
    public UserDto.Response findById(@PathVariable Long id) {
        return toResponse(userService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto.Response create(@Valid @RequestBody UserDto.CreateRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setRole(request.role());
        return toResponse(userService.create(user, request.password()));
    }

    @PatchMapping("/{id}")
    public UserDto.Response update(@PathVariable Long id, @Valid @RequestBody UserDto.PatchRequest request,
                                   @AuthenticationPrincipal SecurityUser principal) {
        User changes = new User();
        changes.setUsername(request.username());
        changes.setFullName(request.fullName());
        changes.setEmail(request.email());
        changes.setRole(request.role());
        changes.setStatus(request.status());
        return toResponse(userService.update(id, changes, request.password(), principal.getId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal SecurityUser principal) {
        userService.delete(id, principal.getId());
    }

    static UserDto.Response toResponse(User user) {
        return new UserDto.Response(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(),
                user.getRole(), user.getStatus(), user.getLockedUntil(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
