package de.tho.reloading.repository;

import de.tho.reloading.entity.PowderLot;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PowderLotRepository extends CrudRepository<PowderLot, UUID> {

    List<PowderLot> findByPowder(UUID powder);

    Optional<PowderLot> findByPowderAndLotNumber(UUID powder, String lotNumber);
}
