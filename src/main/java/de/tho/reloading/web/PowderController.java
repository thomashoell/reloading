package de.tho.reloading.web;

import de.tho.reloading.entity.Powder;
import de.tho.reloading.repository.PowderRepository;
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
    public String list(Model model) {
        Map<UUID, String> manufacturers = lookup.manufacturerNames();
        List<PowderRow> rows = StreamSupport.stream(repository.findAll().spliterator(), false)
                .map(entity -> new PowderRow(entity.getId(), manufacturers.get(entity.getManufacturer()), entity.getName()))
                .sorted(Comparator.comparing(PowderRow::manufacturer, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(PowderRow::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        model.addAttribute("powders", rows);
        return "masterdata/powder-list";
    }

    @GetMapping("/new")
    @PreAuthorize(MasterDataRepository.ADMIN_ONLY)
    public String create(Model model) {
        return form(new PowderForm(), PATH, model);
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize(MasterDataRepository.ADMIN_ONLY)
    public String edit(@PathVariable UUID id, Model model) {
        Powder entity = find(id);
        PowderForm form = new PowderForm();
        form.setManufacturer(entity.getManufacturer());
        form.setName(entity.getName());
        return form(form, PATH + "/" + id, model);
    }

    @PostMapping
    @PreAuthorize(MasterDataRepository.ADMIN_ONLY)
    public String saveNew(@Valid @ModelAttribute("form") PowderForm form, BindingResult result, Model model,
                          RedirectAttributes redirect) {
        return save(new Powder(), PATH, form, result, model, redirect);
    }

    @PostMapping("/{id}")
    @PreAuthorize(MasterDataRepository.ADMIN_ONLY)
    public String saveExisting(@PathVariable UUID id, @Valid @ModelAttribute("form") PowderForm form,
                               BindingResult result, Model model, RedirectAttributes redirect) {
        return save(find(id), PATH + "/" + id, form, result, model, redirect);
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize(MasterDataRepository.ADMIN_ONLY)
    public String delete(@PathVariable UUID id, RedirectAttributes redirect) {
        Powder entity = find(id);
        String label = label(entity);
        try {
            repository.delete(entity);
            redirect.addFlashAttribute("success", "\"" + label + "\" wurde gelöscht.");
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("error", "\"" + label + "\" wird noch verwendet und kann nicht gelöscht werden.");
        }
        return "redirect:" + PATH;
    }

    private String save(Powder entity, String action, PowderForm form, BindingResult result, Model model,
                        RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            entity.setManufacturer(form.getManufacturer());
            entity.setName(form.getName().strip());
            try {
                repository.save(entity);
                redirect.addFlashAttribute("success", "\"" + label(entity) + "\" wurde gespeichert.");
                return "redirect:" + PATH;
            } catch (DuplicateKeyException e) {
                result.reject("duplicate", "Dieses Pulver gibt es bei diesem Hersteller bereits.");
            }
        }
        return form(form, action, model);
    }

    private String form(PowderForm form, String action, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("manufacturers", lookup.manufacturers());
        model.addAttribute("action", action);
        return "masterdata/powder-form";
    }

    private String label(Powder entity) {
        return entity.getName();
    }

    private Powder find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
