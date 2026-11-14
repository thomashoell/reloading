package de.tho.reloading.repository;

import de.tho.reloading.entity.Bullet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJdbcTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestData.class)
public class BulletRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private TestData data;

    @Autowired
    private BulletRepository sut;

    @Test
    void shouldSaveAndFindByNameIgnoringCase() {
        Bullet bullet = data.newBullet();
        bullet.setLength(new BigDecimal("0.0351"));
        bullet = sut.save(bullet);

        List<Bullet> found = sut.findByName("eld match");

        assertThat(found).hasSize(1);
        Bullet actual = found.getFirst();
        assertThat(actual.getId()).isEqualTo(bullet.getId());
        assertThat(actual.getName()).isEqualTo("ELD Match");
        assertThat(actual.getManufacturer()).isEqualTo(bullet.getManufacturer());
        assertThat(actual.getDiameter()).isEqualByComparingTo(TestData.BULLET_DIAMETER);
        assertThat(actual.getWeight()).isEqualByComparingTo(TestData.BULLET_WEIGHT);
        assertThat(actual.getLength()).isEqualByComparingTo("0.0351");
    }

    @Test
    void shouldUpdateExistingBullet() {
        Bullet bullet = data.bullet();
        bullet.setName("ELD-M");
        bullet.setLength(new BigDecimal("0.0351"));
        sut.save(bullet);

        Bullet actual = sut.findById(bullet.getId()).orElseThrow();
        assertThat(actual.getName()).isEqualTo("ELD-M");
        assertThat(actual.getLength()).isEqualByComparingTo("0.0351");
        assertThat(sut.count()).isEqualTo(1);
    }

    @Test
    void shouldFindByManufacturerAndDiameter() {
        Bullet bullet = data.bullet();

        assertThat(sut.findByManufacturer(bullet.getManufacturer())).hasSize(1);
        assertThat(sut.findByDiameter(TestData.BULLET_DIAMETER)).hasSize(1);
        assertThat(sut.findByDiameter(new BigDecimal("0.0078232"))).isEmpty();
    }

    @Test
    void shouldAllowSameNameWithDifferentWeight() {
        Bullet bullet = data.bullet();
        Bullet heavier = new Bullet();
        heavier.setManufacturer(bullet.getManufacturer());
        heavier.setName(bullet.getName());
        heavier.setDiameter(bullet.getDiameter());
        heavier.setWeight(new BigDecimal("0.00952543977")); // 147 gr
        sut.save(heavier);

        assertThat(sut.findByName("ELD Match")).hasSize(2);
    }

    @Test
    void shouldRejectDuplicateBullet() {
        Bullet bullet = data.bullet();
        Bullet duplicate = new Bullet();
        duplicate.setManufacturer(bullet.getManufacturer());
        duplicate.setName("eld match");
        duplicate.setDiameter(bullet.getDiameter());
        duplicate.setWeight(bullet.getWeight());

        assertThatThrownBy(() -> sut.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_bullet_manufacturer_name_diameter_weight");
    }

    @Test
    void shouldRejectNonPositiveWeight() {
        Bullet bullet = new Bullet();
        bullet.setManufacturer(data.manufacturer("Hornady").getId());
        bullet.setName("CX");
        bullet.setDiameter(TestData.BULLET_DIAMETER);
        bullet.setWeight(BigDecimal.ZERO);

        assertThatThrownBy(() -> sut.save(bullet))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_bullet_weight");
    }
}
