package de.tho.reloading.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CasingLot {

    @Id
    private UUID id;

    private UUID casing;
    private String lotNumber;
}
