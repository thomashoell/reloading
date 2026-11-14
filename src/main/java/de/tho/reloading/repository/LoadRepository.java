package de.tho.reloading.repository;

import de.tho.reloading.entity.Load;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoadRepository extends OwnedRepository<Load> {

    List<Load> findByOwnerAndProcessedCase(UUID owner, UUID processedCase);

    List<Load> findByOwnerAndBulletLot(UUID owner, UUID bulletLot);

    List<Load> findByOwnerAndPowderLot(UUID owner, UUID powderLot);

    List<Load> findByOwnerAndPrimerLot(UUID owner, UUID primerLot);

    List<Load> findByOwnerAndRifle(UUID owner, UUID rifle);
}
