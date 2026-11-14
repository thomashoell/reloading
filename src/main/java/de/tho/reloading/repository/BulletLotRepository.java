package de.tho.reloading.repository;

import de.tho.reloading.entity.BulletLot;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BulletLotRepository extends OwnedRepository<BulletLot> {

    List<BulletLot> findByOwnerAndBullet(UUID owner, UUID bullet);

    Optional<BulletLot> findByOwnerAndBulletAndLotNumber(UUID owner, UUID bullet, String lotNumber);
}
