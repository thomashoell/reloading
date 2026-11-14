package de.tho.reloading.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.UUID;

/**
 * A powder product; purchased lots are {@link PowderLot}s.
 */
@Getter
@Setter
@NoArgsConstructor
public class Powder {

    @Id
    private UUID id;

    private UUID manufacturer;
    private String name;
}
