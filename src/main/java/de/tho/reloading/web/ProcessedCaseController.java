package de.tho.reloading.web;

import de.tho.reloading.entity.AppUser;
import de.tho.reloading.entity.CasingLot;
import de.tho.reloading.entity.ProcessedCase;
import de.tho.reloading.entity.Rifle;
import de.tho.reloading.repository.CasingLotRepository;
import de.tho.reloading.repository.CasingRepository;
import de.tho.reloading.repository.ProcessedCaseRepository;
import de.tho.reloading.repository.RifleRepository;
import de.tho.reloading.user.CurrentUser;
import de.tho.reloading.utility.Calibration;
import de.tho.reloading.utility.LengthUnit;
import de.tho.reloading.utility.WeightUnit;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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

/**
 * User data: a batch of cases of one lot that were prepared the same way. A user only sees and changes their own
 * batches, and a batch can only point to a casing lot and a rifle of the same user; the ids from the form are
 * looked up with the owner, and the database enforces it again with its foreign keys.
 * The database stores SI units. The form always sends millimeters (see static/js/length-pair.js) and the case
 * capacity in grains of water.
 */
@Slf4j
@Controller
@RequestMapping("/processed-cases")
public class ProcessedCaseController {

    private static final String PATH = "/processed-cases";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final ProcessedCaseRepository repository;
    private final CasingLotRepository casingLotRepository;
    private final CasingRepository casingRepository;
    private final RifleRepository rifleRepository;
    private final MasterDataLookup lookup;
    private final CurrentUser currentUser;

    public ProcessedCaseController(ProcessedCaseRepository repository, CasingLotRepository casingLotRepository,
                                   CasingRepository casingRepository, RifleRepository rifleRepository,
                                   MasterDataLookup lookup, CurrentUser currentUser) {
        this.repository = repository;
        this.casingLotRepository = casingLotRepository;
        this.casingRepository = casingRepository;
        this.rifleRepository = rifleRepository;
        this.lookup = lookup;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Integer page,
                       @RequestParam(required = false) Integer size,
                       Model model) {
        AppUser user = currentUser.get();
        Map<UUID, String> casings = casingNames();
        Map<UUID, CasingLot> lots = casingLotRepository.findByOwner(user.getId()).stream()
                .collect(Collectors.toMap(CasingLot::getId, Function.identity()));
        Map<UUID, String> rifles = rifleOptions(user);
        List<ProcessedCaseRow> rows = repository.findByOwner(user.getId()).stream()
                // newest first; the id is a UUIDv7 and so keeps the order of creation within a day
                .sorted(Comparator.comparing(ProcessedCase::getProcessedOn)
                        .thenComparing(ProcessedCase::getId).reversed())
                .map(batch -> {
                    CasingLot lot = lots.get(batch.getCasingLot());
                    return new ProcessedCaseRow(
                            batch.getId(),
                            DATE.format(batch.getProcessedOn()),
                            casings.get(lot.getCasing()),
                            lot.getLotNumber(),
                            batch.getQuantity(),
                            batch.getTimesFired(),
                            batch.getFiredFrom() == null ? "" : rifles.get(batch.getFiredFrom()),
                            LengthUnit.formatMillimetersAndInches(batch.getCaseLength(), 3),
                            Boolean.TRUE.equals(batch.getTrimmed()),
                            batch.getCalibrationMethod() == null ? "" : batch.getCalibrationMethod().getLabel());
                })
                .toList();
        model.addAttribute("page", PageView.of(rows, page, size, PATH, Map.of()));
        return "processed-cases/list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        ProcessedCaseForm form = new ProcessedCaseForm();
        form.setProcessedOn(LocalDate.now());
        return form(form, null, currentUser.get(), model);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable UUID id, Model model) {
        AppUser user = currentUser.get();
        ProcessedCase batch = find(id, user);
        ProcessedCaseForm form = new ProcessedCaseForm();
        form.setCasingLot(batch.getCasingLot());
        form.setQuantity(batch.getQuantity());
        form.setProcessedOn(batch.getProcessedOn());
        form.setTimesFired(batch.getTimesFired());
        form.setFiredFrom(batch.getFiredFrom());
        form.setTrimmed(Boolean.TRUE.equals(batch.getTrimmed()));
        form.setCaseLength(LengthUnit.MILLIMETER.fromSi(batch.getCaseLength()));
        form.setWaterCapacity(WeightUnit.GRAIN.fromSi(batch.getWaterCapacity()));
        form.setCalibrationMethod(batch.getCalibrationMethod());
        form.setCalibrationDie(batch.getCalibrationDie());
        form.setBushing(batch.getBushing());
        form.setMandrelUsed(Boolean.TRUE.equals(batch.getMandrelUsed()));
        form.setMandrelDiameter(LengthUnit.MILLIMETER.fromSi(batch.getMandrelDiameter()));
        return form(form, id, user, model);
    }

    @PostMapping
    public String saveNew(@Valid @ModelAttribute("form") ProcessedCaseForm form, BindingResult result, Model model,
                          RedirectAttributes redirect) {
        return save(new ProcessedCase(), null, currentUser.get(), form, result, model, redirect);
    }

