package de.tho.reloading.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Not in SI units: lengths are in millimeters (the inch fields of the form are not sent),
 * the powder charge is in the weight unit of the user.
 */
@Getter
@Setter
public class LoadForm {

    @NotNull(message = "Bitte eine Hülsencharge wählen.")
    private UUID processedCase;

    @NotNull(message = "Bitte ein Geschoss-Los wählen.")
    private UUID bulletLot;

    @NotNull(message = "Bitte ein Pulver-Los wählen.")
    private UUID powderLot;

    @NotNull(message = "Bitte ein Zündhütchen-Los wählen.")
    private UUID primerLot;

    @NotNull(message = "Bitte die Pulverladung angeben.")
    @Positive(message = "Die Pulverladung muss größer als 0 sein.")
    private BigDecimal powderCharge;

    @NotNull(message = "Bitte die Patronenlänge angeben.")
    @Positive(message = "Die Patronenlänge muss größer als 0 sein.")
    private BigDecimal overallLength;

    @Positive(message = "Das Maß muss größer als 0 sein.")
    private BigDecimal baseToOgive;

    @NotNull(message = "Bitte die Anzahl angeben.")
    @Positive(message = "Die Anzahl muss größer als 0 sein.")
    private Integer quantity;

    @NotNull(message = "Bitte das Datum angeben.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate loadedOn;

    private UUID rifle;

    @Size(max = 2000, message = "Die Notiz darf höchstens 2000 Zeichen lang sein.")
    private String notes;
}
