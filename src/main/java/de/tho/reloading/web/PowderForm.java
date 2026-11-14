package de.tho.reloading.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PowderForm {

    @NotNull(message = "Bitte einen Hersteller wählen.")
    private UUID manufacturer;

    @NotBlank(message = "Bitte eine Bezeichnung angeben.")
    private String name;
}
