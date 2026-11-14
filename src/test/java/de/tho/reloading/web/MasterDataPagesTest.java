package de.tho.reloading.web;

import de.tho.reloading.entity.AppUser;
import de.tho.reloading.repository.AppUserRepository;
import de.tho.reloading.repository.CaliberRepository;
import de.tho.reloading.repository.ManufacturerRepository;
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
 * Pages of the master data (read-only, the rows come from the seed changesets) and the settings.
 * The tests share one database and are not rolled back.
 */
@SpringBootTest
@Testcontainers
class MasterDataPagesTest {

    private static final String ADMIN = "admin-subject";
    private static final String USER = "user-subject";

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

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        // the first user of the database is the administrator
        userService.register(ADMIN, "admin");
        userService.register(USER, "user");
    }

    @Test
    void shouldShowHomeWithNavigation() throws Exception {
        mvc.perform(get("/").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/webjars/bootstrap/css/bootstrap.min.css")))
                .andExpect(content().string(containsString("href=\"/manufacturers\"")))
                .andExpect(content().string(containsString("href=\"/primers\"")))
                .andExpect(content().string(containsString("Benutzer")));
    }

    @Test
    void shouldServeBootstrap() throws Exception {
        mvc.perform(get("/webjars/bootstrap/css/bootstrap.min.css").with(user(USER)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldPageManufacturersTwentyAtATime() throws Exception {
        // 45 manufacturers: 20 + 20 + 5
        mvc.perform(get("/manufacturers").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Accuracy International<")))
                .andExpect(content().string(not(containsString(">Lapua<"))))
                .andExpect(content().string(containsString("Einträge 1–20 von 45")))
                .andExpect(content().string(containsString("href=\"/manufacturers?page=2\"")))
                .andExpect(content().string(containsString("href=\"/manufacturers?page=3\"")));

        mvc.perform(get("/manufacturers").param("page", "3").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Winchester<")))
                .andExpect(content().string(not(containsString(">Accuracy International<"))))
                .andExpect(content().string(containsString("Einträge 41–45 von 45")));

        // a page beyond the end shows the last one
        mvc.perform(get("/manufacturers").param("page", "99").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Winchester<")));

        mvc.perform(get("/manufacturers").param("size", "50").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Accuracy International<")))
                .andExpect(content().string(containsString(">Winchester<")))
                .andExpect(content().string(not(containsString("page=2"))));
    }

    @Test
    void shouldFilterManufacturersAndCalibersByName() throws Exception {
        mvc.perform(get("/manufacturers").param("q", "LAP").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Lapua<")))
                .andExpect(content().string(not(containsString(">Norma<"))))
                .andExpect(content().string(containsString("Einträge 1–1 von 1")));
        mvc.perform(get("/calibers").param("q", "creedmoor").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">6.5 Creedmoor<")))
                .andExpect(content().string(containsString(">6 mm Creedmoor<")))
                .andExpect(content().string(not(containsString(">.308 Winchester<"))));
        mvc.perform(get("/calibers").param("q", "no such caliber").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Keine Einträge gefunden.")));
    }

    @Test
    void shouldOfferNoWayToChangeMasterDataEvenToTheAdministrator() throws Exception {
        mvc.perform(get("/manufacturers").with(user(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("/manufacturers/new"))))
                .andExpect(content().string(not(containsString("/delete"))))
                .andExpect(content().string(not(containsString("/edit"))));

        mvc.perform(get("/manufacturers/new").with(user(ADMIN)))
                .andExpect(status().is4xxClientError());
        mvc.perform(post("/manufacturers").with(user(ADMIN)).with(csrf()).param("name", "Not Seeded"))
                .andExpect(status().is4xxClientError());
        mvc.perform(post("/bullets").with(user(ADMIN)).with(csrf()).param("name", "Not Seeded"))
                .andExpect(status().is4xxClientError());

        assertThat(manufacturerRepository.findByName("Not Seeded")).isEmpty();
    }

    @Test
    void shouldFilterProductsByManufacturer() throws Exception {
        String vihtavuori = manufacturerRepository.findByName("Vihtavuori").orElseThrow().getId().toString();

        // 66 powders, sorted by manufacturer: N555 is not on the first page
        mvc.perform(get("/powders").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Einträge 1–20 von 66")))
                .andExpect(content().string(not(containsString(">N555<"))));

        // exactly the 20 powders of Vihtavuori: one page
        mvc.perform(get("/powders").param("manufacturer", vihtavuori).with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">N555<")))
                .andExpect(content().string(not(containsString(">Varget<"))))
                .andExpect(content().string(containsString("Einträge 1–20 von 20")))
                .andExpect(content().string(not(containsString("page=2"))));

        // the links to other pages keep the filter and the page size
        mvc.perform(get("/powders").param("manufacturer", vihtavuori).param("size", "5").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Einträge 1–5 von 20")))
                .andExpect(content().string(containsString("page=2&amp;size=5&amp;manufacturer=" + vihtavuori)));

        String cci = manufacturerRepository.findByName("CCI").orElseThrow().getId().toString();
        mvc.perform(get("/primers").param("manufacturer", cci).with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">BR-4<")))
                .andExpect(content().string(containsString(">Small Rifle Magnum<")))
                .andExpect(content().string(not(containsString(">205M<"))));
    }

    @Test
    void shouldFilterCasingsByManufacturerAndCaliber() throws Exception {
        String peterson = manufacturerRepository.findByName("Peterson").orElseThrow().getId().toString();
        String creedmoor = caliberRepository.findByName("6.5 Creedmoor").orElseThrow().getId().toString();

        mvc.perform(get("/casings").param("manufacturer", peterson).with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Einträge 1–6 von 6")))
                .andExpect(content().string(containsString("<td>.300 Norma Magnum</td>")));
        mvc.perform(get("/casings").param("caliber", creedmoor).with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<td>Lapua</td>")))
                .andExpect(content().string(containsString("<td>Starline</td>")))
                .andExpect(content().string(not(containsString("<td>RWS</td>"))));
        mvc.perform(get("/casings").param("manufacturer", peterson).param("caliber", creedmoor).with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Einträge 1–1 von 1")));
        mvc.perform(get("/casings").param("manufacturer", "").param("caliber", "").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("von 79")));
    }

    @Test
    void shouldShowBulletsInTheUnitsOfTheUser() throws Exception {
        // defaults: grains and millimeters; the diameter is always shown in inches and millimeters
        String hornady = manufacturerRepository.findByName("Hornady").orElseThrow().getId().toString();
        mvc.perform(get("/bullets").param("manufacturer", hornady).param("size", "100").with(user(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("von 41")))
                .andExpect(content().string(containsString(">ELD Match<")))
                .andExpect(content().string(containsString(">140<")))
                .andExpect(content().string(containsString(">.264 in (6.71 mm)<")))
                .andExpect(content().string(containsString(">gr<")))
                .andExpect(content().string(containsString(">mm<")));

        AppUser user = appUserRepository.findBySubject(USER).orElseThrow();
        userService.updateUnits(user.getId(), WeightUnit.GRAM, LengthUnit.INCH);

        mvc.perform(get("/bullets").param("manufacturer", hornady).param("size", "100").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">9.072<")))
                .andExpect(content().string(containsString(">.264 in (6.71 mm)<")))
                .andExpect(content().string(containsString(">g<")))
                .andExpect(content().string(containsString(">in<")));
    }

    @Test
    void shouldFilterBulletsByDiameter() throws Exception {
        String hornady = manufacturerRepository.findByName("Hornady").orElseThrow().getId().toString();

        // .264 in = 0.0067056 m; the filter uses the SI value, the label shows inches and millimeters
        mvc.perform(get("/bullets").param("diameter", "0.0067056").param("size", "100").with(user(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Einträge 1–13 von 13")))
                .andExpect(content().string(containsString(">143<")))
                .andExpect(content().string(not(containsString(">168<"))))
                .andExpect(content().string(containsString(">.264 in (6.71 mm)</option>")))
                .andExpect(content().string(containsString(">.308 in (7.82 mm)</option>")))
                .andExpect(content().string(containsString(">.264 in (6.71 mm)</td>")));

        mvc.perform(get("/bullets").param("diameter", "0.0067056").param("manufacturer", hornady).with(user(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Einträge 1–5 von 5")));

        // the links to other pages keep the diameter
        mvc.perform(get("/bullets").param("diameter", "0.0067056").param("size", "5").with(user(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("diameter=0.0067056")));

        mvc.perform(get("/bullets").param("diameter", "").with(user(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("von 80")));
    }

    @Test
    void shouldFilterBulletsByWeight() throws Exception {
        // 140 gr = 0.0090718474 kg; the filter uses the SI value, the label the weight unit of the user
        mvc.perform(get("/bullets").param("weight", "0.0090718474").with(user(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Einträge 1–3 von 3")))
                .andExpect(content().string(containsString("<td>Berger</td>")))
                .andExpect(content().string(containsString("<td>Sierra</td>")))
                .andExpect(content().string(containsString(">140 gr</option>")))
                .andExpect(content().string(containsString(">168 gr</option>")));

        // with a diameter selected, only the weights of that diameter are offered
        mvc.perform(get("/bullets").param("diameter", "0.0067056").with(user(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">143 gr</option>")))
                .andExpect(content().string(not(containsString(">168 gr</option>"))));

        // the links to other pages keep the weight
        mvc.perform(get("/bullets").param("weight", "0.0090718474").param("size", "2").with(user(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Einträge 1–2 von 3")))
                .andExpect(content().string(containsString("weight=0.0090718474")));

        mvc.perform(get("/bullets").param("weight", "").with(user(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("von 80")));
    }

    @Test
    void shouldSaveUnitSettingsOfTheCurrentUserOnly() throws Exception {
        userService.register("settings-subject", "settings");
        AppUser admin = appUserRepository.findBySubject(ADMIN).orElseThrow();

        mvc.perform(get("/settings").with(user("settings-subject")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Grain (gr)")));

        // an id in the request must not redirect the change to another user
        mvc.perform(post("/settings").with(user("settings-subject")).with(csrf())
                        .param("weightUnit", "GRAM").param("lengthUnit", "INCH").param("id", admin.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings"));

        AppUser changed = appUserRepository.findBySubject("settings-subject").orElseThrow();
        assertThat(changed.getWeightUnit()).isEqualTo(WeightUnit.GRAM);
        assertThat(changed.getLengthUnit()).isEqualTo(LengthUnit.INCH);
        AppUser untouched = appUserRepository.findBySubject(ADMIN).orElseThrow();
        assertThat(untouched.getWeightUnit()).isEqualTo(WeightUnit.GRAIN);
        assertThat(untouched.getLengthUnit()).isEqualTo(LengthUnit.MILLIMETER);
    }
}
