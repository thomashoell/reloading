package de.tho.reloading.web;

import de.tho.reloading.utility.Calibration;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Lengths are in millimeters, not in SI units; the inch fields of the form are not sent.
 * Rules that span several fields (rifle if fired, die if sized, diameter if a mandrel was used) are checked
 * in {@link ProcessedCaseController}.
 */
@Getter
@Setter
public class ProcessedCaseForm {

    @NotNull(message = "Bitte ein Hülsen-Los wählen.")
    private UUID casingLot;

    @NotNull(message = "Bitte die Anzahl angeben.")
    @Positive(message = "Die Anzahl muss größer als 0 sein.")
    private Integer quantity;

    @NotNull(message = "Bitte das Datum angeben.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate processedOn;

    @NotNull(message = "Bitte angeben, wie oft die Hülsen verschossen wurden.")
    @PositiveOrZero(message = "Die Schusszahl darf nicht negativ sein.")
    private Integer timesFired = 0;

    private UUID firedFrom;

    private boolean trimmed;

    @NotNull(message = "Bitte die Hülsenlänge angeben.")
    @Positive(message = "Die Hülsenlänge muss größer als 0 sein.")
    private BigDecimal caseLength;

    private Calibration calibrationMethod;

    private String calibrationDie;

    private String bushing;

    private boolean mandrelUsed;

    @Positive(message = "Der Durchmesser muss größer als 0 sein.")
    private BigDecimal mandrelDiameter;
}
