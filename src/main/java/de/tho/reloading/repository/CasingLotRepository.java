package de.tho.reloading.repository;

import de.tho.reloading.entity.CasingLot;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CasingLotRepository extends CrudRepository<CasingLot, UUID> {

    List<CasingLot> findByCasing(UUID casing);

    Optional<CasingLot> findByCasingAndLotNumber(UUID casing, String lotNumber);
}
