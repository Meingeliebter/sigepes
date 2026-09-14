package com.sigepes.backend.vessels;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VesselRepository extends JpaRepository<VesselEntity, String> {

    boolean existsByRegistrationIgnoreCase(String registration);

    List<VesselEntity> findByOwnerIdOrderByNameAsc(String ownerId);

    List<VesselEntity> findAllByOrderByNameAsc();

    long countByOwnerId(String ownerId);

    long countByOwnerIdAndStatus(String ownerId, VesselStatus status);
}
