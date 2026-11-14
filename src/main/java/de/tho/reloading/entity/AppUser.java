package de.tho.reloading.entity;

import de.tho.reloading.utility.UserRole;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import java.util.UUID;

/**
 * A user of the application. Created on the first request of an authenticated user; the very first user
 * becomes administrator.
 */
@Getter
@Setter
@NoArgsConstructor
public class AppUser {

    @Id
    private UUID id;

    // stable identifier from the identity provider (OIDC sub)
    private String subject;
    // display name, refreshed from the identity provider
    private String username;
    private UserRole role;

    @Transient
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
