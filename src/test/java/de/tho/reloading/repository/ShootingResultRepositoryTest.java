package de.tho.reloading.repository;

import de.tho.reloading.entity.Load;
import de.tho.reloading.entity.Rifle;
import de.tho.reloading.entity.ShootingResult;
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
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJdbcTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestData.class)
public class ShootingResultRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private TestData data;

    @Autowired
    private ShootingResultRepository sut;

    @Test
    void shouldSaveAndFindByLoad() {
        Rifle rifle = data.rifle();
        Load load = data.load();
        ShootingResult result = new ShootingResult();
        result.setOwner(data.owner().getId());
        result.setLoad(load.getId());
        result.setRifle(rifle.getId());
        result.setShotOn(LocalDate.of(2026, 11, 14));
        result.setDistance(new BigDecimal("100"));
        result.setShotCount(5);
        result.setVelocityMean(new BigDecimal("823.4"));
        result.setVelocitySd(new BigDecimal("2.1"));
        result.setVelocityEs(new BigDecimal("5.8"));
        result.setGroupSize(new BigDecimal("0.0112"));
        result.setTemperature(new BigDecimal("281.15"));
        result.setNotes("light wind from the left");

        sut.save(result);

        List<ShootingResult> found = sut.findByOwnerAndLoad(load.getOwner(), load.getId());

        assertThat(found).hasSize(1);
        ShootingResult actual = found.getFirst();
        assertThat(actual.getId()).isNotNull();
        assertThat(actual.getRifle()).isEqualTo(rifle.getId());
        assertThat(actual.getShotOn()).isEqualTo(LocalDate.of(2026, 11, 14));
        assertThat(actual.getDistance()).isEqualByComparingTo("100");
        assertThat(actual.getShotCount()).isEqualTo(5);
        assertThat(actual.getVelocityMean()).isEqualByComparingTo("823.4");
        assertThat(actual.getVelocitySd()).isEqualByComparingTo("2.1");
        assertThat(actual.getVelocityEs()).isEqualByComparingTo("5.8");
        assertThat(actual.getGroupSize()).isEqualByComparingTo("0.0112");
        assertThat(actual.getTemperature()).isEqualByComparingTo("281.15");
        assertThat(actual.getNotes()).isEqualTo("light wind from the left");
    }

    @Test
    void shouldSaveMinimalResultAndFindByRifle() {
        Rifle rifle = data.rifle();
        ShootingResult result = new ShootingResult();
        result.setOwner(data.owner().getId());
        result.setLoad(data.load().getId());
        result.setRifle(rifle.getId());
        result.setShotOn(LocalDate.of(2026, 11, 14));
        sut.save(result);

        List<ShootingResult> found = sut.findByOwnerAndRifle(rifle.getOwner(), rifle.getId());

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getVelocityMean()).isNull();
        assertThat(found.getFirst().getGroupSize()).isNull();
    }

    @Test
    void shouldRejectNegativeStandardDeviation() {
        ShootingResult result = new ShootingResult();
        result.setOwner(data.owner().getId());
        result.setLoad(data.load().getId());
        result.setRifle(data.rifle().getId());
        result.setShotOn(LocalDate.of(2026, 11, 14));
        result.setVelocitySd(new BigDecimal("-1"));

        assertThatThrownBy(() -> sut.save(result))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_shooting_result_velocity_sd");
    }

    @Test
    void shouldRejectLoadOfAnotherOwner() {
        ShootingResult result = new ShootingResult();
        result.setOwner(data.otherOwner().getId());
        result.setLoad(data.load().getId());
        result.setRifle(data.rifle(data.otherOwner()).getId());
        result.setShotOn(LocalDate.of(2026, 11, 14));

        assertThatThrownBy(() -> sut.save(result))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_shooting_result_load");
    }
}
