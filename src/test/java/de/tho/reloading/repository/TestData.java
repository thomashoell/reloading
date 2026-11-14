package de.tho.reloading.repository;

import de.tho.reloading.entity.Bullet;
import de.tho.reloading.entity.BulletLot;
import de.tho.reloading.entity.Caliber;
import de.tho.reloading.entity.Casing;
import de.tho.reloading.entity.CasingLot;
import de.tho.reloading.entity.Load;
import de.tho.reloading.entity.Manufacturer;
import de.tho.reloading.entity.Powder;
import de.tho.reloading.entity.PowderLot;
import de.tho.reloading.entity.Primer;
import de.tho.reloading.entity.PrimerLot;
import de.tho.reloading.entity.ProcessedCase;
import de.tho.reloading.entity.Rifle;
import de.tho.reloading.utility.PrimerSize;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Saved entities for the repository tests ({@code @Import(TestData.class)}).
 * Manufacturers and calibers are reused by name; every other method creates a new row, so calling the same
 * method twice in one test violates the unique constraints.
 * All measures are SI base units (m, kg).
 */
public class TestData {

    // 1 grain = 0.00006479891 kg, 1 inch = 0.0254 m
    public static final BigDecimal BULLET_DIAMETER = new BigDecimal("0.0067056"); // .264 in
    public static final BigDecimal BULLET_WEIGHT = new BigDecimal("0.0090718474"); // 140 gr
    public static final BigDecimal CASE_LENGTH = new BigDecimal("0.0486");
    public static final BigDecimal POWDER_CHARGE = new BigDecimal("0.002689154765"); // 41.5 gr
    public static final BigDecimal OVERALL_LENGTH = new BigDecimal("0.0712");

    @Autowired
    private ManufacturerRepository manufacturerRepository;
    @Autowired
    private CaliberRepository caliberRepository;
    @Autowired
    private RifleRepository rifleRepository;
    @Autowired
    private CasingRepository casingRepository;
    @Autowired
    private CasingLotRepository casingLotRepository;
    @Autowired
    private BulletRepository bulletRepository;
    @Autowired
    private BulletLotRepository bulletLotRepository;
    @Autowired
    private PowderRepository powderRepository;
    @Autowired
    private PowderLotRepository powderLotRepository;
    @Autowired
    private PrimerRepository primerRepository;
    @Autowired
    private PrimerLotRepository primerLotRepository;
    @Autowired
    private ProcessedCaseRepository processedCaseRepository;
    @Autowired
    private LoadRepository loadRepository;

    public Manufacturer manufacturer(String name) {
        return manufacturerRepository.findByName(name).orElseGet(() -> {
            Manufacturer manufacturer = new Manufacturer();
            manufacturer.setName(name);
            return manufacturerRepository.save(manufacturer);
        });
    }

    public Caliber caliber(String name) {
        return caliberRepository.findByName(name).orElseGet(() -> {
            Caliber caliber = new Caliber();
            caliber.setName(name);
            return caliberRepository.save(caliber);
        });
    }

    public Caliber caliber() {
        return caliber("6.5 Creedmoor");
    }

    public Rifle rifle() {
        Rifle rifle = new Rifle();
        rifle.setName("TRG 22");
        rifle.setManufacturer(manufacturer("Sako").getId());
        rifle.setCaliber(caliber().getId());
        return rifleRepository.save(rifle);
    }

    public Casing casing() {
        Casing casing = new Casing();
        casing.setManufacturer(manufacturer("Lapua").getId());
        casing.setCaliber(caliber().getId());
        return casingRepository.save(casing);
    }

    public CasingLot casingLot() {
        CasingLot lot = new CasingLot();
        lot.setCasing(casing().getId());
        lot.setLotNumber("C-0001");
        return casingLotRepository.save(lot);
    }

    /** Not saved. */
    public Bullet newBullet() {
        Bullet bullet = new Bullet();
        bullet.setManufacturer(manufacturer("Hornady").getId());
        bullet.setName("ELD Match");
        bullet.setDiameter(BULLET_DIAMETER);
        bullet.setWeight(BULLET_WEIGHT);
        return bullet;
    }

    public Bullet bullet() {
        return bulletRepository.save(newBullet());
    }

    public BulletLot bulletLot() {
        BulletLot lot = new BulletLot();
        lot.setBullet(bullet().getId());
        lot.setLotNumber("B-0001");
        return bulletLotRepository.save(lot);
    }

    public Powder powder() {
        Powder powder = new Powder();
        powder.setManufacturer(manufacturer("Vihtavuori").getId());
        powder.setName("N555");
        return powderRepository.save(powder);
    }

    public PowderLot powderLot() {
        PowderLot lot = new PowderLot();
        lot.setPowder(powder().getId());
        lot.setLotNumber("P-0001");
        return powderLotRepository.save(lot);
    }

    public Primer primer() {
        Primer primer = new Primer();
        primer.setManufacturer(manufacturer("CCI").getId());
        primer.setName("BR-4");
        primer.setSize(PrimerSize.SMALL_RIFLE);
        return primerRepository.save(primer);
    }

    public PrimerLot primerLot() {
        PrimerLot lot = new PrimerLot();
        lot.setPrimer(primer().getId());
        lot.setLotNumber("Z-0001");
        return primerLotRepository.save(lot);
    }

    /** Not saved: 50 new, untrimmed cases of a new casing lot. */
    public ProcessedCase newProcessedCase() {
        ProcessedCase processedCase = new ProcessedCase();
        processedCase.setCasingLot(casingLot().getId());
        processedCase.setQuantity(50);
        processedCase.setProcessedOn(LocalDate.of(2026, 11, 1));
        processedCase.setTimesFired(0);
        processedCase.setTrimmed(false);
        processedCase.setCaseLength(CASE_LENGTH);
        return processedCase;
    }

    public ProcessedCase processedCase() {
        return processedCaseRepository.save(newProcessedCase());
    }

    /** Not saved: 20 cartridges made of new lots of every component. */
    public Load newLoad() {
        Load load = new Load();
        load.setProcessedCase(processedCase().getId());
        load.setBulletLot(bulletLot().getId());
        load.setPowderLot(powderLot().getId());
        load.setPrimerLot(primerLot().getId());
        load.setPowderCharge(POWDER_CHARGE);
        load.setOverallLength(OVERALL_LENGTH);
        load.setQuantity(20);
        load.setLoadedOn(LocalDate.of(2026, 11, 7));
        return load;
    }

    public Load load() {
        return loadRepository.save(newLoad());
    }
}
