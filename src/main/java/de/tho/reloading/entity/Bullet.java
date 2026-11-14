package de.tho.reloading.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A bullet product; purchased lots are {@link BulletLot}s.
 * Measures are stored in SI base units (m, kg, s, K); conversion to grains, inches etc. is a concern of the UI.
 */
@Getter
@Setter
@NoArgsConstructor
public class Bullet {

    @Id
    private UUID id;

    private UUID manufacturer;
    private String name;
    // meters
    private BigDecimal diameter;
    // kilograms
    private BigDecimal weight;
    // meters
    private BigDecimal length;
}
