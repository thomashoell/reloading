package de.tho.reloading.repository;

import de.tho.reloading.entity.PrimerLot;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrimerLotRepository extends OwnedRepository<PrimerLot> {

    List<PrimerLot> findByOwnerAndPrimer(UUID owner, UUID primer);

    Optional<PrimerLot> findByOwnerAndPrimerAndLotNumber(UUID owner, UUID primer, String lotNumber);
}
