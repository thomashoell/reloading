package de.tho.reloading.repository;

import de.tho.reloading.entity.BulletLot;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BulletLotRepository extends CrudRepository<BulletLot, UUID> {

    List<BulletLot> findByBullet(UUID bullet);

    Optional<BulletLot> findByBulletAndLotNumber(UUID bullet, String lotNumber);
}
