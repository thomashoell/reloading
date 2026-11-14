package de.tho.reloading.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** Measures are in the units of the current user, not in SI units. */
@Getter
@Setter
public class BulletForm {

    @NotNull(message = "Bitte einen Hersteller wählen.")
    private UUID manufacturer;

    @NotBlank(message = "Bitte eine Bezeichnung angeben.")
    private String name;

    @NotNull(message = "Bitte den Durchmesser angeben.")
    @Positive(message = "Der Durchmesser muss größer als 0 sein.")
    private BigDecimal diameter;

    @NotNull(message = "Bitte das Gewicht angeben.")
    @Positive(message = "Das Gewicht muss größer als 0 sein.")
    private BigDecimal weight;

    @Positive(message = "Die Länge muss größer als 0 sein.")
    private BigDecimal length;
}