    @PostMapping("/{id}")
    public String saveExisting(@PathVariable UUID id, @Valid @ModelAttribute("form") ProcessedCaseForm form,
                               BindingResult result, Model model, RedirectAttributes redirect) {
        AppUser user = currentUser.get();
        return save(find(id, user), id, user, form, result, model, redirect);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, RedirectAttributes redirect) {
        ProcessedCase batch = find(id, currentUser.get());
        try {
            repository.delete(batch);
            redirect.addFlashAttribute("success", "Die Hülsencharge wurde gelöscht.");
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("error",
                    "Die Hülsencharge wird noch verwendet und kann nicht gelöscht werden.");
        }
        return "redirect:" + PATH;
    }

    private String save(ProcessedCase batch, UUID id, AppUser user, ProcessedCaseForm form, BindingResult result,
                        Model model, RedirectAttributes redirect) {
        validate(form, user, result);
        if (!result.hasErrors()) {
            // the owner comes from the login, never from the request
            batch.setOwner(user.getId());
            batch.setCasingLot(form.getCasingLot());
            batch.setQuantity(form.getQuantity());
            batch.setProcessedOn(form.getProcessedOn());
            batch.setTimesFired(form.getTimesFired());
            batch.setFiredFrom(form.getFiredFrom());
            batch.setTrimmed(form.isTrimmed());
            batch.setCaseLength(LengthUnit.MILLIMETER.toSi(form.getCaseLength()));
            batch.setWaterCapacity(WeightUnit.GRAIN.toSi(form.getWaterCapacity()));
            batch.setCalibrationMethod(form.getCalibrationMethod());
            batch.setCalibrationDie(blankToNull(form.getCalibrationDie()));
            batch.setBushing(blankToNull(form.getBushing()));
            batch.setMandrelUsed(form.isMandrelUsed());
            // no mandrel, no diameter
            batch.setMandrelDiameter(form.isMandrelUsed() ? LengthUnit.MILLIMETER.toSi(form.getMandrelDiameter()) : null);
            try {
                repository.save(batch);
                redirect.addFlashAttribute("success", "Die Hülsencharge wurde gespeichert.");
                return "redirect:" + PATH;
            } catch (DataIntegrityViolationException e) {
                // the checks above mirror the constraints of the table; this is the safety net
                log.warn("Case batch of user {} was rejected by the database: {}", user.getId(), e.getMessage());
                result.reject("invalid", "Die Hülsencharge konnte nicht gespeichert werden. Bitte die Angaben prüfen.");
            }
        }
        return form(form, id, user, model);
    }

    /** Rules that need more than one field, and the check that lot and rifle belong to the user. */
    private void validate(ProcessedCaseForm form, AppUser user, BindingResult result) {
        if (form.getCasingLot() != null
                && casingLotRepository.findByIdAndOwner(form.getCasingLot(), user.getId()).isEmpty()) {
            result.rejectValue("casingLot", "unknown", "Bitte ein Hülsen-Los aus der Liste wählen.");
        }
        if (form.getFiredFrom() != null
                && rifleRepository.findByIdAndOwner(form.getFiredFrom(), user.getId()).isEmpty()) {
            result.rejectValue("firedFrom", "unknown", "Bitte eine Waffe aus der Liste wählen.");
        }
        if (form.getTimesFired() != null && form.getTimesFired() > 0 && form.getFiredFrom() == null) {
            result.rejectValue("firedFrom", "required",
                    "Bitte die Waffe wählen, aus der die Hülsen zuletzt verschossen wurden.");
        }
        if (form.getCalibrationMethod() != null && blankToNull(form.getCalibrationDie()) == null) {
            result.rejectValue("calibrationDie", "required", "Bitte die Matrize angeben.");
        }
        if (form.isMandrelUsed() && form.getMandrelDiameter() == null) {
            result.rejectValue("mandrelDiameter", "required", "Bitte den Durchmesser des Dorns angeben.");
        }
    }

    /** @param id null for a new batch */
    private String form(ProcessedCaseForm form, UUID id, AppUser user, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("lotGroups", lotGroups(user));
        model.addAttribute("rifles", rifleOptions(user));
        model.addAttribute("calibrationMethods", Calibration.values());
        model.addAttribute("action", id == null ? PATH : PATH + "/" + id);
        model.addAttribute("deleteAction", id == null ? null : PATH + "/" + id + "/delete");
        return "processed-cases/form";
    }

    /** The casing lots of the user for the select box: casing ("Lapua 6.5 Creedmoor") to its lots. */
    private Map<String, List<ProductOption>> lotGroups(AppUser user) {
        Map<UUID, String> casings = casingNames();
        return casingLotRepository.findByOwner(user.getId()).stream()
                .map(lot -> new ProductOption(lot.getId(), casings.get(lot.getCasing()), lot.getLotNumber()))
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
    private Map<UUID, String> casingNames() {
        Map<UUID, String> manufacturers = lookup.manufacturerNames();
        Map<UUID, String> calibers = lookup.caliberNames();
        Map<UUID, String> names = new HashMap<>();
        casingRepository.findAll().forEach(casing -> names.put(casing.getId(),
                manufacturers.get(casing.getManufacturer()) + " " + calibers.get(casing.getCaliber())));
        return names;
    }

    private ProcessedCase find(UUID id, AppUser user) {
        return repository.findByIdAndOwner(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
