package de.tho.reloading.web;

import de.tho.reloading.entity.AppUser;
import de.tho.reloading.entity.Rifle;
import de.tho.reloading.repository.AppUserRepository;
import de.tho.reloading.repository.CaliberRepository;
import de.tho.reloading.repository.ManufacturerRepository;
import de.tho.reloading.repository.RifleRepository;
import de.tho.reloading.user.UserService;
import de.tho.reloading.utility.LengthUnit;
import de.tho.reloading.utility.WeightUnit;
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
 * Rifles are user data: every user only sees and changes their own.
 * The tests share one database and are not rolled back, so every test uses its own users.
 */
@SpringBootTest
@Testcontainers
class RiflePagesTest {

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
    private RifleRepository rifleRepository;

    @Autowired
    private ManufacturerRepository manufacturerRepository;

    @Autowired
    private CaliberRepository caliberRepository;

    private MockMvc mvc;
    private String sako;
    private String creedmoor;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        sako = manufacturerRepository.findByName("Sako").orElseThrow().getId().toString();
        creedmoor = caliberRepository.findByName("6.5 Creedmoor").orElseThrow().getId().toString();
    }

    private UUID register(String subject) {
        return userService.register(subject, subject).getId();
    }

    /** Creates a rifle through the form and returns it. */
    private Rifle createRifle(String subject, String name) throws Exception {
        UUID owner = register(subject);
        mvc.perform(post("/rifles").with(user(subject)).with(csrf())
                        .param("name", name).param("manufacturer", sako).param("caliber", creedmoor)
                        .param("barrelLength", "660").param("lengthOfTwist", "203.2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/rifles"));
        return rifleRepository.findByOwnerAndName(owner, name).orElseThrow();
    }

    @Test
    void shouldShowEmptyListAndFormToNewUser() throws Exception {
        register("empty-user");

        mvc.perform(get("/rifles").with(user("empty-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Du hast noch keine Waffe angelegt.")))
                .andExpect(content().string(containsString("href=\"/rifles/new\"")))
                .andExpect(content().string(containsString("aria-current=\"page\">Waffen<")));
        mvc.perform(get("/rifles/new").with(user("empty-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Waffe anlegen")))
                .andExpect(content().string(containsString(">Sako<")))
                .andExpect(content().string(containsString(">6.5 Creedmoor<")))
                .andExpect(content().string(not(containsString("Endgültig löschen"))))
                // each length has a millimeter field that is sent and an inch field that is only converted
                .andExpect(content().string(containsString("name=\"barrelLength\"")))
                .andExpect(content().string(containsString("id=\"barrelLength-in\"")))
                .andExpect(content().string(not(containsString("name=\"barrelLength-in\""))))
                .andExpect(content().string(containsString("src=\"/js/length-pair.js\"")));
        mvc.perform(get("/js/length-pair.js").with(user("empty-user")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldStoreRifleInSiUnitsForItsOwner() throws Exception {
        Rifle rifle = createRifle("si-user", "TRG 22");

        // the form sends millimeters, the database stores meters
        assertThat(rifle.getOwner()).isEqualTo(register("si-user"));
        assertThat(rifle.getBarrelLength()).isEqualByComparingTo("0.66");
        assertThat(rifle.getLengthOfTwist()).isEqualByComparingTo("0.2032");

        mvc.perform(get("/rifles").with(user("si-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<td>TRG 22</td>")))
                .andExpect(content().string(containsString("<td>Sako</td>")))
                .andExpect(content().string(containsString(">660 mm (25.98 in)<")))
                .andExpect(content().string(containsString(">203.2 mm (8 in)<")))
                .andExpect(content().string(containsString("Einträge 1–1 von 1")));
    }

    @Test
    void shouldUseMillimetersInTheFormWhateverTheUserPrefers() throws Exception {
        Rifle rifle = createRifle("inch-user", "TRG 22");
        userService.updateUnits(rifle.getOwner(), WeightUnit.GRAIN, LengthUnit.INCH);

        mvc.perform(get("/rifles").with(user("inch-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">660 mm (25.98 in)<")));
        mvc.perform(get("/rifles/" + rifle.getId() + "/edit").with(user("inch-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"660\"")))
                .andExpect(content().string(containsString("value=\"203.2\"")))
                .andExpect(content().string(containsString("Endgültig löschen")));

        // 26 in, converted to millimeters by the browser
        mvc.perform(post("/rifles/" + rifle.getId()).with(user("inch-user")).with(csrf())
                        .param("name", "TRG 22").param("manufacturer", sako).param("caliber", creedmoor)
                        .param("barrelLength", "660.4").param("lengthOfTwist", "203.2"))
                .andExpect(status().is3xxRedirection());

        Rifle changed = rifleRepository.findByIdAndOwner(rifle.getId(), rifle.getOwner()).orElseThrow();
        assertThat(changed.getBarrelLength()).isEqualByComparingTo("0.6604");
        assertThat(changed.getLengthOfTwist()).isEqualByComparingTo("0.2032");
    }

    @Test
    void shouldRenameAndDeleteOwnRifle() throws Exception {
        Rifle rifle = createRifle("edit-user", "85 Hunter");

        mvc.perform(get("/rifles/" + rifle.getId() + "/edit").with(user("edit-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Waffe bearbeiten")))
                .andExpect(content().string(containsString("value=\"85 Hunter\"")))
                .andExpect(content().string(containsString("value=\"660\"")));

        mvc.perform(post("/rifles/" + rifle.getId()).with(user("edit-user")).with(csrf())
                        .param("name", "85 Finnlight").param("manufacturer", sako).param("caliber", creedmoor)
                        .param("barrelLength", "").param("lengthOfTwist", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/rifles"));
        Rifle changed = rifleRepository.findByIdAndOwner(rifle.getId(), rifle.getOwner()).orElseThrow();
        assertThat(changed.getName()).isEqualTo("85 Finnlight");
        assertThat(changed.getBarrelLength()).isNull();
        assertThat(changed.getLengthOfTwist()).isNull();

        mvc.perform(post("/rifles/" + rifle.getId() + "/delete").with(user("edit-user")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/rifles"));
        assertThat(rifleRepository.findByOwner(rifle.getOwner())).isEmpty();
    }

    @Test
    void shouldHideRiflesFromOtherUsers() throws Exception {
        Rifle rifle = createRifle("owner-a", "Secret Rifle");
        register("owner-b");

        mvc.perform(get("/rifles").with(user("owner-b")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Secret Rifle"))));
        mvc.perform(get("/rifles/" + rifle.getId() + "/edit").with(user("owner-b")))
                .andExpect(status().isNotFound());
        mvc.perform(post("/rifles/" + rifle.getId()).with(user("owner-b")).with(csrf())
                        .param("name", "Taken Over").param("manufacturer", sako).param("caliber", creedmoor))
                .andExpect(status().isNotFound());
        mvc.perform(post("/rifles/" + rifle.getId() + "/delete").with(user("owner-b")).with(csrf()))
                .andExpect(status().isNotFound());

        Rifle untouched = rifleRepository.findByIdAndOwner(rifle.getId(), rifle.getOwner()).orElseThrow();
        assertThat(untouched.getName()).isEqualTo("Secret Rifle");
        AppUser other = appUserRepository.findBySubject("owner-b").orElseThrow();
        assertThat(rifleRepository.findByOwner(other.getId())).isEmpty();
    }

    @Test
    void shouldIgnoreAnOwnerInTheRequest() throws Exception {
        UUID victim = register("victim");
        UUID attacker = register("attacker");

        mvc.perform(post("/rifles").with(user("attacker")).with(csrf())
                        .param("name", "Planted").param("manufacturer", sako).param("caliber", creedmoor)
                        .param("owner", victim.toString()))
                .andExpect(status().is3xxRedirection());

        assertThat(rifleRepository.findByOwner(victim)).isEmpty();
        assertThat(rifleRepository.findByOwnerAndName(attacker, "Planted")).isPresent();
    }

    @Test
    void shouldRejectInvalidAndDuplicateRifles() throws Exception {
        createRifle("strict-user", "TRG 42");

        mvc.perform(post("/rifles").with(user("strict-user")).with(csrf())
                        .param("name", " ").param("manufacturer", "").param("caliber", "")
                        .param("barrelLength", "0").param("lengthOfTwist", "-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bitte eine Bezeichnung angeben.")))
                .andExpect(content().string(containsString("Bitte einen Hersteller wählen.")))
                .andExpect(content().string(containsString("Bitte ein Kaliber wählen.")))
                .andExpect(content().string(containsString("Die Lauflänge muss größer als 0 sein.")))
                .andExpect(content().string(containsString("Die Dralllänge muss größer als 0 sein.")));

        mvc.perform(post("/rifles").with(user("strict-user")).with(csrf())
                        .param("name", "trg 42").param("manufacturer", sako).param("caliber", creedmoor))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("hast du bereits angelegt")));

        // another user may own the same rifle
        createRifle("second-strict-user", "TRG 42");
    }

    @Test
    void shouldRequireCsrfTokenToChangeRifles() throws Exception {
        register("csrf-user");

        mvc.perform(post("/rifles").with(user("csrf-user"))
                        .param("name", "No Token").param("manufacturer", sako).param("caliber", creedmoor))
                .andExpect(status().isForbidden());
    }
}
