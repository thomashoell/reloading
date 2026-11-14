package de.tho.reloading.web;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Form of master data that only has a name (manufacturers, calibers). */
@Getter
@Setter
public class NameForm {

    @NotBlank(message = "Bitte einen Namen angeben.")
    private String name;
}
