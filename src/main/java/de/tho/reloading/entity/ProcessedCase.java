package de.tho.reloading.entity;

import de.tho.reloading.utility.Calibration;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Table(name = "processed_case")
public class ProcessedCase {

    @Id
    @ReadOnlyProperty
    private UUID id;

    private UUID parentCase;

    @Column("is_fired")
    private Boolean fired;

    private UUID firedFrom;

    @Column("is_trimmed")
    private Boolean trimmed;

    // millimeters
    private Double caseLength;

    private Calibration calibrationMethod;
    private String calibrationDie;
    private String bushing;
    private Boolean mandrelUsed;

    // millimeters
    private Double mandrelDiameter;
}
