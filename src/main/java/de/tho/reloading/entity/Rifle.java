package de.tho.reloading.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Measures are stored in SI base units (m, kg, s, K); conversion to grains, inches etc. is a concern of the UI.
 */
@Getter
@Setter
@NoArgsConstructor
public class Rifle {

    @Id
    private UUID id;

    private String name;
    private UUID manufacturer;
    private UUID caliber;
    // meters
    private BigDecimal barrelLength;
    // meters per turn
    private BigDecimal lengthOfTwist;
}
