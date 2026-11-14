package de.tho.reloading.web;

import de.tho.reloading.repository.ManufacturerRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Master data is immutable (it comes with changesets), so there is only a list. */
@Controller
@RequestMapping("/manufacturers")
public class ManufacturerController {

    private static final String PATH = "/manufacturers";

    private final ManufacturerRepository repository;

    public ManufacturerController(ManufacturerRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) Integer page,
                       @RequestParam(required = false) Integer size,
                       Model model) {
        String search = q == null ? "" : q.strip().toLowerCase(Locale.ROOT);
        List<NamedItem> items = repository.findAll().stream()
                .map(entity -> new NamedItem(entity.getId(), entity.getName()))
                .filter(item -> item.name().toLowerCase(Locale.ROOT).contains(search))
                .sorted(Comparator.comparing(NamedItem::name, String.CASE_INSENSITIVE_ORDER))
                .toList();

        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("q", search);
        model.addAttribute("title", "Hersteller");
        model.addAttribute("path", PATH);
        model.addAttribute("q", search);
        model.addAttribute("page", PageView.of(items, page, size, PATH, filters));
        return "masterdata/named-list";
    }
}
