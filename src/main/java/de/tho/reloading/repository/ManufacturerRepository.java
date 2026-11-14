package de.tho.reloading.repository;

import de.tho.reloading.entity.Manufacturer;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ManufacturerRepository extends CrudRepository<Manufacturer, UUID> {
    Optional<Manufacturer> findByName(String name);
}
