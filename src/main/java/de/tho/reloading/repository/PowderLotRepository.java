package de.tho.reloading.repository;

import de.tho.reloading.entity.PowderLot;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PowderLotRepository extends OwnedRepository<PowderLot> {

    List<PowderLot> findByOwnerAndPowder(UUID owner, UUID powder);

    Optional<PowderLot> findByOwnerAndPowderAndLotNumber(UUID owner, UUID powder, String lotNumber);
}
