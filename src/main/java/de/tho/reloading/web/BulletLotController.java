package de.tho.reloading.web;

import de.tho.reloading.entity.AppUser;
import de.tho.reloading.entity.Bullet;
import de.tho.reloading.entity.BulletLot;
import de.tho.reloading.repository.BulletLotRepository;
import de.tho.reloading.repository.BulletRepository;
import de.tho.reloading.user.CurrentUser;
import de.tho.reloading.utility.LengthUnit;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/bullet-lots")
public class BulletLotController extends AbstractLotController<BulletLot> {

    private final BulletLotRepository repository;
    private final BulletRepository products;

    public BulletLotController(BulletLotRepository repository, BulletRepository products, MasterDataLookup lookup,
                             CurrentUser currentUser) {
        super(lookup, currentUser);
        this.repository = repository;
        this.products = products;
    }

    @Override
    protected String path() {
        return "/bullet-lots";
    }

    @Override
    protected String title() {
        return "Geschoss-Lose";
    }

    @Override
    protected String singular() {
        return "Geschoss-Los";
    }

    @Override
    protected String productLabel() {
        return "Geschoss";
    }

    @Override
    protected List<ProductOption> products(AppUser user) {
        Map<UUID, String> manufacturers = lookup.manufacturerNames();
        return options(products.findAll(),
                bullet -> new ProductOption(bullet.getId(), manufacturers.get(bullet.getManufacturer()),
                        bullet.getName() + ", " + LengthUnit.formatDiameter(bullet.getDiameter()) + ", "
                                + user.getWeightUnit().format(bullet.getWeight()) + " " + user.getWeightUnit().getSymbol()),
                Comparator.comparing(Bullet::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Bullet::getDiameter)
                        .thenComparing(Bullet::getWeight));
    }

    @Override
    protected List<BulletLot> findByOwner(UUID owner) {
        return repository.findByOwner(owner);
    }

    @Override
    protected Optional<BulletLot> findByIdAndOwner(UUID id, UUID owner) {
        return repository.findByIdAndOwner(id, owner);
    }

    @Override
    protected BulletLot newLot() {
        return new BulletLot();
    }

    @Override
    protected void save(BulletLot lot) {
        repository.save(lot);
    }

    @Override
    protected void delete(BulletLot lot) {
        repository.delete(lot);
    }

    @Override
    protected UUID idOf(BulletLot lot) {
        return lot.getId();
    }

    @Override
    protected UUID productOf(BulletLot lot) {
        return lot.getBullet();
    }

    @Override
    protected String lotNumberOf(BulletLot lot) {
        return lot.getLotNumber();
    }

    @Override
    protected void apply(BulletLot lot, UUID owner, UUID product, String lotNumber) {
        lot.setOwner(owner);
        lot.setBullet(product);
        lot.setLotNumber(lotNumber);
    }
}
