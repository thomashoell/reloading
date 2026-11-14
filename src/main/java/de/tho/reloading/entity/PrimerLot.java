package de.tho.reloading.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class PrimerLot {

    @Id
    private UUID id;

    // the app_user this row belongs to
    private UUID owner;

    private UUID primer;
    private String lotNumber;
}
