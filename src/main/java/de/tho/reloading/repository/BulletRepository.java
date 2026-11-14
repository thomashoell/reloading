package de.tho.reloading.repository;

import de.tho.reloading.entity.Bullet;
import de.tho.reloading.entity.Rifle;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BulletRepository extends CrudRepository<Bullet, UUID> {

    List<Bullet> findByType(String type);

    List<Bullet> findByManufacturer(UUID manufacturer);

    List<Bullet> findByCaliber(UUID caliber);
}
