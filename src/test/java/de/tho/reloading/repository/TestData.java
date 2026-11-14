package de.tho.reloading.repository;

import de.tho.reloading.entity.AppUser;
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
import de.tho.reloading.utility.UserRole;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Saved entities for the repository tests ({@code @Import(TestData.class)}).
 * Master data (manufacturers, calibers, products) is immutable and comes from the seed changesets, so those
 * methods only look rows up. Users are reused; every method for user data creates a new row, so calling the same
 * one twice in one test violates the unique constraints. User data belongs to {@link #owner()} unless stated
 * otherwise.
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
    private AppUserRepository appUserRepository;
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

    public AppUser user(String subject) {
        return appUserRepository.findBySubject(subject).orElseGet(() -> {
            AppUser user = new AppUser();
            user.setSubject(subject);
            user.setUsername(subject);
            user.setRole(UserRole.USER);
            return appUserRepository.save(user);
        });
    }

    public AppUser owner() {
        return user("owner");
    }

    public AppUser otherOwner() {
        return user("other-owner");
    }

    public Manufacturer manufacturer(String name) {
        return manufacturerRepository.findByName(name).orElseThrow();
    }

    public Caliber caliber(String name) {
        return caliberRepository.findByName(name).orElseThrow();
    }

    public Caliber caliber() {
        return caliber("6.5 Creedmoor");
    }

    public Rifle rifle() {
        return rifle(owner());
    }

    public Rifle rifle(AppUser owner) {
        Rifle rifle = new Rifle();
        rifle.setOwner(owner.getId());
        rifle.setName("TRG 22");
        rifle.setManufacturer(manufacturer("Sako").getId());
        rifle.setCaliber(caliber().getId());
        return rifleRepository.save(rifle);
    }

    public Casing casing() {
        UUID caliber = caliber().getId();
        return casingRepository.findByManufacturer(manufacturer("Lapua").getId()).stream()
                .filter(casing -> casing.getCaliber().equals(caliber))
                .findFirst().orElseThrow();
    }

    public CasingLot casingLot() {
        return casingLot(owner());
    }

    public CasingLot casingLot(AppUser owner) {
        CasingLot lot = new CasingLot();
        lot.setOwner(owner.getId());
        lot.setCasing(casing().getId());
        lot.setLotNumber("C-0001");
        return casingLotRepository.save(lot);
    }

    /** Hornady ELD Match, .264 in, 140 gr. */
    public Bullet bullet() {
        UUID hornady = manufacturer("Hornady").getId();
        return bulletRepository.findByName("ELD Match").stream()
                .filter(bullet -> bullet.getManufacturer().equals(hornady))
                .filter(bullet -> bullet.getDiameter().compareTo(BULLET_DIAMETER) == 0)
                .filter(bullet -> bullet.getWeight().compareTo(BULLET_WEIGHT) == 0)
                .findFirst().orElseThrow();
    }

    public BulletLot bulletLot() {
        BulletLot lot = new BulletLot();
        lot.setOwner(owner().getId());
        lot.setBullet(bullet().getId());
        lot.setLotNumber("B-0001");
        return bulletLotRepository.save(lot);
    }

    public Powder powder() {
        return powderRepository.findByName("N555").getFirst();
    }

    public PowderLot powderLot() {
        PowderLot lot = new PowderLot();
        lot.setOwner(owner().getId());
        lot.setPowder(powder().getId());
        lot.setLotNumber("P-0001");
        return powderLotRepository.save(lot);
    }

    public Primer primer() {
        return primerRepository.findByName("BR-4").getFirst();
    }

    public PrimerLot primerLot() {
        PrimerLot lot = new PrimerLot();
        lot.setOwner(owner().getId());
        lot.setPrimer(primer().getId());
        lot.setLotNumber("Z-0001");
        return primerLotRepository.save(lot);
    }

    /** Not saved: 50 new, untrimmed cases of a new casing lot. */
    public ProcessedCase newProcessedCase() {
        ProcessedCase processedCase = new ProcessedCase();
        processedCase.setOwner(owner().getId());
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
        load.setOwner(owner().getId());
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
