package de.tho.reloading.web;

import de.tho.reloading.entity.AppUser;
import de.tho.reloading.entity.Bullet;
import de.tho.reloading.repository.BulletRepository;
import de.tho.reloading.user.CurrentUser;
import de.tho.reloading.utility.LengthUnit;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Master data is immutable (it comes with changesets), so there is only a list. */
@Controller
@RequestMapping("/bullets")
public class BulletController {

    private static final String PATH = "/bullets";

    private final BulletRepository repository;
    private final MasterDataLookup lookup;
    private final CurrentUser currentUser;

    public BulletController(BulletRepository repository, MasterDataLookup lookup, CurrentUser currentUser) {
        this.repository = repository;
        this.lookup = lookup;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String list(@RequestParam(required = false) UUID manufacturer,
                       @RequestParam(required = false) BigDecimal diameter,
                       @RequestParam(required = false) BigDecimal weight,
                       @RequestParam(required = false) Integer page,
                       @RequestParam(required = false) Integer size,
                       Model model) {
        Map<UUID, String> manufacturers = lookup.manufacturerNames();
        AppUser user = currentUser.get();
        List<Bullet> all = repository.findAll();
        // the weight filter only offers what is left after the other two filters
        List<Bullet> withoutWeightFilter = all.stream()
                .filter(entity -> manufacturer == null || entity.getManufacturer().equals(manufacturer))
                .filter(entity -> diameter == null || entity.getDiameter().compareTo(diameter) == 0)
                .toList();
        List<BulletRow> rows = withoutWeightFilter.stream()
                .filter(entity -> weight == null || entity.getWeight().compareTo(weight) == 0)
                .map(entity -> new BulletRow(
                        entity.getId(),
                        manufacturers.get(entity.getManufacturer()),
                        entity.getName(),
                        LengthUnit.formatDiameter(entity.getDiameter()),
                        user.getWeightUnit().format(entity.getWeight()),
                        user.getLengthUnit().format(entity.getLength()),
                        entity.getDiameter(),
                        entity.getWeight()))
                .sorted(Comparator.comparing(BulletRow::manufacturer, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(BulletRow::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(BulletRow::diameterSi)
                        .thenComparing(BulletRow::weightSi))
                .toList();

        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("manufacturer", manufacturer);
        // the diameter and weight filters use the SI values, so they don't depend on the units of the user
        filters.put("diameter", diameter == null ? null : plain(diameter));
        filters.put("weight", weight == null ? null : plain(weight));
        List<FilterOption> diameters = all.stream()
                .map(Bullet::getDiameter)
                .map(BigDecimal::stripTrailingZeros)
                .distinct()
                .sorted()
                .map(value -> new FilterOption(plain(value), LengthUnit.formatDiameter(value)))
                .toList();
        model.addAttribute("diameters", diameters);
        model.addAttribute("selectedDiameter", diameter == null ? null : plain(diameter));
        List<FilterOption> weights = withoutWeightFilter.stream()
                .map(Bullet::getWeight)
                .map(BigDecimal::stripTrailingZeros)
                .distinct()
                .sorted()
                .map(value -> new FilterOption(plain(value),
                        user.getWeightUnit().format(value) + " " + user.getWeightUnit().getSymbol()))
                .toList();
        model.addAttribute("weights", weights);
        model.addAttribute("selectedWeight", weight == null ? null : plain(weight));
        model.addAttribute("manufacturers", lookup.manufacturers(all.stream().map(Bullet::getManufacturer).toList()));
        model.addAttribute("selectedManufacturer", manufacturer);
        model.addAttribute("page", PageView.of(rows, page, size, PATH, filters));
        return "masterdata/bullet-list";
    }

    /** The same text for equal values, whatever their scale (0.00670560 and 0.0067056). */
    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
