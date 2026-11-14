package de.tho.reloading.repository;

import de.tho.reloading.entity.PrimerLot;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrimerLotRepository extends CrudRepository<PrimerLot, UUID> {

    List<PrimerLot> findByPrimer(UUID primer);

    Optional<PrimerLot> findByPrimerAndLotNumber(UUID primer, String lotNumber);
}
