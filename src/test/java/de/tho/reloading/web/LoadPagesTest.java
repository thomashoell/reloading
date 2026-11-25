package de.tho.reloading.web;

import de.tho.reloading.entity.AppUser;
import de.tho.reloading.entity.BulletLot;
import de.tho.reloading.entity.Load;
import de.tho.reloading.entity.PowderLot;
import de.tho.reloading.entity.PrimerLot;
import de.tho.reloading.entity.Rifle;
import de.tho.reloading.entity.CasingLot;
import de.tho.reloading.entity.ProcessedCase;
import de.tho.reloading.repository.AppUserRepository;
import de.tho.reloading.repository.BulletLotRepository;
import de.tho.reloading.repository.BulletRepository;
import de.tho.reloading.repository.CaliberRepository;
import de.tho.reloading.repository.CasingLotRepository;
import de.tho.reloading.repository.CasingRepository;
import de.tho.reloading.repository.LoadRepository;
import de.tho.reloading.repository.ManufacturerRepository;
import de.tho.reloading.repository.PowderLotRepository;
import de.tho.reloading.repository.PowderRepository;
import de.tho.reloading.repository.PrimerLotRepository;
import de.tho.reloading.repository.PrimerRepository;
import de.tho.reloading.repository.ProcessedCaseRepository;
import de.tho.reloading.repository.RifleRepository;
import de.tho.reloading.utility.WeightUnit;
import de.tho.reloading.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Loads are user data: every user only sees and changes their own, and a load can only use a case batch, lots and
 * a rifle of the same user. The list is paged in the database.
 * The tests share one database and are not rolled back, so every test uses its own users.
 */
