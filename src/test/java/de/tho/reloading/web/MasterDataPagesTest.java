package de.tho.reloading.web;

import de.tho.reloading.entity.AppUser;
import de.tho.reloading.entity.Bullet;
import de.tho.reloading.entity.Manufacturer;
import de.tho.reloading.repository.AppUserRepository;
import de.tho.reloading.repository.BulletRepository;
import de.tho.reloading.repository.CaliberRepository;
import de.tho.reloading.repository.CasingRepository;
import de.tho.reloading.repository.ManufacturerRepository;
import de.tho.reloading.repository.PowderRepository;
import de.tho.reloading.repository.PrimerRepository;
import de.tho.reloading.user.UserService;
import de.tho.reloading.utility.LengthUnit;
import de.tho.reloading.utility.PrimerSize;
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
 * Pages of the master data and the settings. The tests share one database and are not rolled back,
 * so every test uses its own names.
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
    private BulletRepository bulletRepository;

    @Autowired
    private CaliberRepository caliberRepository;

    @Autowired
    private CasingRepository casingRepository;

    @Autowired
    private PowderRepository powderRepository;

    @Autowired
    private PrimerRepository primerRepository;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        // the first user of the database is the administrator
        userService.register(ADMIN, "admin");
        userService.register(USER, "user");
    }

    private UUID createManufacturer(String name) throws Exception {
        mvc.perform(post("/manufacturers").with(user(ADMIN)).with(csrf()).param("name", name))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manufacturers"));
        return manufacturerRepository.findByName(name).orElseThrow().getId();
    }

    @Test
    void shouldShowHomeWithNavigation() throws Exception {
        mvc.perform(get("/").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/webjars/bootstrap/css/bootstrap.min.css")))
                .andExpect(content().string(containsString("href=\"/manufacturers\"")))
                .andExpect(content().string(containsString("Benutzer")));
    }

    @Test
    void shouldServeBootstrap() throws Exception {
        mvc.perform(get("/webjars/bootstrap/css/bootstrap.min.css").with(user(USER)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldLetAdministratorCreateRenameAndDeleteManufacturer() throws Exception {
        UUID id = createManufacturer("Sierra");

        mvc.perform(get("/manufacturers").with(user(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sierra")))
                .andExpect(content().string(containsString("/manufacturers/new")));

        mvc.perform(get("/manufacturers/" + id + "/edit").with(user(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"Sierra\"")));

        mvc.perform(post("/manufacturers/" + id).with(user(ADMIN)).with(csrf()).param("name", "Sierra Bullets"))
                .andExpect(status().is3xxRedirection());
        assertThat(manufacturerRepository.findById(id).orElseThrow().getName()).isEqualTo("Sierra Bullets");

        mvc.perform(post("/manufacturers/" + id + "/delete").with(user(ADMIN)).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(manufacturerRepository.findById(id)).isEmpty();
    }

    @Test
    void shouldShowManufacturersReadOnlyToUsers() throws Exception {
        UUID id = createManufacturer("Berger");

        mvc.perform(get("/manufacturers").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Berger")))
                .andExpect(content().string(not(containsString("/manufacturers/new"))))
                .andExpect(content().string(not(containsString("/delete"))));

        mvc.perform(get("/manufacturers/new").with(user(USER)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/manufacturers").with(user(USER)).with(csrf()).param("name", "Nosler"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/manufacturers/" + id).with(user(USER)).with(csrf()).param("name", "Changed"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/manufacturers/" + id + "/delete").with(user(USER)).with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(manufacturerRepository.findByName("Nosler")).isEmpty();
        assertThat(manufacturerRepository.findById(id).orElseThrow().getName()).isEqualTo("Berger");
    }

    @Test
    void shouldRejectBlankAndDuplicateNames() throws Exception {
        createManufacturer("Norma");

        mvc.perform(post("/manufacturers").with(user(ADMIN)).with(csrf()).param("name", " "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bitte einen Namen angeben.")));

        mvc.perform(post("/manufacturers").with(user(ADMIN)).with(csrf()).param("name", "norma"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Diesen Namen gibt es bereits.")));
    }

    @Test
    void shouldNotDeleteManufacturerInUse() throws Exception {
        UUID manufacturer = createManufacturer("Lapua");
        mvc.perform(post("/bullets").with(user(ADMIN)).with(csrf())
                        .param("manufacturer", manufacturer.toString()).param("name", "Scenar")
                        .param("diameter", "6.71").param("weight", "139"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post("/manufacturers/" + manufacturer + "/delete").with(user(ADMIN)).with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(manufacturerRepository.findById(manufacturer)).isPresent();
    }

    @Test
    void shouldManageCalibers() throws Exception {
        mvc.perform(post("/calibers").with(user(ADMIN)).with(csrf()).param("name", "6.5 Creedmoor"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calibers"));

        mvc.perform(get("/calibers").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("6.5 Creedmoor")));
        mvc.perform(post("/calibers").with(user(USER)).with(csrf()).param("name", ".308 Winchester"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldStoreBulletInSiUnitsAndShowItInTheUnitsOfTheUser() throws Exception {
        UUID manufacturer = createManufacturer("Hornady");

        // the administrator works with the defaults: grains and millimeters
        mvc.perform(post("/bullets").with(user(ADMIN)).with(csrf())
                        .param("manufacturer", manufacturer.toString()).param("name", "ELD Match")
                        .param("diameter", "6.71").param("weight", "140").param("length", "35.1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bullets"));

        Bullet bullet = bulletRepository.findByName("ELD Match").getFirst();
        assertThat(bullet.getManufacturer()).isEqualTo(manufacturer);
        assertThat(bullet.getDiameter()).isEqualByComparingTo("0.00671");
        assertThat(bullet.getWeight()).isEqualByComparingTo("0.0090718474");
        assertThat(bullet.getLength()).isEqualByComparingTo("0.0351");

        mvc.perform(get("/bullets").with(user(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hornady")))
                .andExpect(content().string(containsString(">140<")))
                .andExpect(content().string(containsString(">6.71<")));

        // the user prefers grams and inches
        AppUser user = appUserRepository.findBySubject(USER).orElseThrow();
        userService.updateUnits(user.getId(), WeightUnit.GRAM, LengthUnit.INCH);

        mvc.perform(get("/bullets").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">9.072<")))
                .andExpect(content().string(containsString(">0.2642<")));

        mvc.perform(get("/bullets/" + bullet.getId() + "/edit").with(user(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"140\"")))
                .andExpect(content().string(containsString("value=\"6.71\"")));
    }

    @Test
    void shouldRejectInvalidAndDuplicateBullets() throws Exception {
        UUID manufacturer = createManufacturer("Speer");

        mvc.perform(post("/bullets").with(user(ADMIN)).with(csrf())
                        .param("manufacturer", "").param("name", "").param("diameter", "0").param("weight", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bitte einen Hersteller wählen.")))
                .andExpect(content().string(containsString("Bitte eine Bezeichnung angeben.")))
                .andExpect(content().string(containsString("Der Durchmesser muss größer als 0 sein.")))
                .andExpect(content().string(containsString("Bitte das Gewicht angeben.")));

        mvc.perform(post("/bullets").with(user(ADMIN)).with(csrf())
                        .param("manufacturer", manufacturer.toString()).param("name", "Gold Dot")
                        .param("diameter", "7.82").param("weight", "150"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/bullets").with(user(ADMIN)).with(csrf())
                        .param("manufacturer", manufacturer.toString()).param("name", "gold dot")
                        .param("diameter", "7.82").param("weight", "150"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("gibt es bereits")));

        mvc.perform(post("/bullets").with(user(USER)).with(csrf())
                        .param("manufacturer", manufacturer.toString()).param("name", "TMJ")
                        .param("diameter", "7.82").param("weight", "150"))
                .andExpect(status().isForbidden());
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

    @Test
    void shouldManagePowders() throws Exception {
        UUID manufacturer = createManufacturer("Vihtavuori");

        mvc.perform(post("/powders").with(user(ADMIN)).with(csrf())
                        .param("manufacturer", manufacturer.toString()).param("name", "N555"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/powders"));
        UUID id = powderRepository.findByName("N555").orElseThrow().getId();

        mvc.perform(get("/powders").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Vihtavuori")))
                .andExpect(content().string(containsString("N555")))
                .andExpect(content().string(not(containsString("/powders/new"))));
        mvc.perform(get("/powders/" + id + "/edit").with(user(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"N555\"")));

        mvc.perform(post("/powders").with(user(ADMIN)).with(csrf())
                        .param("manufacturer", manufacturer.toString()).param("name", "n555"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("gibt es bei diesem Hersteller bereits")));
        mvc.perform(post("/powders").with(user(USER)).with(csrf())
                        .param("manufacturer", manufacturer.toString()).param("name", "N560"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/powders/" + id + "/delete").with(user(ADMIN)).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(powderRepository.findById(id)).isEmpty();
    }

    @Test
    void shouldManagePrimers() throws Exception {
        UUID manufacturer = createManufacturer("CCI");

        mvc.perform(get("/primers/new").with(user(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Small Rifle Magnum")));
        mvc.perform(post("/primers").with(user(ADMIN)).with(csrf())
                        .param("manufacturer", manufacturer.toString()).param("name", "BR-4")
                        .param("size", "SMALL_RIFLE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/primers"));
        assertThat(primerRepository.findByName("BR-4").getFirst().getSize()).isEqualTo(PrimerSize.SMALL_RIFLE);

        mvc.perform(get("/primers").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("BR-4")))
                .andExpect(content().string(containsString("Small Rifle")));

        mvc.perform(post("/primers").with(user(ADMIN)).with(csrf())
                        .param("manufacturer", manufacturer.toString()).param("name", "450").param("size", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bitte eine Größe wählen.")));
        mvc.perform(post("/primers").with(user(USER)).with(csrf())
                        .param("manufacturer", manufacturer.toString()).param("name", "450")
                        .param("size", "SMALL_RIFLE_MAGNUM"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldManageCasings() throws Exception {
        UUID manufacturer = createManufacturer("Peterson");
        mvc.perform(post("/calibers").with(user(ADMIN)).with(csrf()).param("name", "6 mm Dasher"))
                .andExpect(status().is3xxRedirection());
        UUID caliber = caliberRepository.findByName("6 mm Dasher").orElseThrow().getId();

        mvc.perform(post("/casings").with(user(ADMIN)).with(csrf())
                        .param("manufacturer", manufacturer.toString()).param("caliber", caliber.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/casings"));
        UUID id = casingRepository.findByManufacturer(manufacturer).getFirst().getId();

        mvc.perform(get("/casings").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Peterson")))
                .andExpect(content().string(containsString("6 mm Dasher")));
        mvc.perform(get("/casings/" + id + "/edit").with(user(ADMIN)))
                .andExpect(status().isOk());

        mvc.perform(post("/casings").with(user(ADMIN)).with(csrf())
                        .param("manufacturer", manufacturer.toString()).param("caliber", caliber.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("gibt es bereits")));
        mvc.perform(post("/casings").with(user(ADMIN)).with(csrf()).param("manufacturer", "").param("caliber", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bitte ein Kaliber wählen.")));
        mvc.perform(post("/casings/" + id + "/delete").with(user(USER)).with(csrf()))
                .andExpect(status().isForbidden());

        // a caliber that is used by a casing can't be deleted
        mvc.perform(post("/calibers/" + caliber + "/delete").with(user(ADMIN)).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(caliberRepository.findById(caliber)).isPresent();
    }
}
