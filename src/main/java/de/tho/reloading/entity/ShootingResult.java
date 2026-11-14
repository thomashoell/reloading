package de.tho.reloading.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Result of shooting a {@link Load} from a rifle.
 * Measures are stored in SI base units (m, kg, s, K); conversion to grains, inches etc. is a concern of the UI.
 */
@Getter
@Setter
@NoArgsConstructor
public class ShootingResult {

    @Id
    private UUID id;

    // the app_user this row belongs to
    private UUID owner;

    private UUID load;
    private UUID rifle;
    private LocalDate shotOn;
    // meters
    private BigDecimal distance;
    private Integer shotCount;
    // meters per second
    private BigDecimal velocityMean;
    // standard deviation, meters per second
    private BigDecimal velocitySd;
    // extreme spread, meters per second
    private BigDecimal velocityEs;
    // meters
    private BigDecimal groupSize;
    // kelvin
    private BigDecimal temperature;
    private String notes;
}
