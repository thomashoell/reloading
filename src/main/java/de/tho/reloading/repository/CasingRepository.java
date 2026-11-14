package de.tho.reloading.repository;

import de.tho.reloading.entity.Casing;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CasingRepository extends CrudRepository<Casing, UUID> {

    List<Casing> findByManufacturer(UUID manufacturer);

    List<Casing> findByCaliber(UUID caliber);
}
