package de.tho.reloading.repository;

import de.tho.reloading.entity.Rifle;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RifleRepository extends OwnedRepository<Rifle> {

    Optional<Rifle> findByOwnerAndName(UUID owner, String name);

    List<Rifle> findByOwnerAndManufacturer(UUID owner, UUID manufacturer);

    List<Rifle> findByOwnerAndCaliber(UUID owner, UUID caliber);
}