@SpringBootTest
@Testcontainers
class LoadPagesTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserService userService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ManufacturerRepository manufacturerRepository;

    @Autowired
    private CaliberRepository caliberRepository;

    @Autowired
    private CasingRepository casingRepository;

    @Autowired
    private BulletRepository bulletRepository;

    @Autowired
    private PowderRepository powderRepository;

    @Autowired
    private PrimerRepository primerRepository;

    @Autowired
    private CasingLotRepository casingLotRepository;

    @Autowired
    private BulletLotRepository bulletLotRepository;

    @Autowired
    private PowderLotRepository powderLotRepository;

    @Autowired
    private PrimerLotRepository primerLotRepository;

    @Autowired
    private ProcessedCaseRepository processedCaseRepository;

    @Autowired
    private RifleRepository rifleRepository;

    @Autowired
    private LoadRepository repository;

    private MockMvc mvc;
    private UUID lapua;
    private UUID creedmoor;
    private UUID casing;
    private UUID bullet;
    private UUID powder;
    private UUID primer;

    /** What a user needs before the first load. */
    private record Bench(UUID owner, UUID batch, UUID bulletLot, UUID powderLot, UUID primerLot, UUID rifle) {
    }

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        lapua = manufacturerRepository.findByName("Lapua").orElseThrow().getId();
        creedmoor = caliberRepository.findByName("6.5 Creedmoor").orElseThrow().getId();
        casing = casingRepository.findByManufacturer(lapua).stream()
                .filter(each -> each.getCaliber().equals(creedmoor))
                .findFirst().orElseThrow().getId();
        UUID hornady = manufacturerRepository.findByName("Hornady").orElseThrow().getId();
        // Hornady ELD Match .264, 140 gr
        bullet = bulletRepository.findByName("ELD Match").stream()
                .filter(each -> each.getManufacturer().equals(hornady))
                .filter(each -> each.getWeight().subtract(new BigDecimal("0.0090718474")).abs()
                        .compareTo(new BigDecimal("0.000001")) < 0)
                .findFirst().orElseThrow().getId();
        powder = powderRepository.findByName("N555").getFirst().getId();
        primer = primerRepository.findByName("BR-4").getFirst().getId();
    }

    private Bench bench(String subject) {
        UUID owner = userService.register(subject, subject).getId();

        CasingLot casingLot = new CasingLot();
        casingLot.setOwner(owner);
        casingLot.setCasing(casing);
        casingLot.setLotNumber("C-" + subject);
        casingLot = casingLotRepository.save(casingLot);

        ProcessedCase batch = new ProcessedCase();
        batch.setOwner(owner);
        batch.setCasingLot(casingLot.getId());
        batch.setQuantity(100);
        batch.setProcessedOn(LocalDate.of(2026, 10, 1));
        batch.setTimesFired(0);
        batch.setTrimmed(false);
        batch.setCaseLength(new BigDecimal("0.04851"));
        batch = processedCaseRepository.save(batch);

        BulletLot bulletLot = new BulletLot();
        bulletLot.setOwner(owner);
        bulletLot.setBullet(bullet);
        bulletLot.setLotNumber("B-" + subject);
        bulletLot = bulletLotRepository.save(bulletLot);

        PowderLot powderLot = new PowderLot();
        powderLot.setOwner(owner);
        powderLot.setPowder(powder);
        powderLot.setLotNumber("P-" + subject);
        powderLot = powderLotRepository.save(powderLot);

        PrimerLot primerLot = new PrimerLot();
        primerLot.setOwner(owner);
        primerLot.setPrimer(primer);
        primerLot.setLotNumber("Z-" + subject);
        primerLot = primerLotRepository.save(primerLot);

        Rifle rifle = new Rifle();
        rifle.setOwner(owner);
        rifle.setName("TRG " + subject);
        rifle.setManufacturer(lapua);
        rifle.setCaliber(creedmoor);
        rifle = rifleRepository.save(rifle);

        return new Bench(owner, batch.getId(), bulletLot.getId(), powderLot.getId(), primerLot.getId(), rifle.getId());
    }

    /**
     * A valid form: 20 cartridges with 42.5 gr.
     *
     * @param changes pairs of parameter name and value that replace or extend the defaults
     */
    private MockHttpServletRequestBuilder form(String url, String subject, Bench bench, String... changes) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("processedCase", bench.batch().toString());
        params.put("bulletLot", bench.bulletLot().toString());
        params.put("powderLot", bench.powderLot().toString());
        params.put("primerLot", bench.primerLot().toString());
        params.put("powderCharge", "42.5");
        params.put("overallLength", "71.12");
        params.put("quantity", "20");
        params.put("loadedOn", "2026-11-01");
        for (int i = 0; i < changes.length; i += 2) {
            params.put(changes[i], changes[i + 1]);
        }
        MockHttpServletRequestBuilder request = post(url).with(user(subject)).with(csrf());
        params.forEach(request::param);
        return request;
    }

    private Load only(UUID owner) {
        assertThat(repository.findByOwner(owner)).hasSize(1);
        return repository.findByOwner(owner).getFirst();
    }

    @Test
    void shouldShowEmptyListAndPointToWhatIsMissing() throws Exception {
        userService.register("empty-load-user", "empty-load-user");

        mvc.perform(get("/loads").with(user("empty-load-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Du hast noch keine Ladung angelegt.")))
                .andExpect(content().string(containsString("aria-current=\"page\">Ladungen<")));
        mvc.perform(get("/loads/new").with(user("empty-load-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ladung anlegen")))
                .andExpect(content().string(containsString("href=\"/processed-cases/new\"")))
                .andExpect(content().string(containsString("href=\"/bullet-lots/new\"")))
                .andExpect(content().string(containsString("href=\"/powder-lots/new\"")))
                .andExpect(content().string(containsString("href=\"/primer-lots/new\"")))
                .andExpect(content().string(not(containsString("Endgültig löschen"))));
        mvc.perform(get("/").with(user("empty-load-user")))
                .andExpect(content().string(containsString("href=\"/loads\"")));
    }

    @Test
    void shouldCreateLoadAndStoreSiUnits() throws Exception {
        Bench bench = bench("load-user");

        mvc.perform(get("/loads/new").with(user("load-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<optgroup label=\"Lapua 6.5 Creedmoor, Los C-load-user\">")))
                .andExpect(content().string(containsString(">01.10.2026, 100 Stück, 0× verschossen<")))
                .andExpect(content().string(containsString("<optgroup label=\"Hornady ELD Match, .264 in (6.71 mm), 140 gr\">")))
                .andExpect(content().string(containsString("<optgroup label=\"Vihtavuori N555\">")))
                .andExpect(content().string(containsString("<optgroup label=\"CCI BR-4 (Small Rifle)\">")))
                .andExpect(content().string(containsString(">gr</span>")))
                .andExpect(content().string(containsString(">COAL</abbr>")))
                .andExpect(content().string(containsString(">CBTO</abbr>")))
                .andExpect(content().string(not(containsString("href=\"/bullet-lots/new\""))));

        // the owner in the request is ignored
        mvc.perform(form("/loads", "load-user", bench, "owner", UUID.randomUUID().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/loads"));

        Load load = only(bench.owner());
        assertThat(load.getOwner()).isEqualTo(bench.owner());
        assertThat(load.getProcessedCase()).isEqualTo(bench.batch());
        assertThat(load.getBulletLot()).isEqualTo(bench.bulletLot());
        assertThat(load.getPowderLot()).isEqualTo(bench.powderLot());
        assertThat(load.getPrimerLot()).isEqualTo(bench.primerLot());
        // 42.5 gr in kilograms, 71.12 mm in meters
        assertThat(load.getPowderCharge()).isEqualByComparingTo("0.002753953675");
        assertThat(load.getOverallLength()).isEqualByComparingTo("0.07112");
        assertThat(load.getBaseToOgive()).isNull();
        assertThat(load.getQuantity()).isEqualTo(20);
        assertThat(load.getLoadedOn()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(load.getRifle()).isNull();
        assertThat(load.getNotes()).isNull();

        mvc.perform(get("/loads").with(user("load-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("01.11.2026")))
                .andExpect(content().string(containsString("<td>Hornady ELD Match, 140 gr</td>")))
                .andExpect(content().string(containsString("<td>Vihtavuori N555</td>")))
                .andExpect(content().string(containsString(">42.5 gr</td>")))
                .andExpect(content().string(containsString("<td>CCI BR-4 (Small Rifle)</td>")))
                .andExpect(content().string(containsString("<td>Lapua 6.5 Creedmoor</td>")))
                .andExpect(content().string(containsString("71.12 mm (2.8 in)")))
                .andExpect(content().string(containsString("Einträge 1–1 von 1")));
    }

    @Test
    void shouldEditAndDeleteLoad() throws Exception {
        Bench bench = bench("edit-load-user");
        mvc.perform(form("/loads", "edit-load-user", bench))
                .andExpect(status().is3xxRedirection());
        UUID id = only(bench.owner()).getId();

        mvc.perform(form("/loads/" + id, "edit-load-user", bench,
                        "powderCharge", "43.1", "overallLength", "71.5", "baseToOgive", "55.118",
                        "quantity", "10", "loadedOn", "2026-11-20",
                        "rifle", bench.rifle().toString(), "notes", "  Leiter, Stufe 3  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/loads"));

        Load load = only(bench.owner());
        assertThat(load.getId()).isEqualTo(id);
        assertThat(load.getOverallLength()).isEqualByComparingTo("0.0715");
        assertThat(load.getBaseToOgive()).isEqualByComparingTo("0.055118");
        assertThat(load.getQuantity()).isEqualTo(10);
        assertThat(load.getRifle()).isEqualTo(bench.rifle());
        assertThat(load.getNotes()).isEqualTo("Leiter, Stufe 3");

        mvc.perform(get("/loads").with(user("edit-load-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("20.11.2026")))
                .andExpect(content().string(containsString(">43.1 gr</td>")))
                .andExpect(content().string(containsString("<td>TRG edit-load-user (6.5 Creedmoor)</td>")));

        mvc.perform(get("/loads/" + id + "/edit").with(user("edit-load-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ladung bearbeiten")))
                .andExpect(content().string(containsString("value=\"43.1\"")))
                .andExpect(content().string(containsString("value=\"71.5\"")))
                .andExpect(content().string(containsString("value=\"2026-11-20\"")))
                .andExpect(content().string(containsString("selected=\"selected\">B-edit-load-user<")))
                .andExpect(content().string(containsString("selected=\"selected\">TRG edit-load-user (6.5 Creedmoor)<")))
                .andExpect(content().string(containsString(">Leiter, Stufe 3</textarea>")))
                .andExpect(content().string(containsString("Endgültig löschen")));

        mvc.perform(post("/loads/" + id + "/delete").with(user("edit-load-user")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/loads"));
        assertThat(repository.findByOwner(bench.owner())).isEmpty();
    }

    @Test
    void shouldTakePowderChargeInTheUnitOfTheUser() throws Exception {
        Bench bench = bench("gram-load-user");
        AppUser user = appUserRepository.findById(bench.owner()).orElseThrow();
        user.setWeightUnit(WeightUnit.GRAM);
        appUserRepository.save(user);

        mvc.perform(get("/loads/new").with(user("gram-load-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">g</span>")));
        mvc.perform(form("/loads", "gram-load-user", bench, "powderCharge", "2.75"))
                .andExpect(status().is3xxRedirection());

        assertThat(only(bench.owner()).getPowderCharge()).isEqualByComparingTo("0.00275");
        mvc.perform(get("/loads").with(user("gram-load-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">2.75 g</td>")));
        mvc.perform(get("/loads/" + only(bench.owner()).getId() + "/edit").with(user("gram-load-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"2.75\"")));
    }

    @Test
    void shouldHideLoadsFromOtherUsers() throws Exception {
        Bench bench = bench("load-owner-a");
        mvc.perform(form("/loads", "load-owner-a", bench, "notes", "SECRET-NOTE"))
                .andExpect(status().is3xxRedirection());
        UUID id = only(bench.owner()).getId();
        Bench other = bench("load-owner-b");

        mvc.perform(get("/loads").with(user("load-owner-b")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Du hast noch keine Ladung angelegt.")));
        mvc.perform(get("/loads/new").with(user("load-owner-b")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("B-load-owner-b")))
                .andExpect(content().string(not(containsString("B-load-owner-a"))));
        mvc.perform(get("/loads/" + id + "/edit").with(user("load-owner-b")))
                .andExpect(status().isNotFound());
        mvc.perform(form("/loads/" + id, "load-owner-b", other))
                .andExpect(status().isNotFound());
        mvc.perform(post("/loads/" + id + "/delete").with(user("load-owner-b")).with(csrf()))
                .andExpect(status().isNotFound());

        assertThat(only(bench.owner()).getNotes()).isEqualTo("SECRET-NOTE");
        assertThat(repository.findByOwner(other.owner())).isEmpty();
    }

    @Test
    void shouldRejectComponentsOfAnotherUser() throws Exception {
        Bench bench = bench("honest-load-user");
        Bench stranger = bench("load-stranger");

        mvc.perform(form("/loads", "honest-load-user", bench,
                        "processedCase", stranger.batch().toString(),
                        "bulletLot", stranger.bulletLot().toString(),
                        "powderLot", stranger.powderLot().toString(),
                        "primerLot", stranger.primerLot().toString(),
                        "rifle", stranger.rifle().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bitte eine Hülsencharge aus der Liste wählen.")))
                .andExpect(content().string(containsString("Bitte ein Geschoss-Los aus der Liste wählen.")))
                .andExpect(content().string(containsString("Bitte ein Pulver-Los aus der Liste wählen.")))
                .andExpect(content().string(containsString("Bitte ein Zündhütchen-Los aus der Liste wählen.")))
                .andExpect(content().string(containsString("Bitte eine Waffe aus der Liste wählen.")))
                .andExpect(content().string(not(containsString("load-stranger"))));

        assertThat(repository.findByOwner(bench.owner())).isEmpty();
    }

    @Test
    void shouldRejectInvalidLoads() throws Exception {
        Bench bench = bench("strict-load-user");

        mvc.perform(post("/loads").with(user("strict-load-user")).with(csrf())
                        .param("processedCase", "").param("bulletLot", "").param("powderLot", "")
                        .param("primerLot", "").param("powderCharge", "").param("overallLength", "")
                        .param("quantity", "").param("loadedOn", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bitte eine Hülsencharge wählen.")))
                .andExpect(content().string(containsString("Bitte ein Geschoss-Los wählen.")))
                .andExpect(content().string(containsString("Bitte ein Pulver-Los wählen.")))
                .andExpect(content().string(containsString("Bitte ein Zündhütchen-Los wählen.")))
                .andExpect(content().string(containsString("Bitte die Pulverladung angeben.")))
                .andExpect(content().string(containsString("Bitte die Patronenlänge angeben.")))
                .andExpect(content().string(containsString("Bitte die Anzahl angeben.")))
                .andExpect(content().string(containsString("Bitte das Datum angeben.")));

        mvc.perform(form("/loads", "strict-load-user", bench,
                        "powderCharge", "0", "overallLength", "-1", "baseToOgive", "0", "quantity", "0"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Die Pulverladung muss größer als 0 sein.")))
                .andExpect(content().string(containsString("Die Patronenlänge muss größer als 0 sein.")))
                .andExpect(content().string(containsString("Das Maß muss größer als 0 sein.")))
                .andExpect(content().string(containsString("Die Anzahl muss größer als 0 sein.")))
                // what was chosen is kept
                .andExpect(content().string(containsString("selected=\"selected\">P-strict-load-user<")));

        mvc.perform(post("/loads").with(user("strict-load-user"))
                        .param("processedCase", bench.batch().toString()).param("quantity", "20"))
                .andExpect(status().isForbidden());

        assertThat(repository.findByOwner(bench.owner())).isEmpty();
    }

    @Test
    void shouldPageInTheDatabaseNewestFirst() throws Exception {
        Bench bench = bench("paging-load-user");
        // 22 loads on 22 days, each with its own charge: 40.01 gr on Jan 1st up to 40.22 gr on Jan 22nd
        for (int day = 1; day <= 22; day++) {
            Load load = new Load();
            load.setOwner(bench.owner());
            load.setProcessedCase(bench.batch());
            load.setBulletLot(bench.bulletLot());
            load.setPowderLot(bench.powderLot());
            load.setPrimerLot(bench.primerLot());
            load.setPowderCharge(WeightUnit.GRAIN.toSi(new BigDecimal("40").add(new BigDecimal(day).movePointLeft(2))));
            load.setOverallLength(new BigDecimal("0.07112"));
            load.setQuantity(5);
            load.setLoadedOn(LocalDate.of(2026, 1, day));
            repository.save(load);
        }

        String first = mvc.perform(get("/loads").with(user("paging-load-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Einträge 1–20 von 22")))
                .andExpect(content().string(containsString("href=\"/loads?page=2\"")))
                .andReturn().getResponse().getContentAsString();
        assertThat(first).contains("22.01.2026", "03.01.2026").doesNotContain("02.01.2026", "01.01.2026");
        assertThat(first.indexOf("22.01.2026")).isLessThan(first.indexOf("21.01.2026"));

        String second = mvc.perform(get("/loads").param("page", "2").with(user("paging-load-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Einträge 21–22 von 22")))
                .andReturn().getResponse().getContentAsString();
        assertThat(second).contains("02.01.2026", "01.01.2026", ">40.01 gr</td>").doesNotContain("03.01.2026");
        assertThat(second.indexOf("02.01.2026")).isLessThan(second.indexOf("01.01.2026"));

        // a page that does not exist shows the last one; the size is kept in the links
        mvc.perform(get("/loads").param("page", "99").param("size", "5").with(user("paging-load-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Einträge 21–22 von 22")))
                .andExpect(content().string(containsString("href=\"/loads?page=4&amp;size=5\"")));
    }

    @Test
    void shouldNotDeleteBatchThatIsLoaded() throws Exception {
        Bench bench = bench("loaded-batch-user");
        mvc.perform(form("/loads", "loaded-batch-user", bench))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post("/processed-cases/" + bench.batch() + "/delete").with(user("loaded-batch-user")).with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(processedCaseRepository.findByIdAndOwner(bench.batch(), bench.owner())).isPresent();
    }
}
