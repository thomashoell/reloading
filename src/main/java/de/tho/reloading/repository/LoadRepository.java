package de.tho.reloading.repository;

import de.tho.reloading.entity.Load;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoadRepository extends OwnedRepository<Load> {

    /** One page of the loads of a user; loads keep growing, so lists do not fetch all of them. */
    List<Load> findByOwner(UUID owner, Pageable pageable);

    long countByOwner(UUID owner);

    List<Load> findByOwnerAndProcessedCase(UUID owner, UUID processedCase);

    List<Load> findByOwnerAndBulletLot(UUID owner, UUID bulletLot);

    List<Load> findByOwnerAndPowderLot(UUID owner, UUID powderLot);

    List<Load> findByOwnerAndPrimerLot(UUID owner, UUID primerLot);

    List<Load> findByOwnerAndRifle(UUID owner, UUID rifle);
}
