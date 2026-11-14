package de.tho.reloading.repository;

import de.tho.reloading.entity.Caliber;
import de.tho.reloading.entity.Case;
import de.tho.reloading.entity.Manufacturer;
import de.tho.reloading.entity.ProcessedCase;
import de.tho.reloading.entity.Rifle;
import de.tho.reloading.utility.Calibration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJdbcTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ProcessedCaseRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private CaliberRepository caliberRepository;

    @Autowired
    private ManufacturerRepository manufacturerRepository;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private RifleRepository rifleRepository;

    @Autowired
    private ProcessedCaseRepository sut;

    private Case parentCase;
    private Rifle rifle;

    @BeforeEach
    void setUp() {
        Caliber caliber = new Caliber();
        caliber.setName("6.5 Creedmoor");
        caliber = caliberRepository.save(caliber);

        Manufacturer manufacturer = new Manufacturer();
        manufacturer.setName("Lapua");
        manufacturer = manufacturerRepository.save(manufacturer);

        parentCase = new Case();
        parentCase.setCaliber(caliber.getId());
        parentCase.setManufacturer(manufacturer.getId());
        parentCase.setLotNumber("ABC1234");
        parentCase = caseRepository.save(parentCase);

        rifle = new Rifle();
        rifle.setName("TRG 22");
        rifle.setCaliber(caliber.getId());
        rifle.setManufacturer(manufacturer.getId());
        rifle = rifleRepository.save(rifle);
    }

    private ProcessedCase minimal() {
        ProcessedCase processedCase = new ProcessedCase();
        processedCase.setParentCase(parentCase.getId());
        processedCase.setTrimmed(false);
        processedCase.setCaseLength(48.6);
        return processedCase;
    }

    @Test
    void shouldSaveAndFindByParentCase() {
        ProcessedCase processedCase = minimal();
        processedCase.setFired(true);
        processedCase.setFiredFrom(rifle.getId());
        processedCase.setTrimmed(true);
        processedCase.setCalibrationMethod(Calibration.SHOULDER_AND_NECK);
        processedCase.setCalibrationDie("Redding Type S");
        processedCase.setBushing("0.289");
        processedCase.setMandrelUsed(true);
        processedCase.setMandrelDiameter(6.68);

        sut.save(processedCase);

        List<ProcessedCase> found = sut.findByParentCase(parentCase.getId());

        assertThat(found).hasSize(1);
        ProcessedCase actual = found.getFirst();
        assertThat(actual.getId()).isNotNull();
        assertThat(actual.getFired()).isTrue();
        assertThat(actual.getFiredFrom()).isEqualTo(rifle.getId());
        assertThat(actual.getTrimmed()).isTrue();
        assertThat(actual.getCaseLength()).isEqualTo(48.6);
        assertThat(actual.getCalibrationMethod()).isEqualTo(Calibration.SHOULDER_AND_NECK);
        assertThat(actual.getCalibrationDie()).isEqualTo("Redding Type S");
        assertThat(actual.getBushing()).isEqualTo("0.289");
        assertThat(actual.getMandrelUsed()).isTrue();
        assertThat(actual.getMandrelDiameter()).isEqualTo(6.68);
    }

    @Test
    void shouldSaveMinimalProcessedCase() {
        ProcessedCase saved = sut.save(minimal());

        ProcessedCase actual = sut.findById(saved.getId()).orElseThrow();
        assertThat(actual.getFired()).isNull();
        assertThat(actual.getFiredFrom()).isNull();
        assertThat(actual.getCalibrationMethod()).isNull();
        assertThat(actual.getMandrelUsed()).isNull();
    }

    @Test
    void shouldFindByFiredFrom() {
        ProcessedCase processedCase = minimal();
        processedCase.setFired(true);
        processedCase.setFiredFrom(rifle.getId());
        sut.save(processedCase);
        sut.save(minimal());

        assertThat(sut.findByFiredFrom(rifle.getId())).hasSize(1);
    }

    @Test
    void shouldRejectFiredCaseWithoutRifle() {
        ProcessedCase processedCase = minimal();
        processedCase.setFired(true);

        assertThatThrownBy(() -> sut.save(processedCase))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_processed_case_fired_from");
    }

    @Test
    void shouldRejectCalibrationMethodWithoutDie() {
        ProcessedCase processedCase = minimal();
        processedCase.setCalibrationMethod(Calibration.NECK);

        assertThatThrownBy(() -> sut.save(processedCase))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_processed_case_calibration_die");
    }

    @Test
    void shouldRejectMandrelWithoutDiameter() {
        ProcessedCase processedCase = minimal();
        processedCase.setMandrelUsed(true);

        assertThatThrownBy(() -> sut.save(processedCase))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_processed_case_mandrel_diameter");
    }
}
