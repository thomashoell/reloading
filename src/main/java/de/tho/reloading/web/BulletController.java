package de.tho.reloading.web;

import de.tho.reloading.entity.AppUser;
import de.tho.reloading.entity.Bullet;
import de.tho.reloading.entity.Manufacturer;
import de.tho.reloading.repository.BulletRepository;
import de.tho.reloading.repository.ManufacturerRepository;
import de.tho.reloading.repository.MasterDataRepository;
import de.tho.reloading.user.CurrentUser;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

/**
 * Master data: everybody may read, only administrators may change.
 * The database stores SI units; forms and lists use the units of the current user.
 */
@Controller
@RequestMapping("/bullets")
public class BulletController {

    private static final String PATH = "/bullets";

    private final BulletRepository bulletRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final CurrentUser currentUser;

    public BulletController(BulletRepository bulletRepository, ManufacturerRepository manufacturerRepository,
                            CurrentUser currentUser) {
        this.bulletRepository = bulletRepository;
        this.manufacturerRepository = manufacturerRepository;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String list(Model model) {
        AppUser user = currentUser.get();
        Map<UUID, String> manufacturers = new HashMap<>();
        manufacturerRepository.findAll().forEach(m -> manufacturers.put(m.getId(), m.getName()));

        List<BulletRow> bullets = StreamSupport.stream(bulletRepository.findAll().spliterator(), false)
                .map(bullet -> new BulletRow(
                        bullet.getId(),
                        manufacturers.get(bullet.getManufacturer()),
                        bullet.getName(),
                        user.getLengthUnit().format(bullet.getDiameter()),
                        user.getWeightUnit().format(bullet.getWeight()),
                        user.getLengthUnit().format(bullet.getLength())))
                .sorted(Comparator.comparing(BulletRow::manufacturer, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(BulletRow::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(BulletRow::weight))
                .toList();
        model.addAttribute("bullets", bullets);
        return "masterdata/bullet-list";
    }

    @GetMapping("/new")
    @PreAuthorize(MasterDataRepository.ADMIN_ONLY)
    public String create(Model model) {
        return form(new BulletForm(), PATH, model);
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize(MasterDataRepository.ADMIN_ONLY)
    public String edit(@PathVariable UUID id, Model model) {
        AppUser user = currentUser.get();
        Bullet bullet = find(id);
        BulletForm form = new BulletForm();
        form.setManufacturer(bullet.getManufacturer());
        form.setName(bullet.getName());
        form.setDiameter(user.getLengthUnit().fromSi(bullet.getDiameter()));
        form.setWeight(user.getWeightUnit().fromSi(bullet.getWeight()));
        form.setLength(user.getLengthUnit().fromSi(bullet.getLength()));
        return form(form, PATH + "/" + id, model);
    }

    @PostMapping
    @PreAuthorize(MasterDataRepository.ADMIN_ONLY)
    public String saveNew(@Valid @ModelAttribute("form") BulletForm form, BindingResult result, Model model,
                          RedirectAttributes redirect) {
        return save(new Bullet(), PATH, form, result, model, redirect);
    }

    @PostMapping("/{id}")
    @PreAuthorize(MasterDataRepository.ADMIN_ONLY)
    public String saveExisting(@PathVariable UUID id, @Valid @ModelAttribute("form") BulletForm form,
                               BindingResult result, Model model, RedirectAttributes redirect) {
        return save(find(id), PATH + "/" + id, form, result, model, redirect);
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize(MasterDataRepository.ADMIN_ONLY)
    public String delete(@PathVariable UUID id, RedirectAttributes redirect) {
        Bullet bullet = find(id);
        try {
            bulletRepository.delete(bullet);
            redirect.addFlashAttribute("success", "\"" + bullet.getName() + "\" wurde gelöscht.");
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("error",
                    "\"" + bullet.getName() + "\" wird noch verwendet und kann nicht gelöscht werden.");
        }
        return "redirect:" + PATH;
    }

    private String save(Bullet bullet, String action, BulletForm form, BindingResult result, Model model,
                        RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            AppUser user = currentUser.get();
            bullet.setManufacturer(form.getManufacturer());
            bullet.setName(form.getName().strip());
            bullet.setDiameter(user.getLengthUnit().toSi(form.getDiameter()));
            bullet.setWeight(user.getWeightUnit().toSi(form.getWeight()));
            bullet.setLength(user.getLengthUnit().toSi(form.getLength()));
            try {
                bulletRepository.save(bullet);
                redirect.addFlashAttribute("success", "\"" + bullet.getName() + "\" wurde gespeichert.");
                return "redirect:" + PATH;
            } catch (DuplicateKeyException e) {
                result.reject("duplicate",
                        "Dieses Geschoss (Hersteller, Bezeichnung, Durchmesser, Gewicht) gibt es bereits.");
            }
        }
        return form(form, action, model);
    }

    private String form(BulletForm form, String action, Model model) {
        List<Manufacturer> manufacturers = StreamSupport.stream(manufacturerRepository.findAll().spliterator(), false)
                .sorted(Comparator.comparing(Manufacturer::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        model.addAttribute("form", form);
        model.addAttribute("manufacturers", manufacturers);
        model.addAttribute("action", action);
        return "masterdata/bullet-form";
    }

    private Bullet find(UUID id) {
        return bulletRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
