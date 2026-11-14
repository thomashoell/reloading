package de.tho.reloading.repository;

import de.tho.reloading.entity.CasingLot;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CasingLotRepository extends OwnedRepository<CasingLot> {

    List<CasingLot> findByOwnerAndCasing(UUID owner, UUID casing);

    Optional<CasingLot> findByOwnerAndCasingAndLotNumber(UUID owner, UUID casing, String lotNumber);
}
