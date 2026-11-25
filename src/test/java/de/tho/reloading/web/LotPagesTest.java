package de.tho.reloading.web;

import de.tho.reloading.entity.Bullet;
import de.tho.reloading.entity.CasingLot;
import de.tho.reloading.entity.ProcessedCase;
import de.tho.reloading.repository.BulletLotRepository;
import de.tho.reloading.repository.BulletRepository;
import de.tho.reloading.repository.CaliberRepository;
import de.tho.reloading.repository.CasingLotRepository;
import de.tho.reloading.repository.CasingRepository;
import de.tho.reloading.repository.ManufacturerRepository;
import de.tho.reloading.repository.PowderLotRepository;
import de.tho.reloading.repository.PowderRepository;
import de.tho.reloading.repository.PrimerLotRepository;
import de.tho.reloading.repository.PrimerRepository;
import de.tho.reloading.repository.ProcessedCaseRepository;
import de.tho.reloading.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * Lots are user data: every user only sees and changes their own. The four kinds share one implementation,
 * so the casing lots are tested in depth and the others once.
 * The tests share one database and are not rolled back, so every test uses its own users.
 */
@SpringBootTest
@Testcontainers
class LotPagesTest {

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

    private MockMvc mvc;
    private UUID lapuaCreedmoor;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        UUID lapua = manufacturerRepository.findByName("Lapua").orElseThrow().getId();
        UUID creedmoor = caliberRepository.findByName("6.5 Creedmoor").orElseThrow().getId();
        lapuaCreedmoor = casingRepository.findByManufacturer(lapua).stream()
                .filter(casing -> casing.getCaliber().equals(creedmoor))
                .findFirst().orElseThrow().getId();
    }

    private UUID register(String subject) {
        return userService.register(subject, subject).getId();
    }

    /** Creates a casing lot through the form and returns it. */
    private CasingLot createCasingLot(String subject, String lotNumber) throws Exception {
        UUID owner = register(subject);
        mvc.perform(post("/casing-lots").with(user(subject)).with(csrf())
                        .param("product", lapuaCreedmoor.toString()).param("lotNumber", lotNumber))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/casing-lots"));
        return casingLotRepository.findByOwnerAndCasingAndLotNumber(owner, lapuaCreedmoor, lotNumber).orElseThrow();
    }

    @Test
    void shouldShowEmptyListAndFormWithProductsByManufacturer() throws Exception {
        register("empty-lot-user");

        mvc.perform(get("/casing-lots").with(user("empty-lot-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Du hast noch kein Hülsen-Los angelegt.")))
                .andExpect(content().string(containsString("href=\"/casing-lots/new\"")))
                .andExpect(content().string(containsString("aria-current=\"page\">Hülsen-Lose<")));
        mvc.perform(get("/casing-lots/new").with(user("empty-lot-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hülsen-Los anlegen")))
                .andExpect(content().string(containsString("<optgroup label=\"Lapua\">")))
                .andExpect(content().string(containsString(">6.5 Creedmoor<")))
                .andExpect(content().string(not(containsString("Endgültig löschen"))));
    }

    @Test
    void shouldCreateEditAndDeleteOwnCasingLot() throws Exception {
        CasingLot lot = createCasingLot("lot-user", "L-2026-001");
        assertThat(lot.getOwner()).isEqualTo(register("lot-user"));

        mvc.perform(get("/casing-lots").with(user("lot-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<td>Lapua 6.5 Creedmoor</td>")))
                .andExpect(content().string(containsString("<td>L-2026-001</td>")))
                .andExpect(content().string(containsString("Einträge 1–1 von 1")));

        mvc.perform(get("/casing-lots/" + lot.getId() + "/edit").with(user("lot-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hülsen-Los bearbeiten")))
                .andExpect(content().string(containsString("value=\"L-2026-001\"")))
                .andExpect(content().string(containsString("selected=\"selected\">6.5 Creedmoor<")))
                .andExpect(content().string(containsString("Endgültig löschen")));

        mvc.perform(post("/casing-lots/" + lot.getId()).with(user("lot-user")).with(csrf())
                        .param("product", lapuaCreedmoor.toString()).param("lotNumber", " L-2026-002 "))
                .andExpect(status().is3xxRedirection());
        assertThat(casingLotRepository.findByIdAndOwner(lot.getId(), lot.getOwner()).orElseThrow().getLotNumber())
                .isEqualTo("L-2026-002");

        mvc.perform(post("/casing-lots/" + lot.getId() + "/delete").with(user("lot-user")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/casing-lots"));
        assertThat(casingLotRepository.findByOwner(lot.getOwner())).isEmpty();
    }

    @Test
    void shouldHideLotsFromOtherUsers() throws Exception {
        CasingLot lot = createCasingLot("lot-owner-a", "SECRET-1");
        UUID other = register("lot-owner-b");

        mvc.perform(get("/casing-lots").with(user("lot-owner-b")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("SECRET-1"))));
        mvc.perform(get("/casing-lots/" + lot.getId() + "/edit").with(user("lot-owner-b")))
                .andExpect(status().isNotFound());
        mvc.perform(post("/casing-lots/" + lot.getId()).with(user("lot-owner-b")).with(csrf())
                        .param("product", lapuaCreedmoor.toString()).param("lotNumber", "TAKEN"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/casing-lots/" + lot.getId() + "/delete").with(user("lot-owner-b")).with(csrf()))
                .andExpect(status().isNotFound());

        assertThat(casingLotRepository.findByIdAndOwner(lot.getId(), lot.getOwner()).orElseThrow().getLotNumber())
                .isEqualTo("SECRET-1");
        assertThat(casingLotRepository.findByOwner(other)).isEmpty();

        // the same lot number is fine for another owner
        createCasingLot("lot-owner-b", "SECRET-1");
    }

    @Test
    void shouldRejectInvalidUnknownAndDuplicateLots() throws Exception {
        createCasingLot("strict-lot-user", "DUP-1");

        mvc.perform(post("/casing-lots").with(user("strict-lot-user")).with(csrf())
                        .param("product", "").param("lotNumber", " "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bitte ein Produkt wählen.")))
                .andExpect(content().string(containsString("Bitte eine Losnummer angeben.")));

        // an id that is no casing, e.g. taken from another list
        mvc.perform(post("/casing-lots").with(user("strict-lot-user")).with(csrf())
                        .param("product", UUID.randomUUID().toString()).param("lotNumber", "X-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bitte ein Produkt aus der Liste wählen.")));

        mvc.perform(post("/casing-lots").with(user("strict-lot-user")).with(csrf())
                        .param("product", lapuaCreedmoor.toString()).param("lotNumber", "dup-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("bereits angelegt")));

        mvc.perform(post("/casing-lots").with(user("strict-lot-user"))
                        .param("product", lapuaCreedmoor.toString()).param("lotNumber", "NO-TOKEN"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldNotDeleteLotThatIsInUse() throws Exception {
        CasingLot lot = createCasingLot("used-lot-user", "USED-1");
        ProcessedCase batch = new ProcessedCase();
        batch.setOwner(lot.getOwner());
        batch.setCasingLot(lot.getId());
        batch.setQuantity(50);
        batch.setProcessedOn(LocalDate.of(2026, 11, 1));
        batch.setTimesFired(0);
        batch.setTrimmed(false);
        batch.setCaseLength(new BigDecimal("0.0486"));
        processedCaseRepository.save(batch);

        mvc.perform(post("/casing-lots/" + lot.getId() + "/delete").with(user("used-lot-user")).with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(casingLotRepository.findByIdAndOwner(lot.getId(), lot.getOwner())).isPresent();
    }

    @Test
    void shouldManageBulletPowderAndPrimerLots() throws Exception {
        UUID owner = register("all-lots-user");
        UUID hornady = manufacturerRepository.findByName("Hornady").orElseThrow().getId();
        Bullet bullet = bulletRepository.findByName("ELD Match").stream()
                .filter(each -> each.getManufacturer().equals(hornady))
                .filter(each -> each.getWeight().subtract(new BigDecimal("0.0090718474")).abs()
                        .compareTo(new BigDecimal("0.000001")) < 0)
                .findFirst().orElseThrow();
        UUID powder = powderRepository.findByName("N555").getFirst().getId();
        UUID primer = primerRepository.findByName("BR-4").getFirst().getId();

        mvc.perform(get("/bullet-lots/new").with(user("all-lots-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Geschoss-Los anlegen")))
                .andExpect(content().string(containsString(">ELD Match, .264 in (6.71 mm), 140 gr<")));
        mvc.perform(post("/bullet-lots").with(user("all-lots-user")).with(csrf())
                        .param("product", bullet.getId().toString()).param("lotNumber", "B-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bullet-lots"));
        mvc.perform(get("/bullet-lots").with(user("all-lots-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<td>Hornady ELD Match, .264 in (6.71 mm), 140 gr</td>")))
                .andExpect(content().string(containsString("aria-current=\"page\">Geschoss-Lose<")));
        assertThat(bulletLotRepository.findByOwnerAndBulletAndLotNumber(owner, bullet.getId(), "B-1")).isPresent();

        mvc.perform(post("/powder-lots").with(user("all-lots-user")).with(csrf())
                        .param("product", powder.toString()).param("lotNumber", "P-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/powder-lots"));
        mvc.perform(get("/powder-lots").with(user("all-lots-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<td>Vihtavuori N555</td>")));
        assertThat(powderLotRepository.findByOwnerAndPowderAndLotNumber(owner, powder, "P-1")).isPresent();

        mvc.perform(post("/primer-lots").with(user("all-lots-user")).with(csrf())
                        .param("product", primer.toString()).param("lotNumber", "Z-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/primer-lots"));
        mvc.perform(get("/primer-lots").with(user("all-lots-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<td>CCI BR-4 (Small Rifle)</td>")));
        assertThat(primerLotRepository.findByOwnerAndPrimerAndLotNumber(owner, primer, "Z-1")).isPresent();

        // a powder is no bullet
        mvc.perform(post("/bullet-lots").with(user("all-lots-user")).with(csrf())
                        .param("product", powder.toString()).param("lotNumber", "WRONG"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bitte ein Produkt aus der Liste wählen.")));
    }

    @Test
    void shouldLinkLotsOnTheStartPage() throws Exception {
        register("start-lot-user");

        mvc.perform(get("/").with(user("start-lot-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/casing-lots\"")))
                .andExpect(content().string(containsString("href=\"/bullet-lots\"")))
                .andExpect(content().string(containsString("href=\"/powder-lots\"")))
                .andExpect(content().string(containsString("href=\"/primer-lots\"")));
    }
}
