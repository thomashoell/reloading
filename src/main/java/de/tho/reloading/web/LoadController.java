package de.tho.reloading.web;

import de.tho.reloading.entity.AppUser;
import de.tho.reloading.entity.Bullet;
import de.tho.reloading.entity.BulletLot;
import de.tho.reloading.entity.CasingLot;
import de.tho.reloading.entity.Load;
import de.tho.reloading.entity.PowderLot;
import de.tho.reloading.entity.PrimerLot;
import de.tho.reloading.entity.ProcessedCase;
import de.tho.reloading.entity.Rifle;
import de.tho.reloading.repository.BulletLotRepository;
import de.tho.reloading.repository.BulletRepository;
import de.tho.reloading.repository.CasingLotRepository;
import de.tho.reloading.repository.CasingRepository;
import de.tho.reloading.repository.LoadRepository;
import de.tho.reloading.repository.PowderLotRepository;
import de.tho.reloading.repository.PowderRepository;
import de.tho.reloading.repository.PrimerLotRepository;
import de.tho.reloading.repository.PrimerRepository;
import de.tho.reloading.repository.ProcessedCaseRepository;
import de.tho.reloading.repository.RifleRepository;
import de.tho.reloading.user.CurrentUser;
import de.tho.reloading.utility.LengthUnit;
import de.tho.reloading.utility.WeightUnit;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * User data: a batch of cartridges loaded with the same components and the same data. A user only sees and
 * changes their own loads, and a load can only use a case batch, lots and a rifle of the same user; the ids from
 * the form are looked up with the owner, and the database enforces it again with its foreign keys.
 * The database stores SI units. The form sends lengths in millimeters (see static/js/length-pair.js) and the
 * powder charge in the weight unit of the user.
 * Loads keep growing, so the list is paged in the database.
 */
@Slf4j
@Controller
@RequestMapping("/loads")
public class LoadController {

