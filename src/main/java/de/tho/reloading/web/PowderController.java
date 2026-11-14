package de.tho.reloading.web;

import de.tho.reloading.entity.Powder;
import de.tho.reloading.repository.PowderRepository;
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
@RequestMapping("/powders")
public class PowderController {

    private static final String PATH = "/powders";

    private final PowderRepository repository;
    private final MasterDataLookup lookup;

    public PowderController(PowderRepository repository, MasterDataLookup lookup) {
        this.repository = repository;
        this.lookup = lookup;
    }

    @GetMapping
    public String list(@RequestParam(required = false) UUID manufacturer,
                       @RequestParam(required = false) Integer page,
                       @RequestParam(required = false) Integer size,
                       Model model) {
        Map<UUID, String> manufacturers = lookup.manufacturerNames();
        List<Powder> all = repository.findAll();
        List<PowderRow> rows = all.stream()
                .filter(entity -> manufacturer == null || entity.getManufacturer().equals(manufacturer))
                .map(entity -> new PowderRow(entity.getId(), manufacturers.get(entity.getManufacturer()), entity.getName()))
                .sorted(Comparator.comparing(PowderRow::manufacturer, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(PowderRow::name, String.CASE_INSENSITIVE_ORDER))
                .toList();

        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("manufacturer", manufacturer);
        model.addAttribute("manufacturers", lookup.manufacturers(all.stream().map(Powder::getManufacturer).toList()));
        model.addAttribute("selectedManufacturer", manufacturer);
        model.addAttribute("page", PageView.of(rows, page, size, PATH, filters));
        return "masterdata/powder-list";
    }
}
