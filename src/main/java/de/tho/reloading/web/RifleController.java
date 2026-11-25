package de.tho.reloading.web;

import de.tho.reloading.entity.AppUser;
import de.tho.reloading.entity.Rifle;
import de.tho.reloading.repository.RifleRepository;
import de.tho.reloading.user.CurrentUser;
import de.tho.reloading.utility.LengthUnit;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * User data: a user only sees and changes the rifles they own. Every access goes through
 * {@link RifleRepository#findByIdAndOwner}, so the id of another user's rifle leads to "not found".
 * The database stores SI units. The form always sends millimeters (an inch field next to each length is
 * converted in the browser, see static/js/length-pair.js); the list shows millimeters and inches.
 */
@Controller
@RequestMapping("/rifles")
public class RifleController {

    private static final String PATH = "/rifles";

    private final RifleRepository repository;
    private final MasterDataLookup lookup;
    private final CurrentUser currentUser;

    public RifleController(RifleRepository repository, MasterDataLookup lookup, CurrentUser currentUser) {
        this.repository = repository;
        this.lookup = lookup;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Integer page,
                       @RequestParam(required = false) Integer size,
                       Model model) {
        AppUser user = currentUser.get();
        Map<UUID, String> manufacturers = lookup.manufacturerNames();
        Map<UUID, String> calibers = lookup.caliberNames();
        List<RifleRow> rows = repository.findByOwner(user.getId()).stream()
                .map(rifle -> new RifleRow(
                        rifle.getId(),
                        rifle.getName(),
                        manufacturers.get(rifle.getManufacturer()),
                        calibers.get(rifle.getCaliber()),
                        LengthUnit.formatMillimetersAndInches(rifle.getBarrelLength()),
                        LengthUnit.formatMillimetersAndInches(rifle.getLengthOfTwist())))
                .sorted(Comparator.comparing(RifleRow::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(RifleRow::caliber, String.CASE_INSENSITIVE_ORDER))
                .toList();
        model.addAttribute("page", PageView.of(rows, page, size, PATH, Map.of()));
        return "rifles/list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        return form(new RifleForm(), null, model);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable UUID id, Model model) {
        AppUser user = currentUser.get();
        Rifle rifle = find(id, user);
        RifleForm form = new RifleForm();
        form.setName(rifle.getName());
        form.setManufacturer(rifle.getManufacturer());
        form.setCaliber(rifle.getCaliber());
        form.setBarrelLength(LengthUnit.MILLIMETER.fromSi(rifle.getBarrelLength()));
        form.setLengthOfTwist(LengthUnit.MILLIMETER.fromSi(rifle.getLengthOfTwist()));
        return form(form, id, model);
    }

    @PostMapping
    public String saveNew(@Valid @ModelAttribute("form") RifleForm form, BindingResult result, Model model,
                          RedirectAttributes redirect) {
        return save(new Rifle(), null, form, result, model, redirect);
    }

    @PostMapping("/{id}")
    public String saveExisting(@PathVariable UUID id, @Valid @ModelAttribute("form") RifleForm form,
                               BindingResult result, Model model, RedirectAttributes redirect) {
        return save(find(id, currentUser.get()), id, form, result, model, redirect);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, RedirectAttributes redirect) {
        Rifle rifle = find(id, currentUser.get());
        try {
            repository.delete(rifle);
            redirect.addFlashAttribute("success", "\"" + rifle.getName() + "\" wurde gelöscht.");
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("error",
                    "\"" + rifle.getName() + "\" wird noch verwendet und kann nicht gelöscht werden.");
        }
        return "redirect:" + PATH;
    }

    private String save(Rifle rifle, UUID id, RifleForm form, BindingResult result, Model model,
                        RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            AppUser user = currentUser.get();
            // the owner comes from the login, never from the request
            rifle.setOwner(user.getId());
            rifle.setName(form.getName().strip());
            rifle.setManufacturer(form.getManufacturer());
            rifle.setCaliber(form.getCaliber());
            rifle.setBarrelLength(LengthUnit.MILLIMETER.toSi(form.getBarrelLength()));
            rifle.setLengthOfTwist(LengthUnit.MILLIMETER.toSi(form.getLengthOfTwist()));
            try {
                repository.save(rifle);
                redirect.addFlashAttribute("success", "\"" + rifle.getName() + "\" wurde gespeichert.");
                return "redirect:" + PATH;
            } catch (DuplicateKeyException e) {
                result.reject("duplicate", "Diese Waffe (Hersteller, Bezeichnung, Kaliber) hast du bereits angelegt.");
            }
        }
        return form(form, id, model);
    }

    /** @param id null for a new rifle */
    private String form(RifleForm form, UUID id, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("manufacturers", lookup.manufacturers());
        model.addAttribute("calibers", lookup.calibers());
        model.addAttribute("action", id == null ? PATH : PATH + "/" + id);
        model.addAttribute("deleteAction", id == null ? null : PATH + "/" + id + "/delete");
        return "rifles/form";
    }

    private Rifle find(UUID id, AppUser user) {
        return repository.findByIdAndOwner(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
