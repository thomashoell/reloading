package de.tho.reloading.user;

import de.tho.reloading.entity.AppUser;
import de.tho.reloading.repository.AppUserRepository;
import de.tho.reloading.utility.UserRole;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    // arbitrary key of the PostgreSQL advisory lock that serializes the creation of users
    private static final long REGISTRATION_LOCK = 7_265_001L;

    private final AppUserRepository appUserRepository;
    private final JdbcTemplate jdbcTemplate;

    public UserService(AppUserRepository appUserRepository, JdbcTemplate jdbcTemplate) {
        this.appUserRepository = appUserRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Returns the user with this subject and creates it if it doesn't exist yet.
     * The very first user becomes administrator, every later one a regular user.
     */
    @Transactional
    public AppUser register(String subject, String username) {
        Optional<AppUser> existing = appUserRepository.findBySubject(subject);
        if (existing.isEmpty()) {
            // two first logins at the same time must not both find an empty table
            jdbcTemplate.execute("SELECT pg_advisory_xact_lock(" + REGISTRATION_LOCK + ")");
            existing = appUserRepository.findBySubject(subject);
        }
        if (existing.isPresent()) {
            return refreshUsername(existing.get(), username);
        }

        AppUser user = new AppUser();
        user.setSubject(subject);
        user.setUsername(username);
        user.setRole(appUserRepository.count() == 0 ? UserRole.ADMIN : UserRole.USER);
        return appUserRepository.save(user);
    }

    private AppUser refreshUsername(AppUser user, String username) {
        if (username == null || username.equals(user.getUsername())) {
            return user;
        }
        user.setUsername(username);
        return appUserRepository.save(user);
    }
}
