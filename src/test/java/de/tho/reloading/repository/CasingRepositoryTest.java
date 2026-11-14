package de.tho.reloading.repository;

import de.tho.reloading.entity.Casing;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJdbcTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestData.class)
public class CasingRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private TestData data;

    @Autowired
    private CasingRepository sut;

    @Test
    void shouldSaveAndFindByManufacturer() {
        Casing casing = data.casing();

        List<Casing> found = sut.findByManufacturer(casing.getManufacturer());

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getId()).isNotNull();
        assertThat(found.getFirst().getCaliber()).isEqualTo(casing.getCaliber());
    }

    @Test
    void shouldFindByCaliber() {
        Casing casing = data.casing();

        assertThat(sut.findByCaliber(casing.getCaliber())).hasSize(1);
    }

    @Test
    void shouldRejectDuplicateCasing() {
        Casing casing = data.casing();
        Casing duplicate = new Casing();
        duplicate.setManufacturer(casing.getManufacturer());
        duplicate.setCaliber(casing.getCaliber());

        assertThatThrownBy(() -> sut.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_casing_manufacturer_caliber");
    }
}
