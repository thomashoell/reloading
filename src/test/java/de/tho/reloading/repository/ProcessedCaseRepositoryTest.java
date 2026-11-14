package de.tho.reloading.repository;

import de.tho.reloading.entity.AppUser;
import de.tho.reloading.entity.ProcessedCase;
import de.tho.reloading.entity.Rifle;
import de.tho.reloading.utility.Calibration;
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
public class ProcessedCaseRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private TestData data;

    @Autowired
    private ProcessedCaseRepository sut;

    @Test
    void shouldSaveAndFindByCasingLot() {
        Rifle rifle = data.rifle();
        ProcessedCase processedCase = data.newProcessedCase();
        processedCase.setTimesFired(2);
        processedCase.setFiredFrom(rifle.getId());
        processedCase.setTrimmed(true);
        processedCase.setCalibrationMethod(Calibration.SHOULDER_AND_NECK);
        processedCase.setCalibrationDie("Redding Type S");
        processedCase.setBushing("0.289");
        processedCase.setMandrelUsed(true);
        processedCase.setMandrelDiameter(new BigDecimal("0.00668"));

        sut.save(processedCase);

        List<ProcessedCase> found = sut.findByOwnerAndCasingLot(processedCase.getOwner(), processedCase.getCasingLot());

        assertThat(found).hasSize(1);
        ProcessedCase actual = found.getFirst();
        assertThat(actual.getId()).isNotNull();
        assertThat(actual.getQuantity()).isEqualTo(50);
        assertThat(actual.getProcessedOn()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(actual.getTimesFired()).isEqualTo(2);
        assertThat(actual.getFiredFrom()).isEqualTo(rifle.getId());
        assertThat(actual.getTrimmed()).isTrue();
        assertThat(actual.getCaseLength()).isEqualByComparingTo(TestData.CASE_LENGTH);
        assertThat(actual.getCalibrationMethod()).isEqualTo(Calibration.SHOULDER_AND_NECK);
        assertThat(actual.getCalibrationDie()).isEqualTo("Redding Type S");
        assertThat(actual.getBushing()).isEqualTo("0.289");
        assertThat(actual.getMandrelUsed()).isTrue();
        assertThat(actual.getMandrelDiameter()).isEqualByComparingTo("0.00668");
    }

    @Test
    void shouldSaveMinimalProcessedCase() {
        ProcessedCase saved = data.processedCase();

        ProcessedCase actual = sut.findByIdAndOwner(saved.getId(), saved.getOwner()).orElseThrow();
        assertThat(actual.getTimesFired()).isZero();
        assertThat(actual.getFiredFrom()).isNull();
        assertThat(actual.getCalibrationMethod()).isNull();
        assertThat(actual.getMandrelUsed()).isNull();
    }

    @Test
    void shouldFindByFiredFrom() {
        Rifle rifle = data.rifle();
        ProcessedCase processedCase = data.newProcessedCase();
        processedCase.setTimesFired(1);
        processedCase.setFiredFrom(rifle.getId());
        sut.save(processedCase);

        assertThat(sut.findByOwnerAndFiredFrom(rifle.getOwner(), rifle.getId())).hasSize(1);
    }

    @Test
    void shouldRejectFiredCasesWithoutRifle() {
        ProcessedCase processedCase = data.newProcessedCase();
        processedCase.setTimesFired(1);

        assertThatThrownBy(() -> sut.save(processedCase))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_processed_case_fired_from");
    }

    @Test
    void shouldRejectEmptyBatch() {
        ProcessedCase processedCase = data.newProcessedCase();
        processedCase.setQuantity(0);

        assertThatThrownBy(() -> sut.save(processedCase))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_processed_case_quantity");
    }

    @Test
    void shouldRejectCalibrationMethodWithoutDie() {
        ProcessedCase processedCase = data.newProcessedCase();
        processedCase.setCalibrationMethod(Calibration.NECK);

        assertThatThrownBy(() -> sut.save(processedCase))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_processed_case_calibration_die");
    }

    @Test
    void shouldRejectMandrelWithoutDiameter() {
        ProcessedCase processedCase = data.newProcessedCase();
        processedCase.setMandrelUsed(true);

        assertThatThrownBy(() -> sut.save(processedCase))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_processed_case_mandrel_diameter");
    }

    @Test
    void shouldHideProcessedCasesOfOtherUsers() {
        ProcessedCase processedCase = data.processedCase();
        AppUser other = data.otherOwner();

        assertThat(sut.findByOwner(processedCase.getOwner())).hasSize(1);
        assertThat(sut.findByOwner(other.getId())).isEmpty();
        assertThat(sut.findByIdAndOwner(processedCase.getId(), other.getId())).isEmpty();
    }

    @Test
    void shouldRejectCasingLotOfAnotherOwner() {
        ProcessedCase processedCase = data.newProcessedCase();
        processedCase.setOwner(data.otherOwner().getId());

        assertThatThrownBy(() -> sut.save(processedCase))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_processed_case_casing_lot");
    }

    @Test
    void shouldRejectRifleOfAnotherOwner() {
        ProcessedCase processedCase = data.newProcessedCase();
        processedCase.setTimesFired(1);
        processedCase.setFiredFrom(data.rifle(data.otherOwner()).getId());

        assertThatThrownBy(() -> sut.save(processedCase))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_processed_case_rifle");
    }
}
