package de.tho.reloading.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Table(name = "bullet_casing")
public class Case {

    @Id
    @ReadOnlyProperty
    private UUID id;

    private UUID manufacturer;
    private UUID caliber;
    private String lotNumber;
}
