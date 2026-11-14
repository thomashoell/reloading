package de.tho.reloading.repository;

import de.tho.reloading.entity.Caliber;
import de.tho.reloading.entity.Rifle;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RifleRepository extends CrudRepository<Rifle, UUID> {
    Optional<Rifle> findByName(String name);

    List<Rifle> findByManufacturer(UUID manufacturer);

    List<Rifle> findByCaliber(UUID caliber);
}
