package de.tho.reloading.web;

import de.tho.reloading.utility.LengthUnit;
import de.tho.reloading.utility.WeightUnit;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingsForm {

    @NotNull(message = "Bitte eine Einheit wählen.")
    private WeightUnit weightUnit;

    @NotNull(message = "Bitte eine Einheit wählen.")
    private LengthUnit lengthUnit;
}