    private static final String PATH = "/loads";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    // newest first; the id is a UUIDv7 and so keeps the order of creation within a day
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("loadedOn"), Sort.Order.desc("id"));

    private final LoadRepository repository;
    private final ProcessedCaseRepository processedCaseRepository;
    private final CasingLotRepository casingLotRepository;
    private final BulletLotRepository bulletLotRepository;
    private final PowderLotRepository powderLotRepository;
    private final PrimerLotRepository primerLotRepository;
    private final RifleRepository rifleRepository;
    private final CasingRepository casingRepository;
    private final BulletRepository bulletRepository;
    private final PowderRepository powderRepository;
    private final PrimerRepository primerRepository;
    private final MasterDataLookup lookup;
    private final CurrentUser currentUser;

    public LoadController(LoadRepository repository, ProcessedCaseRepository processedCaseRepository,
                          CasingLotRepository casingLotRepository, BulletLotRepository bulletLotRepository,
                          PowderLotRepository powderLotRepository, PrimerLotRepository primerLotRepository,
                          RifleRepository rifleRepository, CasingRepository casingRepository,
                          BulletRepository bulletRepository, PowderRepository powderRepository,
                          PrimerRepository primerRepository, MasterDataLookup lookup, CurrentUser currentUser) {
        this.repository = repository;
        this.processedCaseRepository = processedCaseRepository;
        this.casingLotRepository = casingLotRepository;
        this.bulletLotRepository = bulletLotRepository;
        this.powderLotRepository = powderLotRepository;
        this.primerLotRepository = primerLotRepository;
        this.rifleRepository = rifleRepository;
        this.casingRepository = casingRepository;
        this.bulletRepository = bulletRepository;
        this.powderRepository = powderRepository;
        this.primerRepository = primerRepository;
        this.lookup = lookup;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Integer page,
                       @RequestParam(required = false) Integer size,
                       Model model) {
        AppUser user = currentUser.get();
        UUID owner = user.getId();
        int pageSize = PageView.sizeOf(size);
        long total = repository.countByOwner(owner);
        int number = PageView.numberOf(page, pageSize, total);
        List<Load> loads = repository.findByOwner(owner, PageRequest.of(number - 1, pageSize, NEWEST_FIRST));

        WeightUnit weightUnit = user.getWeightUnit();
        Map<UUID, String> manufacturers = lookup.manufacturerNames();
        Map<UUID, String> casings = casingNames(manufacturers);
        Map<UUID, UUID> casingOfLot = casingLotRepository.findByOwner(owner).stream()
                .collect(Collectors.toMap(CasingLot::getId, CasingLot::getCasing));
        Map<UUID, UUID> casingLotOfBatch = processedCaseRepository.findByOwner(owner).stream()
                .collect(Collectors.toMap(ProcessedCase::getId, ProcessedCase::getCasingLot));
        Map<UUID, Bullet> bullets = bulletRepository.findAll().stream()
                .collect(Collectors.toMap(Bullet::getId, Function.identity()));
        Map<UUID, UUID> bulletOfLot = bulletLotRepository.findByOwner(owner).stream()
                .collect(Collectors.toMap(BulletLot::getId, BulletLot::getBullet));
        Map<UUID, String> powders = powderNames(manufacturers);
        Map<UUID, UUID> powderOfLot = powderLotRepository.findByOwner(owner).stream()
                .collect(Collectors.toMap(PowderLot::getId, PowderLot::getPowder));
        Map<UUID, String> primers = primerNames(manufacturers);
        Map<UUID, UUID> primerOfLot = primerLotRepository.findByOwner(owner).stream()
                .collect(Collectors.toMap(PrimerLot::getId, PrimerLot::getPrimer));
        Map<UUID, String> rifles = rifleOptions(user);

        List<LoadRow> rows = loads.stream()
                .map(load -> {
                    Bullet bullet = bullets.get(bulletOfLot.get(load.getBulletLot()));
                    return new LoadRow(
                            load.getId(),
                            DATE.format(load.getLoadedOn()),
                            manufacturers.get(bullet.getManufacturer()) + " " + bullet.getName() + ", "
                                    + weightUnit.format(bullet.getWeight()) + " " + weightUnit.getSymbol(),
                            powders.get(powderOfLot.get(load.getPowderLot())),
                            weightUnit.format(load.getPowderCharge()) + " " + weightUnit.getSymbol(),
                            primers.get(primerOfLot.get(load.getPrimerLot())),
                            casings.get(casingOfLot.get(casingLotOfBatch.get(load.getProcessedCase()))),
                            LengthUnit.formatMillimetersAndInches(load.getOverallLength(), 3),
                            load.getQuantity(),
                            load.getRifle() == null ? "" : rifles.get(load.getRifle()));
                })
                .toList();
        model.addAttribute("page", PageView.ofPage(rows, number, pageSize, total, PATH, Map.of()));
        return "loads/list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        LoadForm form = new LoadForm();
        form.setLoadedOn(LocalDate.now());
        return form(form, null, currentUser.get(), model);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable UUID id, Model model) {
        AppUser user = currentUser.get();
        Load load = find(id, user);
        LoadForm form = new LoadForm();
        form.setProcessedCase(load.getProcessedCase());
        form.setBulletLot(load.getBulletLot());
        form.setPowderLot(load.getPowderLot());
        form.setPrimerLot(load.getPrimerLot());
        form.setPowderCharge(user.getWeightUnit().fromSi(load.getPowderCharge()));
        form.setOverallLength(LengthUnit.MILLIMETER.fromSi(load.getOverallLength()));
        form.setBaseToOgive(LengthUnit.MILLIMETER.fromSi(load.getBaseToOgive()));
        form.setQuantity(load.getQuantity());
        form.setLoadedOn(load.getLoadedOn());
        form.setRifle(load.getRifle());
        form.setNotes(load.getNotes());
        return form(form, id, user, model);
    }

    @PostMapping
    public String saveNew(@Valid @ModelAttribute("form") LoadForm form, BindingResult result, Model model,
                          RedirectAttributes redirect) {
        return save(new Load(), null, currentUser.get(), form, result, model, redirect);
    }

    @PostMapping("/{id}")
    public String saveExisting(@PathVariable UUID id, @Valid @ModelAttribute("form") LoadForm form,
                               BindingResult result, Model model, RedirectAttributes redirect) {
        AppUser user = currentUser.get();
        return save(find(id, user), id, user, form, result, model, redirect);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, RedirectAttributes redirect) {
        Load load = find(id, currentUser.get());
        try {
            repository.delete(load);
            redirect.addFlashAttribute("success", "Die Ladung wurde gelöscht.");
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("error", "Die Ladung hat Schießergebnisse und kann nicht gelöscht werden.");
        }
        return "redirect:" + PATH;
    }

    private String save(Load load, UUID id, AppUser user, LoadForm form, BindingResult result, Model model,
                        RedirectAttributes redirect) {
        validate(form, user, result);
        if (!result.hasErrors()) {
            // the owner comes from the login, never from the request
            load.setOwner(user.getId());
            load.setProcessedCase(form.getProcessedCase());
            load.setBulletLot(form.getBulletLot());
            load.setPowderLot(form.getPowderLot());
            load.setPrimerLot(form.getPrimerLot());
            load.setPowderCharge(user.getWeightUnit().toSi(form.getPowderCharge()));
            load.setOverallLength(LengthUnit.MILLIMETER.toSi(form.getOverallLength()));
            load.setBaseToOgive(LengthUnit.MILLIMETER.toSi(form.getBaseToOgive()));
            load.setQuantity(form.getQuantity());
            load.setLoadedOn(form.getLoadedOn());
            load.setRifle(form.getRifle());
            load.setNotes(form.getNotes() == null || form.getNotes().isBlank() ? null : form.getNotes().strip());
            try {
                repository.save(load);
                redirect.addFlashAttribute("success", "Die Ladung wurde gespeichert.");
                return "redirect:" + PATH;
            } catch (DataIntegrityViolationException e) {
                // the checks above mirror the constraints of the table; this is the safety net
                log.warn("Load of user {} was rejected by the database: {}", user.getId(), e.getMessage());
                result.reject("invalid", "Die Ladung konnte nicht gespeichert werden. Bitte die Angaben prüfen.");
            }
        }
        return form(form, id, user, model);
    }

    /** Everything a load points to must belong to the user. */
    private void validate(LoadForm form, AppUser user, BindingResult result) {
        UUID owner = user.getId();
        if (form.getProcessedCase() != null
                && processedCaseRepository.findByIdAndOwner(form.getProcessedCase(), owner).isEmpty()) {
            result.rejectValue("processedCase", "unknown", "Bitte eine Hülsencharge aus der Liste wählen.");
        }
        if (form.getBulletLot() != null && bulletLotRepository.findByIdAndOwner(form.getBulletLot(), owner).isEmpty()) {
            result.rejectValue("bulletLot", "unknown", "Bitte ein Geschoss-Los aus der Liste wählen.");
        }
        if (form.getPowderLot() != null && powderLotRepository.findByIdAndOwner(form.getPowderLot(), owner).isEmpty()) {
            result.rejectValue("powderLot", "unknown", "Bitte ein Pulver-Los aus der Liste wählen.");
        }
        if (form.getPrimerLot() != null && primerLotRepository.findByIdAndOwner(form.getPrimerLot(), owner).isEmpty()) {
            result.rejectValue("primerLot", "unknown", "Bitte ein Zündhütchen-Los aus der Liste wählen.");
        }
        if (form.getRifle() != null && rifleRepository.findByIdAndOwner(form.getRifle(), owner).isEmpty()) {
            result.rejectValue("rifle", "unknown", "Bitte eine Waffe aus der Liste wählen.");
        }
    }

    /** @param id null for a new load */
    private String form(LoadForm form, UUID id, AppUser user, Model model) {
        UUID owner = user.getId();
        Map<UUID, String> manufacturers = lookup.manufacturerNames();
        Map<UUID, String> casings = casingNames(manufacturers);
        Map<UUID, String> bullets = bulletNames(manufacturers, user.getWeightUnit());
        Map<UUID, String> powders = powderNames(manufacturers);
        Map<UUID, String> primers = primerNames(manufacturers);

        Map<UUID, CasingLot> casingLots = casingLotRepository.findByOwner(owner).stream()
                .collect(Collectors.toMap(CasingLot::getId, Function.identity()));
        // newest batch first within a casing lot
        Stream<ProductOption> batches = processedCaseRepository.findByOwner(owner).stream()
                .sorted(Comparator.comparing(ProcessedCase::getProcessedOn).thenComparing(ProcessedCase::getId).reversed())
                .map(batch -> {
                    CasingLot lot = casingLots.get(batch.getCasingLot());
                    return new ProductOption(batch.getId(),
                            casings.get(lot.getCasing()) + ", Los " + lot.getLotNumber(),
                            DATE.format(batch.getProcessedOn()) + ", " + batch.getQuantity() + " Stück, "
                                    + batch.getTimesFired() + "× verschossen");
                });
        Map<String, List<ProductOption>> batchGroups = batches
                .sorted(Comparator.comparing(ProductOption::group, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.groupingBy(ProductOption::group, LinkedHashMap::new, Collectors.toList()));

        model.addAttribute("form", form);
        model.addAttribute("batchGroups", batchGroups);
        model.addAttribute("bulletGroups", groups(bulletLotRepository.findByOwner(owner).stream()
                .map(lot -> new ProductOption(lot.getId(), bullets.get(lot.getBullet()), lot.getLotNumber()))));
        model.addAttribute("powderGroups", groups(powderLotRepository.findByOwner(owner).stream()
                .map(lot -> new ProductOption(lot.getId(), powders.get(lot.getPowder()), lot.getLotNumber()))));
        model.addAttribute("primerGroups", groups(primerLotRepository.findByOwner(owner).stream()
                .map(lot -> new ProductOption(lot.getId(), primers.get(lot.getPrimer()), lot.getLotNumber()))));
        model.addAttribute("rifles", rifleOptions(user));
        model.addAttribute("weightSymbol", user.getWeightUnit().getSymbol());
        model.addAttribute("action", id == null ? PATH : PATH + "/" + id);
        model.addAttribute("deleteAction", id == null ? null : PATH + "/" + id + "/delete");
        return "loads/form";
    }

    /** Lots for a select box: product to its lots, both sorted by name. */
    private static Map<String, List<ProductOption>> groups(Stream<ProductOption> lots) {
        return lots
                .sorted(Comparator.comparing(ProductOption::group, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ProductOption::label, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.groupingBy(ProductOption::group, LinkedHashMap::new, Collectors.toList()));
    }

    /** The rifles of the user sorted by name: id to "name (caliber)". */
    private Map<UUID, String> rifleOptions(AppUser user) {
        Map<UUID, String> calibers = lookup.caliberNames();
        Map<UUID, String> options = new LinkedHashMap<>();
        rifleRepository.findByOwner(user.getId()).stream()
                .sorted(Comparator.comparing(Rifle::getName, String.CASE_INSENSITIVE_ORDER))
                .forEach(rifle -> options.put(rifle.getId(),
                        rifle.getName() + " (" + calibers.get(rifle.getCaliber()) + ")"));
        return options;
    }

    /** Casing id to "manufacturer caliber". */
    private Map<UUID, String> casingNames(Map<UUID, String> manufacturers) {
        Map<UUID, String> calibers = lookup.caliberNames();
        Map<UUID, String> names = new HashMap<>();
        casingRepository.findAll().forEach(casing -> names.put(casing.getId(),
                manufacturers.get(casing.getManufacturer()) + " " + calibers.get(casing.getCaliber())));
        return names;
    }

    /** Bullet id to "manufacturer name, .264 in (6.71 mm), 140 gr". */
    private Map<UUID, String> bulletNames(Map<UUID, String> manufacturers, WeightUnit weightUnit) {
        Map<UUID, String> names = new HashMap<>();
        bulletRepository.findAll().forEach(bullet -> names.put(bullet.getId(),
                manufacturers.get(bullet.getManufacturer()) + " " + bullet.getName() + ", "
                        + LengthUnit.formatDiameter(bullet.getDiameter()) + ", "
                        + weightUnit.format(bullet.getWeight()) + " " + weightUnit.getSymbol()));
        return names;
    }

    /** Powder id to "manufacturer name". */
    private Map<UUID, String> powderNames(Map<UUID, String> manufacturers) {
        Map<UUID, String> names = new HashMap<>();
        powderRepository.findAll().forEach(powder -> names.put(powder.getId(),
                manufacturers.get(powder.getManufacturer()) + " " + powder.getName()));
        return names;
    }

    /** Primer id to "manufacturer name (size)". */
    private Map<UUID, String> primerNames(Map<UUID, String> manufacturers) {
        Map<UUID, String> names = new HashMap<>();
        primerRepository.findAll().forEach(primer -> names.put(primer.getId(),
                manufacturers.get(primer.getManufacturer()) + " " + primer.getName()
                        + " (" + primer.getSize().getLabel() + ")"));
        return names;
    }

    private Load find(UUID id, AppUser user) {
        return repository.findByIdAndOwner(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
