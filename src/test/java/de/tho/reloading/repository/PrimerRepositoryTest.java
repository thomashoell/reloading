package de.tho.reloading.repository;

import de.tho.reloading.entity.Primer;
import de.tho.reloading.utility.PrimerSize;
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
public class PrimerRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private TestData data;

    @Autowired
    private PrimerRepository sut;

    @Test
    void shouldSaveAndFindByNameIgnoringCase() {
        Primer primer = data.primer();

        List<Primer> found = sut.findByName("br-4");

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getId()).isEqualTo(primer.getId());
        assertThat(found.getFirst().getName()).isEqualTo("BR-4");
        assertThat(found.getFirst().getSize()).isEqualTo(PrimerSize.SMALL_RIFLE);
        assertThat(found.getFirst().getManufacturer()).isEqualTo(primer.getManufacturer());
    }

    @Test
    void shouldFindByManufacturer() {
        Primer primer = data.primer();

        assertThat(sut.findByManufacturer(primer.getManufacturer())).hasSize(1);
    }

    @Test
    void shouldRejectDuplicatePrimer() {
        Primer primer = data.primer();
        Primer duplicate = new Primer();
        duplicate.setManufacturer(primer.getManufacturer());
        duplicate.setName(primer.getName());
        duplicate.setSize(PrimerSize.SMALL_RIFLE_MAGNUM);

        assertThatThrownBy(() -> sut.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_primer_manufacturer_name");
    }
}
