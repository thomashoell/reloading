package de.tho.reloading.web;

import de.tho.reloading.entity.Manufacturer;
import de.tho.reloading.repository.ManufacturerRepository;
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
import java.util.UUID;
import java.util.stream.StreamSupport;

/** Master data: everybody may read, only administrators may change. */
@Controller
@RequestMapping("/manufacturers")
public class ManufacturerController {

    private static final String PATH = "/manufacturers";

    private final ManufacturerRepository repository;

    public ManufacturerController(ManufacturerRepository repository) {
        this.repository = repository;
    }

    @ModelAttribute
    public void page(Model model) {
        model.addAttribute("title", "Hersteller");
        model.addAttribute("singular", "Hersteller");
        model.addAttribute("path", PATH);
    }

    @GetMapping
    public String list(Model model) {
        List<NamedItem> items = StreamSupport.stream(repository.findAll().spliterator(), false)
                .map(entity -> new NamedItem(entity.getId(), entity.getName()))
                .sorted(Comparator.comparing(NamedItem::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        model.addAttribute("items", items);
        return "masterdata/named-list";
    }

    @GetMapping("/new")
    @PreAuthorize(MasterDataRepository.ADMIN_ONLY)
    public String create(Model model) {
        model.addAttribute("form", new NameForm());
        model.addAttribute("action", PATH);
        return "masterdata/named-form";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize(MasterDataRepository.ADMIN_ONLY)
    public String edit(@PathVariable UUID id, Model model) {
        NameForm form = new NameForm();
        form.setName(find(id).getName());
        model.addAttribute("form", form);
        model.addAttribute("action", PATH + "/" + id);
        return "masterdata/named-form";
    }

    @PostMapping
    @PreAuthorize(MasterDataRepository.ADMIN_ONLY)
    public String saveNew(@Valid @ModelAttribute("form") NameForm form, BindingResult result, Model model,
                          RedirectAttributes redirect) {
        return save(new Manufacturer(), PATH, form, result, model, redirect);
    }

    @PostMapping("/{id}")
    @PreAuthorize(MasterDataRepository.ADMIN_ONLY)
    public String saveExisting(@PathVariable UUID id, @Valid @ModelAttribute("form") NameForm form,
                               BindingResult result, Model model, RedirectAttributes redirect) {
        return save(find(id), PATH + "/" + id, form, result, model, redirect);
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize(MasterDataRepository.ADMIN_ONLY)
    public String delete(@PathVariable UUID id, RedirectAttributes redirect) {
        Manufacturer entity = find(id);
        try {
            repository.delete(entity);
            redirect.addFlashAttribute("success", "\"" + entity.getName() + "\" wurde gelöscht.");
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("error",
                    "\"" + entity.getName() + "\" wird noch verwendet und kann nicht gelöscht werden.");
        }
        return "redirect:" + PATH;
    }

    private String save(Manufacturer entity, String action, NameForm form, BindingResult result, Model model,
                        RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            entity.setName(form.getName().strip());
            try {
                repository.save(entity);
                redirect.addFlashAttribute("success", "\"" + entity.getName() + "\" wurde gespeichert.");
                return "redirect:" + PATH;
            } catch (DuplicateKeyException e) {
                result.rejectValue("name", "duplicate", "Diesen Namen gibt es bereits.");
            }
        }
        model.addAttribute("action", action);
        return "masterdata/named-form";
    }

    private Manufacturer find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
