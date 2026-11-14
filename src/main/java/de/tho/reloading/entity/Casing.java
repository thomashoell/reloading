package de.tho.reloading.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.UUID;

/**
 * A case product; purchased lots are {@link CasingLot}s.
 */
@Getter
@Setter
@NoArgsConstructor
public class Casing {

    @Id
    private UUID id;

    private UUID manufacturer;
    private UUID caliber;
}
