package de.tho.reloading.repository;

import de.tho.reloading.entity.BulletLot;
import de.tho.reloading.entity.CasingLot;
import de.tho.reloading.entity.PowderLot;
import de.tho.reloading.entity.PrimerLot;
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
public class LotRepositoriesTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private TestData data;

    @Autowired
    private CasingLotRepository casingLotRepository;

    @Autowired
    private BulletLotRepository bulletLotRepository;

    @Autowired
    private PowderLotRepository powderLotRepository;

    @Autowired
    private PrimerLotRepository primerLotRepository;

    @Test
    void shouldSaveAndFindCasingLot() {
        CasingLot lot = data.casingLot();

        assertThat(lot.getId()).isNotNull();
        assertThat(casingLotRepository.findByCasing(lot.getCasing())).hasSize(1);
        Optional<CasingLot> found = casingLotRepository.findByCasingAndLotNumber(lot.getCasing(), "c-0001");
        assertThat(found).isPresent();
        assertThat(found.get().getLotNumber()).isEqualTo("C-0001");
    }

    @Test
    void shouldSaveAndFindBulletLot() {
        BulletLot lot = data.bulletLot();

        assertThat(lot.getId()).isNotNull();
        assertThat(bulletLotRepository.findByBullet(lot.getBullet())).hasSize(1);
        assertThat(bulletLotRepository.findByBulletAndLotNumber(lot.getBullet(), "b-0001")).isPresent();
    }

    @Test
    void shouldSaveAndFindPowderLot() {
        PowderLot lot = data.powderLot();

        assertThat(lot.getId()).isNotNull();
        assertThat(powderLotRepository.findByPowder(lot.getPowder())).hasSize(1);
        assertThat(powderLotRepository.findByPowderAndLotNumber(lot.getPowder(), "p-0001")).isPresent();
    }

    @Test
    void shouldSaveAndFindPrimerLot() {
        PrimerLot lot = data.primerLot();

        assertThat(lot.getId()).isNotNull();
        assertThat(primerLotRepository.findByPrimer(lot.getPrimer())).hasSize(1);
        assertThat(primerLotRepository.findByPrimerAndLotNumber(lot.getPrimer(), "z-0001")).isPresent();
    }

    @Test
    void shouldAllowSeveralLotsOfOneProduct() {
        CasingLot lot = data.casingLot();
        CasingLot second = new CasingLot();
        second.setCasing(lot.getCasing());
        second.setLotNumber("C-0002");
        casingLotRepository.save(second);

        assertThat(casingLotRepository.findByCasing(lot.getCasing())).hasSize(2);
    }

    @Test
    void shouldRejectDuplicateLotNumberIgnoringCase() {
        CasingLot lot = data.casingLot();
        CasingLot duplicate = new CasingLot();
        duplicate.setCasing(lot.getCasing());
        duplicate.setLotNumber("c-0001");

        assertThatThrownBy(() -> casingLotRepository.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_casing_lot_casing_lot_number");
    }

    @Test
    void shouldRejectLotWithoutLotNumber() {
        PowderLot lot = new PowderLot();
        lot.setPowder(data.powder().getId());

        assertThatThrownBy(() -> powderLotRepository.save(lot))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
