package de.tho.reloading.user;

import de.tho.reloading.entity.AppUser;
import de.tho.reloading.repository.AppUserRepository;
import de.tho.reloading.utility.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UserService.class)
class UserServiceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private UserService sut;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void shouldMakeOnlyTheFirstUserAdministrator() {
        AppUser first = sut.register("subject-1", "anna");
        AppUser second = sut.register("subject-2", "bert");
        AppUser third = sut.register("subject-3", "cleo");

        assertThat(first.getId()).isNotNull();
        assertThat(first.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(first.isAdmin()).isTrue();
        assertThat(second.getRole()).isEqualTo(UserRole.USER);
        assertThat(second.isAdmin()).isFalse();
        assertThat(third.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    void shouldReturnTheExistingUserOnLaterLogins() {
        AppUser first = sut.register("subject-1", "anna");
        sut.register("subject-2", "bert");

        AppUser again = sut.register("subject-1", "anna");

        assertThat(again.getId()).isEqualTo(first.getId());
        assertThat(again.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(appUserRepository.count()).isEqualTo(2);
    }

    @Test
    void shouldRefreshTheUsername() {
        AppUser user = sut.register("subject-1", "anna");

        sut.register("subject-1", "anna.new");

        AppUser actual = appUserRepository.findBySubject("subject-1").orElseThrow();
        assertThat(actual.getId()).isEqualTo(user.getId());
        assertThat(actual.getUsername()).isEqualTo("anna.new");
        assertThat(actual.getRole()).isEqualTo(UserRole.ADMIN);
    }
}
