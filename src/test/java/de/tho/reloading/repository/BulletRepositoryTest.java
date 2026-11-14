package de.tho.reloading.repository;


import de.tho.reloading.entity.Bullet;
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
public class BulletRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private CaliberRepository caliberRepository;

    @Autowired
    private ManufacturerRepository manufacturerRepository;

    @Autowired
    private BulletRepository sut;

    @Test
    void shouldSaveAndFindByName() {
        Caliber caliber = new Caliber();
        caliber.setName(".30-06 Springfield");
        caliber = caliberRepository.save(caliber);

        Manufacturer manufacturer = new Manufacturer();
        manufacturer.setName("Hornady");
        manufacturer = manufacturerRepository.save(manufacturer);

        Bullet bullet = new Bullet();
        bullet.setType("CX");
        bullet.setManufacturer(manufacturer.getId());
        bullet.setCaliber(caliber.getId());

        sut.save(bullet);

        List<Bullet> found = sut.findByType("CX");

        // Then
        assertThat(found).isNotEmpty();
        assertThat(found.getFirst().getId()).isNotNull();
        assertThat(found.getFirst().getType()).isEqualTo("CX");
        assertThat(found.getFirst().getCaliber()).isEqualTo(caliber.getId());
        assertThat(found.getFirst().getManufacturer()).isEqualTo(manufacturer.getId());
    }
}