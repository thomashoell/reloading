package de.tho.reloading.repository;

import de.tho.reloading.entity.Powder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PowderRepository extends MasterDataRepository<Powder> {

    List<Powder> findByName(String name);

    List<Powder> findByManufacturer(UUID manufacturer);
}
