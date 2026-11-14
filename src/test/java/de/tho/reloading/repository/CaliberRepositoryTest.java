package de.tho.reloading.repository;


import de.tho.reloading.entity.Caliber;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CaliberRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private CaliberRepository sut;

    @Test
    void shouldSaveAndFindByName() {
        Caliber caliber = new Caliber();
        caliber.setName(".30-06 Springfield");

        sut.save(caliber);

        Optional<Caliber> found = sut.findByName(".30-06 Springfield");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo(".30-06 Springfield");
        assertThat(found.get().getId()).isNotNull();
    }
}


