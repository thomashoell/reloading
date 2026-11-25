package de.tho.reloading.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Form of a lot of any component: the product it is a lot of, and the lot number. */
@Getter
@Setter
public class LotForm {

    @NotNull(message = "Bitte ein Produkt wählen.")
    private UUID product;

    @NotBlank(message = "Bitte eine Losnummer angeben.")
    private String lotNumber;
}
