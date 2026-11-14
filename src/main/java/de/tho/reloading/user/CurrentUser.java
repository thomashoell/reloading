package de.tho.reloading.user;

import de.tho.reloading.entity.AppUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * The application user behind the current request. Looked up (and created on first use) on every call,
 * so a changed role takes effect immediately.
 */
@Component
public class CurrentUser {

    private final UserService userService;

    public CurrentUser(UserService userService) {
        this.userService = userService;
    }

    public AppUser get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("no authenticated user");
        }
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            String username = oidcUser.getPreferredUsername() != null ? oidcUser.getPreferredUsername() : authentication.getName();
            return userService.register(oidcUser.getSubject(), username);
        }
        // profile noauth and tests: the name is the only identifier
        return userService.register(authentication.getName(), authentication.getName());
    }

    /** Owner id for the user data repositories. */
    public UUID id() {
        return get().getId();
    }

    public boolean isAdmin() {
        return get().isAdmin();
    }
}
