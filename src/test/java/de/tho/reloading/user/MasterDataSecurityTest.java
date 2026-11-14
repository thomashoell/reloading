package de.tho.reloading.user;

import de.tho.reloading.entity.AppUser;
import de.tho.reloading.entity.Manufacturer;
import de.tho.reloading.repository.ManufacturerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Master data: readable by every user, changeable by administrators only (full application context). */
@SpringBootTest
@Testcontainers
class MasterDataSecurityTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private UserService userService;

    @Autowired
    private CurrentUser currentUser;

    @Autowired
    private ManufacturerRepository manufacturerRepository;

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    private static void loginAs(String subject) {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(subject, "n/a", "ROLE_USER"));
    }

    // one test: the first user of the database is the administrator, so the order matters
    @Test
    void shouldLetOnlyTheAdministratorChangeMasterData() {
        AppUser admin = userService.register("admin-subject", "admin");
        AppUser user = userService.register("user-subject", "user");
        assertThat(admin.isAdmin()).isTrue();
        assertThat(user.isAdmin()).isFalse();

        Manufacturer lapua = new Manufacturer();
        lapua.setName("Lapua");

        loginAs("user-subject");
        assertThat(currentUser.id()).isEqualTo(user.getId());
        assertThatThrownBy(() -> manufacturerRepository.save(lapua)).isInstanceOf(AccessDeniedException.class);
        assertThat(manufacturerRepository.findByName("Lapua")).isEmpty();

        loginAs("admin-subject");
        assertThat(currentUser.id()).isEqualTo(admin.getId());
        Manufacturer saved = manufacturerRepository.save(lapua);
        assertThat(saved.getId()).isNotNull();

        loginAs("user-subject");
        assertThat(manufacturerRepository.findByName("Lapua")).isPresent();
        assertThatThrownBy(() -> manufacturerRepository.deleteById(saved.getId())).isInstanceOf(AccessDeniedException.class);
        assertThat(manufacturerRepository.findByName("Lapua")).isPresent();
    }

    @Test
    void shouldRejectChangesWithoutAuthenticatedUser() {
        Manufacturer norma = new Manufacturer();
        norma.setName("Norma");

        assertThatThrownBy(() -> manufacturerRepository.save(norma)).isInstanceOf(RuntimeException.class);
        assertThat(manufacturerRepository.findByName("Norma")).isEmpty();
    }
}
