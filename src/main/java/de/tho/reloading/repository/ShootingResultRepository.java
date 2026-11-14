package de.tho.reloading.repository;

import de.tho.reloading.entity.ShootingResult;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShootingResultRepository extends OwnedRepository<ShootingResult> {

    List<ShootingResult> findByOwnerAndLoad(UUID owner, UUID load);

    List<ShootingResult> findByOwnerAndRifle(UUID owner, UUID rifle);
}
