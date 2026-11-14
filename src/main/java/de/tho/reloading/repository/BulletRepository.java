package de.tho.reloading.repository;

import de.tho.reloading.entity.Bullet;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface BulletRepository extends MasterDataRepository<Bullet> {

    List<Bullet> findByName(String name);

    List<Bullet> findByManufacturer(UUID manufacturer);

    List<Bullet> findByDiameter(BigDecimal diameter);
}
