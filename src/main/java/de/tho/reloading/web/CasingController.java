package de.tho.reloading.web;

import de.tho.reloading.entity.Casing;
import de.tho.reloading.repository.CasingRepository;
import de.tho.reloading.repository.MasterDataRepository;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

/** Master data: everybody may read, only administrators may change. */
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
    public String list(Model model) {
        Map<UUID, String> manufacturers = lookup.manufacturerNames();
        Map<UUID, String> calibers = lookup.caliberNames();
        List<CasingRow> rows = StreamSupport.stream(repository.findAll().spliterator(), false)
                .map(entity -> new CasingRow(entity.getId(), manufacturers.get(entity.getManufacturer()),
                        calibers.get(entity.getCaliber())))
                .sorted(Comparator.comparing(CasingRow::manufacturer, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(CasingRow::caliber, String.CASE_INSENSITIVE_ORDER))
                .toList();
        model.addAttribute("casings", rows);
        return "masterdata/casing-list";
    }

    @GetMapping("/new")
    @PreAuthorize(MasterDataRepository.ADMIN_ONLY)
    public String create(Model model) {
        return form(new CasingForm(), PATH, model);
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize(MasterDataRepository.ADMIN_ONLY)
    public String edit(@PathVariable UUID id, Model model) {
        Casing entity = find(id);
        CasingForm form = new CasingForm();
        form.setManufacturer(entity.getManufacturer());
        form.setCaliber(entity.getCaliber());
        return form(form, PATH + "/" + id, model);
    }

    @PostMapping
    @PreAuthorize(MasterDataRepository.ADMIN_ONLY)
    public String saveNew(@Valid @ModelAttribute("form") CasingForm form, BindingResult result, Model model,
                          RedirectAttributes redirect) {
        return save(new Casing(), PATH, form, result, model, redirect);
    }

    @PostMapping("/{id}")
    @PreAuthorize(MasterDataRepository.ADMIN_ONLY)
    public String saveExisting(@PathVariable UUID id, @Valid @ModelAttribute("form") CasingForm form,
                               BindingResult result, Model model, RedirectAttributes redirect) {
        return save(find(id), PATH + "/" + id, form, result, model, redirect);
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize(MasterDataRepository.ADMIN_ONLY)
    public String delete(@PathVariable UUID id, RedirectAttributes redirect) {
        Casing entity = find(id);
        String label = label(entity);
        try {
            repository.delete(entity);
            redirect.addFlashAttribute("success", "\"" + label + "\" wurde gelöscht.");
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("error", "\"" + label + "\" wird noch verwendet und kann nicht gelöscht werden.");
        }
        return "redirect:" + PATH;
    }

    private String save(Casing entity, String action, CasingForm form, BindingResult result, Model model,
                        RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            entity.setManufacturer(form.getManufacturer());
            entity.setCaliber(form.getCaliber());
            try {
                repository.save(entity);
                redirect.addFlashAttribute("success", "\"" + label(entity) + "\" wurde gespeichert.");
                return "redirect:" + PATH;
            } catch (DuplicateKeyException e) {
                result.reject("duplicate", "Diese Hülse (Hersteller und Kaliber) gibt es bereits.");
            }
        }
        return form(form, action, model);
    }

    private String form(CasingForm form, String action, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("manufacturers", lookup.manufacturers());
        model.addAttribute("calibers", lookup.calibers());
        model.addAttribute("action", action);
        return "masterdata/casing-form";
    }

    private String label(Casing entity) {
        return lookup.manufacturerNames().get(entity.getManufacturer()) + " " + lookup.caliberNames().get(entity.getCaliber());
    }

    private Casing find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
