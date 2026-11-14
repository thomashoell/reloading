package de.tho.reloading.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Bullet {

    @Id
    @ReadOnlyProperty
    private UUID id;

    private UUID manufacturer;
    private UUID caliber;
    private String lotNumber;
    private String type;
    private Double length;
    private Double weight;
}
