package de.tho.reloading.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A batch of cartridges loaded with the same components and the same data.
 * Measures are stored in SI base units (m, kg, s, K); conversion to grains, inches etc. is a concern of the UI.
 */
@Getter
@Setter
@NoArgsConstructor
public class Load {

    @Id
    private UUID id;

    private UUID processedCase;
    private UUID bulletLot;
    private UUID powderLot;
    private UUID primerLot;
    // kilograms
    private BigDecimal powderCharge;
    // cartridge overall length (COAL), meters
    private BigDecimal overallLength;
    // cartridge base to ogive (CBTO), meters
    private BigDecimal baseToOgive;
    private Integer quantity;
    private LocalDate loadedOn;
    // rifle the load is intended for
    private UUID rifle;
    private String notes;
}
