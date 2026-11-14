package de.tho.reloading.repository;

import de.tho.reloading.entity.Powder;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJdbcTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestData.class)
public class PowderRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private TestData data;

    @Autowired
    private PowderRepository sut;

    @Test
    void shouldSaveAndFindByNameIgnoringCase() {
        Powder powder = data.powder();

        Optional<Powder> found = sut.findByName("n555");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(powder.getId());
        assertThat(found.get().getName()).isEqualTo("N555");
        assertThat(found.get().getManufacturer()).isEqualTo(powder.getManufacturer());
    }

    @Test
    void shouldFindByManufacturer() {
        Powder powder = data.powder();

        assertThat(sut.findByManufacturer(powder.getManufacturer())).hasSize(1);
    }

    @Test
    void shouldRejectDuplicatePowder() {
        Powder powder = data.powder();
        Powder duplicate = new Powder();
        duplicate.setManufacturer(powder.getManufacturer());
        duplicate.setName("n555");

        assertThatThrownBy(() -> sut.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_powder_manufacturer_name");
    }
}
