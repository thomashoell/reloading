package de.tho.reloading.entity;

import de.tho.reloading.utility.Calibration;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A batch of cases of one lot that were prepared the same way.
 * Measures are stored in SI base units (m, kg, s, K); conversion to grains, inches etc. is a concern of the UI.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProcessedCase {

    @Id
    private UUID id;

    // the app_user this row belongs to
    private UUID owner;

    private UUID casingLot;
    private Integer quantity;
    private LocalDate processedOn;
    // how often the cases of this batch have been fired so far
    private Integer timesFired;
    // rifle the cases were last fired from; required if timesFired > 0
    private UUID firedFrom;
    @Column("is_trimmed")
    private Boolean trimmed;
    // meters
    private BigDecimal caseLength;
    private Calibration calibrationMethod;
    private String calibrationDie;
    private String bushing;
    private Boolean mandrelUsed;
    // meters
    private BigDecimal mandrelDiameter;
}
