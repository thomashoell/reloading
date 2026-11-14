package de.tho.reloading.web;

import de.tho.reloading.utility.PrimerSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PrimerForm {

    @NotNull(message = "Bitte einen Hersteller wählen.")
    private UUID manufacturer;

    @NotBlank(message = "Bitte eine Bezeichnung angeben.")
    private String name;

    @NotNull(message = "Bitte eine Größe wählen.")
    private PrimerSize size;
}
