package de.tho.reloading.web;

import de.tho.reloading.entity.AppUser;
import de.tho.reloading.entity.Primer;
import de.tho.reloading.entity.PrimerLot;
import de.tho.reloading.repository.PrimerLotRepository;
import de.tho.reloading.repository.PrimerRepository;
import de.tho.reloading.user.CurrentUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/primer-lots")
public class PrimerLotController extends AbstractLotController<PrimerLot> {

    private final PrimerLotRepository repository;
    private final PrimerRepository products;

    public PrimerLotController(PrimerLotRepository repository, PrimerRepository products, MasterDataLookup lookup,
                             CurrentUser currentUser) {
        super(lookup, currentUser);
        this.repository = repository;
        this.products = products;
    }

    @Override
    protected String path() {
        return "/primer-lots";
    }

    @Override
    protected String title() {
        return "Zündhütchen-Lose";
    }

    @Override
    protected String singular() {
        return "Zündhütchen-Los";
    }

    @Override
    protected String productLabel() {
        return "Zündhütchen";
    }

    @Override
    protected List<ProductOption> products(AppUser user) {
        Map<UUID, String> manufacturers = lookup.manufacturerNames();
        return options(products.findAll(),
                primer -> new ProductOption(primer.getId(), manufacturers.get(primer.getManufacturer()),
                        primer.getName() + " (" + primer.getSize().getLabel() + ")"),
                Comparator.comparing(Primer::getName, String.CASE_INSENSITIVE_ORDER));
    }

    @Override
    protected List<PrimerLot> findByOwner(UUID owner) {
        return repository.findByOwner(owner);
    }

    @Override
    protected Optional<PrimerLot> findByIdAndOwner(UUID id, UUID owner) {
        return repository.findByIdAndOwner(id, owner);
    }

    @Override
    protected PrimerLot newLot() {
        return new PrimerLot();
    }

    @Override
    protected void save(PrimerLot lot) {
        repository.save(lot);
    }

    @Override
    protected void delete(PrimerLot lot) {
        repository.delete(lot);
    }

    @Override
    protected UUID idOf(PrimerLot lot) {
        return lot.getId();
    }

    @Override
    protected UUID productOf(PrimerLot lot) {
        return lot.getPrimer();
    }

    @Override
    protected String lotNumberOf(PrimerLot lot) {
        return lot.getLotNumber();
    }

    @Override
    protected void apply(PrimerLot lot, UUID owner, UUID product, String lotNumber) {
        lot.setOwner(owner);
        lot.setPrimer(product);
        lot.setLotNumber(lotNumber);
    }
}
