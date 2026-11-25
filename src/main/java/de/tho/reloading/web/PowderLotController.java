package de.tho.reloading.web;

import de.tho.reloading.entity.AppUser;
import de.tho.reloading.entity.Powder;
import de.tho.reloading.entity.PowderLot;
import de.tho.reloading.repository.PowderLotRepository;
import de.tho.reloading.repository.PowderRepository;
import de.tho.reloading.user.CurrentUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/powder-lots")
public class PowderLotController extends AbstractLotController<PowderLot> {

    private final PowderLotRepository repository;
    private final PowderRepository products;

    public PowderLotController(PowderLotRepository repository, PowderRepository products, MasterDataLookup lookup,
                             CurrentUser currentUser) {
        super(lookup, currentUser);
        this.repository = repository;
        this.products = products;
    }

    @Override
    protected String path() {
        return "/powder-lots";
    }

    @Override
    protected String title() {
        return "Pulver-Lose";
    }

    @Override
    protected String singular() {
        return "Pulver-Los";
    }

    @Override
    protected String productLabel() {
        return "Pulver";
    }

    @Override
    protected List<ProductOption> products(AppUser user) {
        Map<UUID, String> manufacturers = lookup.manufacturerNames();
        return options(products.findAll(),
                powder -> new ProductOption(powder.getId(), manufacturers.get(powder.getManufacturer()), powder.getName()),
                Comparator.comparing(Powder::getName, String.CASE_INSENSITIVE_ORDER));
    }

    @Override
    protected List<PowderLot> findByOwner(UUID owner) {
        return repository.findByOwner(owner);
    }

    @Override
    protected Optional<PowderLot> findByIdAndOwner(UUID id, UUID owner) {
        return repository.findByIdAndOwner(id, owner);
    }

    @Override
    protected PowderLot newLot() {
        return new PowderLot();
    }

    @Override
    protected void save(PowderLot lot) {
        repository.save(lot);
    }

    @Override
    protected void delete(PowderLot lot) {
        repository.delete(lot);
    }

    @Override
    protected UUID idOf(PowderLot lot) {
        return lot.getId();
    }

    @Override
    protected UUID productOf(PowderLot lot) {
        return lot.getPowder();
    }

    @Override
    protected String lotNumberOf(PowderLot lot) {
        return lot.getLotNumber();
    }

    @Override
    protected void apply(PowderLot lot, UUID owner, UUID product, String lotNumber) {
        lot.setOwner(owner);
        lot.setPowder(product);
        lot.setLotNumber(lotNumber);
    }
}
