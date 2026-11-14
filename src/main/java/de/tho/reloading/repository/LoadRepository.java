package de.tho.reloading.repository;

import de.tho.reloading.entity.Load;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoadRepository extends CrudRepository<Load, UUID> {

    List<Load> findByProcessedCase(UUID processedCase);

    List<Load> findByBulletLot(UUID bulletLot);

    List<Load> findByPowderLot(UUID powderLot);

    List<Load> findByPrimerLot(UUID primerLot);

    List<Load> findByRifle(UUID rifle);
}
