package de.tho.reloading.repository;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * User data belongs to one user (owner) and is only visible to that user. Unlike CrudRepository this
 * interface has no finders without an owner, so data of other users can't be read by accident.
 * Before changing an existing row, load it with {@link #findByIdAndOwner(UUID, UUID)}.
 */
@NoRepositoryBean
public interface OwnedRepository<T> extends Repository<T, UUID> {

    <S extends T> S save(S entity);

    Optional<T> findByIdAndOwner(UUID id, UUID owner);

    List<T> findByOwner(UUID owner);

    /** Only for an entity that was loaded with {@link #findByIdAndOwner(UUID, UUID)}. */
    void delete(T entity);
}
