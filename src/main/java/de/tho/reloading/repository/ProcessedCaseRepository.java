package de.tho.reloading.repository;

import de.tho.reloading.entity.ProcessedCase;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProcessedCaseRepository extends CrudRepository<ProcessedCase, UUID> {

    List<ProcessedCase> findByCasingLot(UUID casingLot);

    List<ProcessedCase> findByFiredFrom(UUID firedFrom);
}
