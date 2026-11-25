package de.tho.reloading.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** Lengths are in millimeters, not in SI units; the inch fields of the form are not sent. */
@Getter
@Setter
public class RifleForm {

    @NotBlank(message = "Bitte eine Bezeichnung angeben.")
    private String name;

    @NotNull(message = "Bitte einen Hersteller wählen.")
    private UUID manufacturer;

    @NotNull(message = "Bitte ein Kaliber wählen.")
    private UUID caliber;

    @Positive(message = "Die Lauflänge muss größer als 0 sein.")
    private BigDecimal barrelLength;

    @Positive(message = "Die Dralllänge muss größer als 0 sein.")
    private BigDecimal lengthOfTwist;
}
