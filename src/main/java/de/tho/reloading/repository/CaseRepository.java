package de.tho.reloading.repository;

import de.tho.reloading.entity.Case;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaseRepository extends CrudRepository<Case, UUID> {

    List<Case> findByManufacturer(UUID manufacturer);
    List<Case> findByCaliber(UUID caliber);
    Optional<Case> findByLotNumber(String lotNumber);
}
