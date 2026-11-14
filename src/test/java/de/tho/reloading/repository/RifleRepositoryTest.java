package de.tho.reloading.repository;

import de.tho.reloading.entity.AppUser;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJdbcTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestData.class)
public class RifleRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private TestData data;

    @Autowired
    private RifleRepository sut;

    @Test
    void shouldSaveAndFindByNameIgnoringCase() {
        Rifle rifle = data.rifle();
        UUID owner = rifle.getOwner();

        Optional<Rifle> found = sut.findByOwnerAndName(owner, "trg 22");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(rifle.getId());
        assertThat(found.get().getName()).isEqualTo("TRG 22");
        assertThat(found.get().getOwner()).isEqualTo(owner);
        assertThat(found.get().getCaliber()).isEqualTo(rifle.getCaliber());
        assertThat(found.get().getManufacturer()).isEqualTo(rifle.getManufacturer());
    }

    @Test
    void shouldFindByManufacturerAndCaliber() {
        Rifle rifle = data.rifle();

        assertThat(sut.findByOwnerAndManufacturer(rifle.getOwner(), rifle.getManufacturer())).hasSize(1);
        assertThat(sut.findByOwnerAndCaliber(rifle.getOwner(), rifle.getCaliber())).hasSize(1);
    }

    @Test
    void shouldSaveMeasuresInSiUnits() {
        Rifle rifle = data.rifle();
        rifle.setBarrelLength(new BigDecimal("0.66"));
        rifle.setLengthOfTwist(new BigDecimal("0.2032")); // 1:8"
        sut.save(rifle);

        Rifle actual = sut.findByIdAndOwner(rifle.getId(), rifle.getOwner()).orElseThrow();
        assertThat(actual.getBarrelLength()).isEqualByComparingTo("0.66");
        assertThat(actual.getLengthOfTwist()).isEqualByComparingTo("0.2032");
    }

    @Test
    void shouldHideRiflesOfOtherUsers() {
        Rifle rifle = data.rifle();
        AppUser other = data.otherOwner();

        assertThat(sut.findByOwner(rifle.getOwner())).hasSize(1);
        assertThat(sut.findByOwner(other.getId())).isEmpty();
        assertThat(sut.findByIdAndOwner(rifle.getId(), other.getId())).isEmpty();
        assertThat(sut.findByOwnerAndName(other.getId(), "TRG 22")).isEmpty();
    }

    @Test
    void shouldAllowTheSameRifleForDifferentOwners() {
        data.rifle();
        Rifle second = data.rifle(data.otherOwner());

        assertThat(sut.findByOwner(second.getOwner())).hasSize(1);
    }

    @Test
    void shouldRejectDuplicateRifleOfOneOwner() {
        Rifle rifle = data.rifle();
        Rifle duplicate = new Rifle();
        duplicate.setOwner(rifle.getOwner());
        duplicate.setName("trg 22");
        duplicate.setManufacturer(rifle.getManufacturer());
        duplicate.setCaliber(rifle.getCaliber());

        assertThatThrownBy(() -> sut.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_rifle_owner_manufacturer_name_caliber");
    }

    @Test
    void shouldRejectRifleWithoutOwner() {
        Rifle rifle = data.rifle();
        Rifle withoutOwner = new Rifle();
        withoutOwner.setName("85");
        withoutOwner.setManufacturer(rifle.getManufacturer());
        withoutOwner.setCaliber(rifle.getCaliber());

        assertThatThrownBy(() -> sut.save(withoutOwner))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
