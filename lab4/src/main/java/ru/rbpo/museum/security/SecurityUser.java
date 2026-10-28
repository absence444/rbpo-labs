package ru.rbpo.museum.security;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import ru.rbpo.museum.model.Permission;
import ru.rbpo.museum.model.Role;
import ru.rbpo.museum.model.User;
import ru.rbpo.museum.model.UserStatus;

/** Аутентифицированный пользователь: роль даёт authority ROLE_*, а также набор отдельных прав. */
public final class SecurityUser implements UserDetails {

    private final Long id;
    private final String username;
    private final String passwordHash;
    private final Role role;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final List<GrantedAuthority> authorities;

    private SecurityUser(User user, Instant now) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
        this.enabled = user.getStatus() == UserStatus.ACTIVE && user.getPasswordHash() != null;
        this.accountNonLocked = user.getLockedUntil() == null || !user.getLockedUntil().isAfter(now);
        List<GrantedAuthority> granted = new ArrayList<>();
        granted.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        for (Permission permission : role.getPermissions()) {
            granted.add(new SimpleGrantedAuthority(permission.name()));
        }
        this.authorities = List.copyOf(granted);
    }

    public static SecurityUser from(User user, Instant now) {
        return new SecurityUser(user, now);
    }

    public Long getId() { return id; }

    public Role getRole() { return role; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isAccountNonLocked() { return accountNonLocked; }

    @Override
    public boolean isEnabled() { return enabled; }
}
