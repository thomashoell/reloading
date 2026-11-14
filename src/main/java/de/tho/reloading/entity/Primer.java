package de.tho.reloading.entity;

import de.tho.reloading.utility.PrimerSize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.UUID;

/**
 * A primer product; purchased lots are {@link PrimerLot}s.
 */
@Getter
@Setter
@NoArgsConstructor
public class Primer {

    @Id
    private UUID id;

    private UUID manufacturer;
    private String name;
    private PrimerSize size;
}
