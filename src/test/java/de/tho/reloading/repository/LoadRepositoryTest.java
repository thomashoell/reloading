package de.tho.reloading.repository;

import de.tho.reloading.entity.AppUser;
import de.tho.reloading.entity.Load;
import de.tho.reloading.entity.Rifle;
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
public class LoadRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private TestData data;

    @Autowired
    private LoadRepository sut;

    @Test
    void shouldSaveAndFindByProcessedCase() {
        Rifle rifle = data.rifle();
        Load load = data.newLoad();
        load.setBaseToOgive(new BigDecimal("0.0561"));
        load.setRifle(rifle.getId());
        load.setNotes("ladder test, step 3");

        sut.save(load);

        List<Load> found = sut.findByOwnerAndProcessedCase(load.getOwner(), load.getProcessedCase());

        assertThat(found).hasSize(1);
        Load actual = found.getFirst();
        assertThat(actual.getId()).isNotNull();
        assertThat(actual.getBulletLot()).isEqualTo(load.getBulletLot());
        assertThat(actual.getPowderLot()).isEqualTo(load.getPowderLot());
        assertThat(actual.getPrimerLot()).isEqualTo(load.getPrimerLot());
        assertThat(actual.getPowderCharge()).isEqualByComparingTo(TestData.POWDER_CHARGE);
        assertThat(actual.getOverallLength()).isEqualByComparingTo(TestData.OVERALL_LENGTH);
        assertThat(actual.getBaseToOgive()).isEqualByComparingTo("0.0561");
        assertThat(actual.getQuantity()).isEqualTo(20);
        assertThat(actual.getLoadedOn()).isEqualTo(LocalDate.of(2026, 11, 7));
        assertThat(actual.getRifle()).isEqualTo(rifle.getId());
        assertThat(actual.getNotes()).isEqualTo("ladder test, step 3");
    }

    @Test
    void shouldFindByComponentLots() {
        Load load = data.load();

        assertThat(sut.findByOwnerAndBulletLot(load.getOwner(), load.getBulletLot())).hasSize(1);
        assertThat(sut.findByOwnerAndPowderLot(load.getOwner(), load.getPowderLot())).hasSize(1);
        assertThat(sut.findByOwnerAndPrimerLot(load.getOwner(), load.getPrimerLot())).hasSize(1);
    }

    @Test
    void shouldFindByRifle() {
        Rifle rifle = data.rifle();
        Load load = data.newLoad();
        load.setRifle(rifle.getId());
        sut.save(load);

        assertThat(sut.findByOwnerAndRifle(rifle.getOwner(), rifle.getId())).hasSize(1);
    }

    @Test
    void shouldRejectNonPositivePowderCharge() {
        Load load = data.newLoad();
        load.setPowderCharge(BigDecimal.ZERO);

        assertThatThrownBy(() -> sut.save(load))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_load_powder_charge");
    }

    @Test
    void shouldRejectLoadWithoutPrimerLot() {
        Load load = data.newLoad();
        load.setPrimerLot(null);

        assertThatThrownBy(() -> sut.save(load))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldHideLoadsOfOtherUsers() {
        Load load = data.load();
        AppUser other = data.otherOwner();

        assertThat(sut.findByOwner(load.getOwner())).hasSize(1);
        assertThat(sut.findByOwner(other.getId())).isEmpty();
        assertThat(sut.findByIdAndOwner(load.getId(), other.getId())).isEmpty();
    }

    @Test
    void shouldRejectComponentsOfAnotherOwner() {
        Load load = data.newLoad();
        load.setOwner(data.otherOwner().getId());

        assertThatThrownBy(() -> sut.save(load))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_load_");
    }

    @Test
    void shouldRejectRifleOfAnotherOwner() {
        Load load = data.newLoad();
        load.setRifle(data.rifle(data.otherOwner()).getId());

        assertThatThrownBy(() -> sut.save(load))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_load_rifle");
    }
}
