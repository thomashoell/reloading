package de.tho.reloading.repository;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Master data (manufacturers, calibers, component products) is immutable: it is visible to every user and
 * only comes into the database with Liquibase changesets (db/changelog/changes/1xx-seed-*.yaml).
 * That's why this interface has no methods to save or delete.
 */
@NoRepositoryBean
public interface MasterDataRepository<T> extends Repository<T, UUID> {

    Optional<T> findById(UUID id);

    List<T> findAll();
}
