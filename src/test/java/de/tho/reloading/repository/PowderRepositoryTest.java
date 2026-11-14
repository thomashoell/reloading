package de.tho.reloading.repository;


import de.tho.reloading.entity.Manufacturer;
import de.tho.reloading.entity.Powder;
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
public class PowderRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private ManufacturerRepository manufacturerRepository;

    @Autowired
    private PowderRepository sut;

    @Test
    void shouldSaveAndFindByName() {
        Manufacturer manufacturer = new Manufacturer();
        manufacturer.setName("ReloadSwiss");
        manufacturer = manufacturerRepository.save(manufacturer);

        Powder powder = new Powder();
        powder.setName("RS40");
        powder.setManufacturer(manufacturer.getId());
        sut.save(powder);

        Optional<Powder> found = sut.findByName("rs40");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("RS40");
        assertThat(found.get().getId()).isNotNull();
        assertThat(found.get().getManufacturer()).isEqualTo(manufacturer.getId());
    }
}