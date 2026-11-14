package de.tho.reloading.repository;


import de.tho.reloading.entity.Manufacturer;
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
public class ManufacturerRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private ManufacturerRepository sut;

    @Test
    void shouldSaveAndFindByName() {
        Manufacturer manufacturer = new Manufacturer();
        manufacturer.setName("Winchester");

        sut.save(manufacturer);

        Optional<Manufacturer> found = sut.findByName("Winchester");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Winchester");
        assertThat(found.get().getId()).isNotNull();
    }
}


