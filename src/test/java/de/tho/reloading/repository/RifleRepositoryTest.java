package de.tho.reloading.repository;


import de.tho.reloading.entity.Caliber;
import de.tho.reloading.entity.Manufacturer;
import de.tho.reloading.entity.Rifle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class RifleRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private CaliberRepository caliberRepository;

    @Autowired
    private ManufacturerRepository manufacturerRepository;

    @Autowired
    private RifleRepository sut;

    @Test
    void shouldSaveAndFindByName() {
        Caliber caliber = new Caliber();
        caliber.setName(".30-06 Springfield");
        caliber = caliberRepository.save(caliber);

        Manufacturer manufacturer = new Manufacturer();
        manufacturer.setName("Winchester");
        manufacturer = manufacturerRepository.save(manufacturer);

        Rifle rifle = new Rifle();
        rifle.setName("XPR");
        rifle.setManufacturer(manufacturer.getId());
        rifle.setCaliber(caliber.getId());
        sut.save(rifle);

        Optional<Rifle> found = sut.findByName("XPR");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("XPR");
        assertThat(found.get().getId()).isNotNull();
        assertThat(found.get().getCaliber()).isEqualTo(caliber.getId());
        assertThat(found.get().getManufacturer()).isEqualTo(manufacturer.getId());
    }


    @Test
    void shouldSaveAndFindByManufacturer() {
        Caliber caliber = new Caliber();
        caliber.setName(".30-06 Springfield");
        caliber = caliberRepository.save(caliber);

        Manufacturer manufacturer = new Manufacturer();
        manufacturer.setName("Winchester");
        manufacturer = manufacturerRepository.save(manufacturer);

        Rifle rifle = new Rifle();
        rifle.setName("XPR");
        rifle.setManufacturer(manufacturer.getId());
        rifle.setCaliber(caliber.getId());
        sut.save(rifle);

        List<Rifle> found = sut.findByManufacturer(manufacturer.getId());

        assertThat(found).isNotEmpty();
        assertThat(found.getFirst().getName()).isEqualTo("XPR");
        assertThat(found.getFirst().getId()).isNotNull();
        assertThat(found.getFirst().getCaliber()).isEqualTo(caliber.getId());
        assertThat(found.getFirst().getManufacturer()).isEqualTo(manufacturer.getId());
    }

    @Test
    void shouldSaveAndFindCaliber() {
        Caliber caliber = new Caliber();
        caliber.setName(".30-06 Springfield");
        caliber = caliberRepository.save(caliber);

        Manufacturer manufacturer = new Manufacturer();
        manufacturer.setName("Winchester");
        manufacturer = manufacturerRepository.save(manufacturer);

        Rifle rifle = new Rifle();
        rifle.setName("XPR");
        rifle.setManufacturer(manufacturer.getId());
        rifle.setCaliber(caliber.getId());
        sut.save(rifle);

        List<Rifle> found = sut.findByCaliber(caliber.getId());

        assertThat(found).isNotEmpty();
        assertThat(found.getFirst().getName()).isEqualTo("XPR");
        assertThat(found.getFirst().getId()).isNotNull();
        assertThat(found.getFirst().getCaliber()).isEqualTo(caliber.getId());
        assertThat(found.getFirst().getManufacturer()).isEqualTo(manufacturer.getId());
    }
}