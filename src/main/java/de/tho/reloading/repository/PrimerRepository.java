package de.tho.reloading.repository;

import de.tho.reloading.entity.Primer;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrimerRepository extends MasterDataRepository<Primer> {

    List<Primer> findByName(String name);

    List<Primer> findByManufacturer(UUID manufacturer);
}
