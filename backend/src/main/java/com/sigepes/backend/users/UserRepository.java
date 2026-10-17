package com.sigepes.backend.users;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, String> {

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    Optional<UserEntity> findByNationalId(String nationalId);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByNationalId(String nationalId);

    List<UserEntity> findAllByOrderByFullNameAsc();

    /** Destinatarios de alertas por rol (RF-11). */
    List<UserEntity> findByRoleInAndStatus(Collection<Role> roles, UserStatus status);
}
