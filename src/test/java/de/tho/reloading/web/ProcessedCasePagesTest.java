package de.tho.reloading.web;

import de.tho.reloading.entity.Rifle;
import de.tho.reloading.entity.CasingLot;
import de.tho.reloading.entity.ProcessedCase;
import de.tho.reloading.repository.CaliberRepository;
import de.tho.reloading.repository.CasingLotRepository;
import de.tho.reloading.repository.CasingRepository;
import de.tho.reloading.repository.ManufacturerRepository;
import de.tho.reloading.repository.ProcessedCaseRepository;
import de.tho.reloading.repository.RifleRepository;
import de.tho.reloading.utility.Calibration;
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
 * Case batches are user data: every user only sees and changes their own, and a batch can only use a casing lot
 * and a rifle of the same user.
 * The tests share one database and are not rolled back, so every test uses its own users.
 */
@SpringBootTest
@Testcontainers
class ProcessedCasePagesTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserService userService;

    @Autowired
    private ManufacturerRepository manufacturerRepository;

    @Autowired
    private CaliberRepository caliberRepository;

    @Autowired
    private CasingRepository casingRepository;

    @Autowired
    private CasingLotRepository casingLotRepository;

    @Autowired
    private RifleRepository rifleRepository;

    @Autowired
    private ProcessedCaseRepository repository;

    private MockMvc mvc;
    private UUID lapua;
    private UUID creedmoor;
    private UUID lapuaCreedmoor;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        lapua = manufacturerRepository.findByName("Lapua").orElseThrow().getId();
        creedmoor = caliberRepository.findByName("6.5 Creedmoor").orElseThrow().getId();
        lapuaCreedmoor = casingRepository.findByManufacturer(lapua).stream()
                .filter(casing -> casing.getCaliber().equals(creedmoor))
                .findFirst().orElseThrow().getId();
    }

    private UUID register(String subject) {
        return userService.register(subject, subject).getId();
    }

    private UUID casingLot(UUID owner, String lotNumber) {
        CasingLot lot = new CasingLot();
        lot.setOwner(owner);
        lot.setCasing(lapuaCreedmoor);
        lot.setLotNumber(lotNumber);
        return casingLotRepository.save(lot).getId();
    }

    private UUID rifle(UUID owner, String name) {
        Rifle rifle = new Rifle();
        rifle.setOwner(owner);
        rifle.setName(name);
        rifle.setManufacturer(lapua);
        rifle.setCaliber(creedmoor);
        return rifleRepository.save(rifle).getId();
    }

    /**
     * A valid form for new, unfired cases.
     *
     * @param changes pairs of parameter name and value that replace or extend the defaults
     */
    private MockHttpServletRequestBuilder form(String url, String subject, UUID lot, String... changes) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("casingLot", lot.toString());
        params.put("quantity", "50");
        params.put("processedOn", "2026-11-01");
        params.put("timesFired", "0");
        params.put("caseLength", "48.51");
        for (int i = 0; i < changes.length; i += 2) {
            params.put(changes[i], changes[i + 1]);
        }
        MockHttpServletRequestBuilder request = post(url).with(user(subject)).with(csrf());
        params.forEach(request::param);
        return request;
    }

    private ProcessedCase only(UUID owner) {
        assertThat(repository.findByOwner(owner)).hasSize(1);
        return repository.findByOwner(owner).getFirst();
    }

    @Test
    void shouldShowEmptyListAndPointToLotsWhenThereAreNone() throws Exception {
        register("empty-batch-user");

        mvc.perform(get("/processed-cases").with(user("empty-batch-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Du hast noch keine Hülsencharge angelegt.")))
                .andExpect(content().string(containsString("aria-current=\"page\">Hülsenchargen<")));
        mvc.perform(get("/processed-cases/new").with(user("empty-batch-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hülsencharge anlegen")))
                .andExpect(content().string(containsString("href=\"/casing-lots/new\"")))
                .andExpect(content().string(containsString(">Vollkalibriert<")))
                .andExpect(content().string(not(containsString("Endgültig löschen"))));
        mvc.perform(get("/").with(user("empty-batch-user")))
                .andExpect(content().string(containsString("href=\"/processed-cases\"")));
    }

    @Test
    void shouldCreateNewCasesAndStoreMeters() throws Exception {
        UUID owner = register("batch-user");
        UUID lot = casingLot(owner, "B-LOT-1");

        mvc.perform(get("/processed-cases/new").with(user("batch-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<optgroup label=\"Lapua 6.5 Creedmoor\">")))
                .andExpect(content().string(containsString(">B-LOT-1<")))
                .andExpect(content().string(not(containsString("href=\"/casing-lots/new\""))));

        // the owner in the request is ignored
        mvc.perform(form("/processed-cases", "batch-user", lot, "owner", UUID.randomUUID().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/processed-cases"));

        ProcessedCase batch = only(owner);
        assertThat(batch.getOwner()).isEqualTo(owner);
        assertThat(batch.getCasingLot()).isEqualTo(lot);
        assertThat(batch.getQuantity()).isEqualTo(50);
        assertThat(batch.getProcessedOn()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(batch.getTimesFired()).isZero();
        assertThat(batch.getFiredFrom()).isNull();
        assertThat(batch.getTrimmed()).isFalse();
        assertThat(batch.getCaseLength()).isEqualByComparingTo("0.04851");
        assertThat(batch.getWaterCapacity()).isNull();
        assertThat(batch.getCalibrationMethod()).isNull();
        assertThat(batch.getMandrelUsed()).isFalse();
        assertThat(batch.getMandrelDiameter()).isNull();

        mvc.perform(get("/processed-cases").with(user("batch-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("01.11.2026")))
                .andExpect(content().string(containsString("<td>Lapua 6.5 Creedmoor</td>")))
                .andExpect(content().string(containsString("<td>B-LOT-1</td>")))
                .andExpect(content().string(containsString("48.51 mm (1.91 in)")))
                .andExpect(content().string(not(containsString("getrimmt"))))
                .andExpect(content().string(containsString("Einträge 1–1 von 1")));
    }

    @Test
    void shouldEditAndDeleteFiredAndSizedCases() throws Exception {
        UUID owner = register("sized-batch-user");
        UUID lot = casingLot(owner, "S-LOT-1");
        UUID rifle = rifle(owner, "TRG");

        mvc.perform(form("/processed-cases", "sized-batch-user", lot))
                .andExpect(status().is3xxRedirection());
        UUID id = only(owner).getId();

        mvc.perform(post("/processed-cases/" + id).with(user("sized-batch-user")).with(csrf())
                        .param("casingLot", lot.toString())
                        .param("quantity", "48")
                        .param("processedOn", "2026-11-15")
                        .param("timesFired", "2")
                        .param("firedFrom", rifle.toString())
                        .param("trimmed", "true")
                        .param("caseLength", "48.6")
                        .param("waterCapacity", "52.5")
                        .param("calibrationMethod", "SHOULDER_AND_NECK")
                        .param("calibrationDie", " Redding Type S ")
                        .param("bushing", "0.289")
                        .param("mandrelUsed", "true")
                        .param("mandrelDiameter", "6.668"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/processed-cases"));

        ProcessedCase batch = only(owner);
        assertThat(batch.getId()).isEqualTo(id);
        assertThat(batch.getQuantity()).isEqualTo(48);
        assertThat(batch.getTimesFired()).isEqualTo(2);
        assertThat(batch.getFiredFrom()).isEqualTo(rifle);
        assertThat(batch.getTrimmed()).isTrue();
        assertThat(batch.getCaseLength()).isEqualByComparingTo("0.0486");
        // 52.5 grains of water in kilograms
        assertThat(batch.getWaterCapacity()).isEqualByComparingTo("0.003401942775");
        assertThat(batch.getCalibrationMethod()).isEqualTo(Calibration.SHOULDER_AND_NECK);
        assertThat(batch.getCalibrationDie()).isEqualTo("Redding Type S");
        assertThat(batch.getBushing()).isEqualTo("0.289");
        assertThat(batch.getMandrelUsed()).isTrue();
        assertThat(batch.getMandrelDiameter()).isEqualByComparingTo("0.006668");

        mvc.perform(get("/processed-cases").with(user("sized-batch-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("15.11.2026")))
                .andExpect(content().string(containsString("<td>TRG (6.5 Creedmoor)</td>")))
                .andExpect(content().string(containsString("getrimmt")))
                .andExpect(content().string(containsString("<td>Schulter und Hals</td>")));

        mvc.perform(get("/processed-cases/" + id + "/edit").with(user("sized-batch-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hülsencharge bearbeiten")))
                .andExpect(content().string(containsString("value=\"2026-11-15\"")))
                .andExpect(content().string(containsString("value=\"Redding Type S\"")))
                .andExpect(content().string(containsString("value=\"52.5\"")))
                .andExpect(content().string(containsString("selected=\"selected\">S-LOT-1<")))
                .andExpect(content().string(containsString("selected=\"selected\">TRG (6.5 Creedmoor)<")))
                .andExpect(content().string(containsString("selected=\"selected\">Schulter und Hals<")))
                .andExpect(content().string(containsString("Endgültig löschen")));

        // without the mandrel its diameter is dropped
        mvc.perform(form("/processed-cases/" + id, "sized-batch-user", lot, "mandrelDiameter", "6.668"))
                .andExpect(status().is3xxRedirection());
        assertThat(only(owner).getMandrelUsed()).isFalse();
        assertThat(only(owner).getMandrelDiameter()).isNull();

        mvc.perform(post("/processed-cases/" + id + "/delete").with(user("sized-batch-user")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/processed-cases"));
        assertThat(repository.findByOwner(owner)).isEmpty();
    }

    @Test
    void shouldHideBatchesFromOtherUsers() throws Exception {
        UUID owner = register("batch-owner-a");
        UUID lot = casingLot(owner, "SECRET-LOT");
        mvc.perform(form("/processed-cases", "batch-owner-a", lot))
                .andExpect(status().is3xxRedirection());
        UUID id = only(owner).getId();
        UUID other = register("batch-owner-b");
        UUID otherLot = casingLot(other, "OTHER-LOT");

        mvc.perform(get("/processed-cases").with(user("batch-owner-b")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("SECRET-LOT"))));
        mvc.perform(get("/processed-cases/new").with(user("batch-owner-b")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">OTHER-LOT<")))
                .andExpect(content().string(not(containsString("SECRET-LOT"))));
        mvc.perform(get("/processed-cases/" + id + "/edit").with(user("batch-owner-b")))
                .andExpect(status().isNotFound());
        mvc.perform(form("/processed-cases/" + id, "batch-owner-b", otherLot))
                .andExpect(status().isNotFound());
        mvc.perform(post("/processed-cases/" + id + "/delete").with(user("batch-owner-b")).with(csrf()))
                .andExpect(status().isNotFound());

        assertThat(only(owner).getCasingLot()).isEqualTo(lot);
        assertThat(repository.findByOwner(other)).isEmpty();
    }

    @Test
    void shouldRejectLotAndRifleOfAnotherUser() throws Exception {
        UUID owner = register("honest-batch-user");
        UUID lot = casingLot(owner, "MINE-1");
        UUID stranger = register("batch-stranger");
        UUID foreignLot = casingLot(stranger, "FOREIGN-1");
        UUID foreignRifle = rifle(stranger, "Foreign rifle");

        mvc.perform(form("/processed-cases", "honest-batch-user", foreignLot))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bitte ein Hülsen-Los aus der Liste wählen.")));
        mvc.perform(form("/processed-cases", "honest-batch-user", lot,
                        "timesFired", "1", "firedFrom", foreignRifle.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bitte eine Waffe aus der Liste wählen.")))
                .andExpect(content().string(not(containsString("Foreign rifle"))));

        assertThat(repository.findByOwner(owner)).isEmpty();
    }

    @Test
    void shouldRejectInvalidBatches() throws Exception {
        UUID owner = register("strict-batch-user");
        UUID lot = casingLot(owner, "STRICT-1");

        mvc.perform(post("/processed-cases").with(user("strict-batch-user")).with(csrf())
                        .param("casingLot", "").param("quantity", "0").param("processedOn", "")
                        .param("timesFired", "-1").param("caseLength", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bitte ein Hülsen-Los wählen.")))
                .andExpect(content().string(containsString("Die Anzahl muss größer als 0 sein.")))
                .andExpect(content().string(containsString("Bitte das Datum angeben.")))
                .andExpect(content().string(containsString("Die Schusszahl darf nicht negativ sein.")))
                .andExpect(content().string(containsString("Bitte die Hülsenlänge angeben.")));

        // fired cases need the rifle, sized cases the die, a mandrel its diameter
        mvc.perform(form("/processed-cases", "strict-batch-user", lot,
                        "timesFired", "3",
                        "calibrationMethod", "FULL", "calibrationDie", " ",
                        "mandrelUsed", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bitte die Waffe wählen, aus der die Hülsen zuletzt verschossen wurden.")))
                .andExpect(content().string(containsString("Bitte die Matrize angeben.")))
                .andExpect(content().string(containsString("Bitte den Durchmesser des Dorns angeben.")))
                // what was entered is kept
                .andExpect(content().string(containsString("selected=\"selected\">Vollkalibriert<")));

        mvc.perform(form("/processed-cases", "strict-batch-user", lot,
                        "mandrelUsed", "true", "mandrelDiameter", "0", "waterCapacity", "0"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Der Durchmesser muss größer als 0 sein.")))
                .andExpect(content().string(containsString("Das Hülsenvolumen muss größer als 0 sein.")));

        mvc.perform(post("/processed-cases").with(user("strict-batch-user"))
                        .param("casingLot", lot.toString()).param("quantity", "50")
                        .param("processedOn", "2026-11-01").param("timesFired", "0").param("caseLength", "48.51"))
                .andExpect(status().isForbidden());

        assertThat(repository.findByOwner(owner)).isEmpty();
    }

    @Test
    void shouldListNewestBatchFirst() throws Exception {
        UUID owner = register("sorted-batch-user");
        UUID lot = casingLot(owner, "SORT-1");
        mvc.perform(form("/processed-cases", "sorted-batch-user", lot, "processedOn", "2026-01-05"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(form("/processed-cases", "sorted-batch-user", lot, "processedOn", "2026-03-07"))
                .andExpect(status().is3xxRedirection());

        String html = mvc.perform(get("/processed-cases").with(user("sorted-batch-user")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html.indexOf("07.03.2026")).isPositive().isLessThan(html.indexOf("05.01.2026"));
    }
}
