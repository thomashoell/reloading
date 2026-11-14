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
public class Powder {

    @Id
    @ReadOnlyProperty
    private UUID id;

    private String name;
    private UUID manufacturer;
    private String lotNumber;
}
