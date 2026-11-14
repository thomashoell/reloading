package de.tho.reloading.web;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CasingForm {

    @NotNull(message = "Bitte einen Hersteller wählen.")
    private UUID manufacturer;

    @NotNull(message = "Bitte ein Kaliber wählen.")
    private UUID caliber;
}
