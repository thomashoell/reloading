package de.tho.reloading.web;

import de.tho.reloading.entity.AppUser;
import de.tho.reloading.entity.Casing;
import de.tho.reloading.entity.CasingLot;
import de.tho.reloading.repository.CasingLotRepository;
import de.tho.reloading.repository.CasingRepository;
import de.tho.reloading.user.CurrentUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/casing-lots")
public class CasingLotController extends AbstractLotController<CasingLot> {

    private final CasingLotRepository repository;
    private final CasingRepository products;

    public CasingLotController(CasingLotRepository repository, CasingRepository products, MasterDataLookup lookup,
                             CurrentUser currentUser) {
        super(lookup, currentUser);
        this.repository = repository;
        this.products = products;
    }

    @Override
    protected String path() {
        return "/casing-lots";
    }

    @Override
    protected String title() {
        return "Hülsen-Lose";
    }

    @Override
    protected String singular() {
        return "Hülsen-Los";
    }

    @Override
    protected String productLabel() {
        return "Hülse";
    }

    @Override
    protected List<ProductOption> products(AppUser user) {
        Map<UUID, String> manufacturers = lookup.manufacturerNames();
        Map<UUID, String> calibers = lookup.caliberNames();
        return options(products.findAll(),
                casing -> new ProductOption(casing.getId(), manufacturers.get(casing.getManufacturer()),
                        calibers.get(casing.getCaliber())),
                Comparator.comparing((Casing casing) -> calibers.get(casing.getCaliber()), String.CASE_INSENSITIVE_ORDER));
    }

    @Override
    protected List<CasingLot> findByOwner(UUID owner) {
        return repository.findByOwner(owner);
    }

    @Override
    protected Optional<CasingLot> findByIdAndOwner(UUID id, UUID owner) {
        return repository.findByIdAndOwner(id, owner);
    }

    @Override
    protected CasingLot newLot() {
        return new CasingLot();
    }

    @Override
    protected void save(CasingLot lot) {
        repository.save(lot);
    }

    @Override
    protected void delete(CasingLot lot) {
        repository.delete(lot);
    }

    @Override
    protected UUID idOf(CasingLot lot) {
        return lot.getId();
    }

    @Override
    protected UUID productOf(CasingLot lot) {
        return lot.getCasing();
    }

    @Override
    protected String lotNumberOf(CasingLot lot) {
        return lot.getLotNumber();
    }

    @Override
    protected void apply(CasingLot lot, UUID owner, UUID product, String lotNumber) {
        lot.setOwner(owner);
        lot.setCasing(product);
        lot.setLotNumber(lotNumber);
    }
}
