package de.tho.reloading.web;

import de.tho.reloading.entity.AppUser;
import de.tho.reloading.user.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Pages of the lots of one component (casings, bullets, powders, primers): list, create, edit, delete.
 * Lots are user data: a user only sees and changes their own, and every access goes through
 * {@link #findByIdAndOwner}, so the id of another user's lot leads to "not found".
 * The four controllers only differ in the entity and in how a product is named.
 *
 * @param <L> the lot entity
 */
public abstract class AbstractLotController<L> {

    protected final MasterDataLookup lookup;
    private final CurrentUser currentUser;

    protected AbstractLotController(MasterDataLookup lookup, CurrentUser currentUser) {
        this.lookup = lookup;
        this.currentUser = currentUser;
    }

    /** Path of the list, e.g. /casing-lots. */
    protected abstract String path();

    /** Heading of the list, e.g. "Hülsen-Lose". */
    protected abstract String title();

    /** One of them, e.g. "Hülsen-Los". */
    protected abstract String singular();

    /** What the product is called, e.g. "Hülse". */
    protected abstract String productLabel();

    /** All products a lot can belong to, in the order they are offered. */
    protected abstract List<ProductOption> products(AppUser user);

    protected abstract List<L> findByOwner(UUID owner);

    protected abstract Optional<L> findByIdAndOwner(UUID id, UUID owner);

    protected abstract L newLot();

    protected abstract void save(L lot);

    protected abstract void delete(L lot);

    protected abstract UUID idOf(L lot);

    protected abstract UUID productOf(L lot);

    protected abstract String lotNumberOf(L lot);

    protected abstract void apply(L lot, UUID owner, UUID product, String lotNumber);

    @ModelAttribute
    public void page(Model model) {
        model.addAttribute("title", title());
        model.addAttribute("singular", singular());
        model.addAttribute("productLabel", productLabel());
        model.addAttribute("path", path());
    }

    @GetMapping
    public String list(@RequestParam(required = false) Integer page,
                       @RequestParam(required = false) Integer size,
                       Model model) {
        AppUser user = currentUser.get();
        Map<UUID, String> names = products(user).stream()
                .collect(Collectors.toMap(ProductOption::id, ProductOption::fullName));
        List<LotRow> rows = findByOwner(user.getId()).stream()
                .map(lot -> new LotRow(idOf(lot), names.get(productOf(lot)), lotNumberOf(lot)))
                .sorted(Comparator.comparing(LotRow::product, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(LotRow::lotNumber, String.CASE_INSENSITIVE_ORDER))
                .toList();
        model.addAttribute("page", PageView.of(rows, page, size, path(), Map.of()));
        return "lots/list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        return form(new LotForm(), null, currentUser.get(), model);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable UUID id, Model model) {
        AppUser user = currentUser.get();
        L lot = find(id, user);
        LotForm form = new LotForm();
        form.setProduct(productOf(lot));
        form.setLotNumber(lotNumberOf(lot));
        return form(form, id, user, model);
    }

    @PostMapping
    public String saveNew(@Valid @ModelAttribute("form") LotForm form, BindingResult result, Model model,
                          RedirectAttributes redirect) {
        return save(newLot(), null, form, result, model, redirect);
    }

    @PostMapping("/{id}")
    public String saveExisting(@PathVariable UUID id, @Valid @ModelAttribute("form") LotForm form,
                               BindingResult result, Model model, RedirectAttributes redirect) {
        return save(find(id, currentUser.get()), id, form, result, model, redirect);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, RedirectAttributes redirect) {
        L lot = find(id, currentUser.get());
        String name = singular() + " " + lotNumberOf(lot);
        try {
            delete(lot);
            redirect.addFlashAttribute("success", name + " wurde gelöscht.");
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("error", name + " wird noch verwendet und kann nicht gelöscht werden.");
        }
        return "redirect:" + path();
    }

    private String save(L lot, UUID id, LotForm form, BindingResult result, Model model, RedirectAttributes redirect) {
        AppUser user = currentUser.get();
        if (form.getProduct() != null
                && products(user).stream().noneMatch(option -> option.id().equals(form.getProduct()))) {
            result.rejectValue("product", "unknown", "Bitte ein Produkt aus der Liste wählen.");
        }
        if (!result.hasErrors()) {
            // the owner comes from the login, never from the request
            apply(lot, user.getId(), form.getProduct(), form.getLotNumber().strip());
            try {
                save(lot);
                redirect.addFlashAttribute("success", singular() + " " + lotNumberOf(lot) + " wurde gespeichert.");
                return "redirect:" + path();
            } catch (DuplicateKeyException e) {
                result.reject("duplicate", "Dieses Los hast du für dieses Produkt bereits angelegt.");
            }
        }
        return form(form, id, user, model);
    }

    /** @param id null for a new lot */
    private String form(LotForm form, UUID id, AppUser user, Model model) {
        // grouped by manufacturer, in the order of products()
        Map<String, List<ProductOption>> groups = products(user).stream()
                .collect(Collectors.groupingBy(ProductOption::group, LinkedHashMap::new, Collectors.toList()));
        model.addAttribute("form", form);
        model.addAttribute("productGroups", groups);
        model.addAttribute("action", id == null ? path() : path() + "/" + id);
        model.addAttribute("deleteAction", id == null ? null : path() + "/" + id + "/delete");
        return "lots/form";
    }

    private L find(UUID id, AppUser user) {
        return findByIdAndOwner(id, user.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** Sorts products by manufacturer first; ties keep the given order. */
    protected static <P> List<ProductOption> options(List<P> products, Function<P, ProductOption> toOption,
                                                     Comparator<P> withinManufacturer) {
        return products.stream()
                .sorted(withinManufacturer)
                .map(toOption)
                .sorted(Comparator.comparing(ProductOption::group, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
