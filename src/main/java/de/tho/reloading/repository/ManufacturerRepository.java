package de.tho.reloading.repository;

import de.tho.reloading.entity.Manufacturer;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ManufacturerRepository extends MasterDataRepository<Manufacturer> {
    Optional<Manufacturer> findByName(String name);
}
