package com.sigepes.backend.geo;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestrictedZoneRepository extends JpaRepository<RestrictedZoneEntity, String> {

    List<RestrictedZoneEntity> findAllByOrderByNameAsc();

    /** AC-04.5: prefiltro por caja envolvente (índice ix_restricted_zones_bbox). */
    @Query("""
            SELECT z FROM RestrictedZoneEntity z
            WHERE z.active = TRUE
              AND z.minLat <= :lat AND z.maxLat >= :lat
              AND z.minLon <= :lon AND z.maxLon >= :lon
            """)
    List<RestrictedZoneEntity> findActiveCandidates(@Param("lat") BigDecimal lat, @Param("lon") BigDecimal lon);
}
