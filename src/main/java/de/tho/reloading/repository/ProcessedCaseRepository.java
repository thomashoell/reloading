package de.tho.reloading.repository;

import de.tho.reloading.entity.ProcessedCase;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProcessedCaseRepository extends OwnedRepository<ProcessedCase> {

    List<ProcessedCase> findByOwnerAndCasingLot(UUID owner, UUID casingLot);

    List<ProcessedCase> findByOwnerAndFiredFrom(UUID owner, UUID firedFrom);
}
