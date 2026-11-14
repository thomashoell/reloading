package de.tho.reloading.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.UUID;

/**
 * Master data (manufacturers, calibers, component products) is visible to every user,
 * but only administrators may create, change or delete it.
 */
@NoRepositoryBean
public interface MasterDataRepository<T> extends CrudRepository<T, UUID> {

    String ADMIN_ONLY = "@currentUser.isAdmin()";

    @Override
    @PreAuthorize(ADMIN_ONLY)
    <S extends T> S save(S entity);

    @Override
    @PreAuthorize(ADMIN_ONLY)
    <S extends T> Iterable<S> saveAll(Iterable<S> entities);

    @Override
    @PreAuthorize(ADMIN_ONLY)
    void deleteById(UUID id);

    @Override
    @PreAuthorize(ADMIN_ONLY)
    void delete(T entity);

    @Override
    @PreAuthorize(ADMIN_ONLY)
    void deleteAllById(Iterable<? extends UUID> ids);

    @Override
    @PreAuthorize(ADMIN_ONLY)
    void deleteAll(Iterable<? extends T> entities);

    @Override
    @PreAuthorize(ADMIN_ONLY)
    void deleteAll();
}
