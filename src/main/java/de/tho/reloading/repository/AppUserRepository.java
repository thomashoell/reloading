package de.tho.reloading.repository;

import de.tho.reloading.entity.AppUser;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppUserRepository extends CrudRepository<AppUser, UUID> {

    Optional<AppUser> findBySubject(String subject);
}
