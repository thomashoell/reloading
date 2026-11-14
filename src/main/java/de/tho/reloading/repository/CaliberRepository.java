package de.tho.reloading.repository;

import de.tho.reloading.entity.Caliber;
import de.tho.reloading.entity.Manufacturer;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaliberRepository extends CrudRepository<Caliber, UUID> {
    Optional<Caliber> findByName(String name);
}
