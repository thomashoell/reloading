package de.tho.reloading.repository;

import de.tho.reloading.entity.Bullet;
import de.tho.reloading.entity.Casing;
import de.tho.reloading.entity.Manufacturer;
import de.tho.reloading.entity.Powder;
import de.tho.reloading.entity.Primer;
import de.tho.reloading.utility.PrimerSize;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Master data is immutable and comes from the seed changesets (1xx-seed-*.yaml); the repositories only read. */
@DataJdbcTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestData.class)
public class MasterDataRepositoriesTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private TestData data;

    @Autowired
    private ManufacturerRepository manufacturerRepository;

    @Autowired
    private CaliberRepository caliberRepository;

    @Autowired
    private CasingRepository casingRepository;

    @Autowired
    private BulletRepository bulletRepository;

    @Autowired
    private PowderRepository powderRepository;

    @Autowired
    private PrimerRepository primerRepository;

    @Test
    void shouldFindSeededManufacturerIgnoringCase() {
        Manufacturer lapua = manufacturerRepository.findByName("lapua").orElseThrow();

        assertThat(lapua.getId()).isNotNull();
        assertThat(lapua.getName()).isEqualTo("Lapua");
        assertThat(manufacturerRepository.findById(lapua.getId())).isPresent();
        assertThat(manufacturerRepository.findAll()).hasSizeGreaterThan(10);
        assertThat(manufacturerRepository.findByName("No Such Manufacturer")).isEmpty();
    }

    @Test
    void shouldFindSeededCaliberIgnoringCase() {
        assertThat(caliberRepository.findByName("6.5 creedmoor")).isPresent();
        assertThat(caliberRepository.findByName(".30-06 Springfield")).isPresent();
        assertThat(caliberRepository.findAll()).hasSizeGreaterThan(10);
    }

    @Test
    void shouldFindSeededCasing() {
        Casing casing = data.casing();

        assertThat(casing.getManufacturer()).isEqualTo(data.manufacturer("Lapua").getId());
        assertThat(casing.getCaliber()).isEqualTo(data.caliber("6.5 Creedmoor").getId());
        assertThat(casingRepository.findByCaliber(casing.getCaliber())).hasSizeGreaterThan(1);
    }

    @Test
    void shouldStoreSeededBulletsInSiUnits() {
        Bullet bullet = data.bullet();

        // listed as .264 in and 140 gr in the changeset
        assertThat(bullet.getDiameter()).isEqualByComparingTo("0.0067056");
        assertThat(bullet.getWeight()).isEqualByComparingTo("0.0090718474");
        assertThat(bulletRepository.findByDiameter(new BigDecimal("0.0067056"))).hasSizeGreaterThan(1);
        assertThat(bulletRepository.findByManufacturer(bullet.getManufacturer())).hasSizeGreaterThan(5);
        assertThat(bulletRepository.findAll()).allSatisfy(each -> {
            assertThat(each.getDiameter()).isPositive();
            assertThat(each.getWeight()).isPositive();
        });
    }

    @Test
    void shouldFindSeededPowder() {
        Powder powder = data.powder();

        assertThat(powder.getName()).isEqualTo("N555");
        assertThat(powder.getManufacturer()).isEqualTo(data.manufacturer("Vihtavuori").getId());
        assertThat(powderRepository.findByManufacturer(powder.getManufacturer())).hasSizeGreaterThan(5);
    }

    @Test
    void shouldFindSeededPrimerWithSize() {
        Primer primer = data.primer();

        assertThat(primer.getName()).isEqualTo("BR-4");
        assertThat(primer.getSize()).isEqualTo(PrimerSize.SMALL_RIFLE);
        assertThat(primer.getManufacturer()).isEqualTo(data.manufacturer("CCI").getId());
        assertThat(primerRepository.findByManufacturer(primer.getManufacturer())).hasSizeGreaterThan(5);
    }
}
