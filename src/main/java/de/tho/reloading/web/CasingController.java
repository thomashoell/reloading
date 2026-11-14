package de.tho.reloading.web;

import de.tho.reloading.entity.Casing;
import de.tho.reloading.repository.CasingRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Master data is immutable (it comes with changesets), so there is only a list. */
@Controller
@RequestMapping("/casings")
public class CasingController {

    private static final String PATH = "/casings";

    private final CasingRepository repository;
    private final MasterDataLookup lookup;

    public CasingController(CasingRepository repository, MasterDataLookup lookup) {
        this.repository = repository;
        this.lookup = lookup;
    }

    @GetMapping
    public String list(@RequestParam(required = false) UUID manufacturer,
                       @RequestParam(required = false) UUID caliber,
                       @RequestParam(required = false) Integer page,
                       @RequestParam(required = false) Integer size,
                       Model model) {
        Map<UUID, String> manufacturers = lookup.manufacturerNames();
        Map<UUID, String> calibers = lookup.caliberNames();
        List<Casing> all = repository.findAll();
        List<CasingRow> rows = all.stream()
                .filter(entity -> manufacturer == null || entity.getManufacturer().equals(manufacturer))
                .filter(entity -> caliber == null || entity.getCaliber().equals(caliber))
                .map(entity -> new CasingRow(entity.getId(), manufacturers.get(entity.getManufacturer()),
                        calibers.get(entity.getCaliber())))
                .sorted(Comparator.comparing(CasingRow::manufacturer, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(CasingRow::caliber, String.CASE_INSENSITIVE_ORDER))
                .toList();

        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("manufacturer", manufacturer);
        filters.put("caliber", caliber);
        model.addAttribute("calibers", lookup.calibers(all.stream().map(Casing::getCaliber).toList()));
        model.addAttribute("selectedCaliber", caliber);
        model.addAttribute("manufacturers", lookup.manufacturers(all.stream().map(Casing::getManufacturer).toList()));
        model.addAttribute("selectedManufacturer", manufacturer);
        model.addAttribute("page", PageView.of(rows, page, size, PATH, filters));
        return "masterdata/casing-list";
    }
}
