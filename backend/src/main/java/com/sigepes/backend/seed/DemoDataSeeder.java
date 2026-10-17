package com.sigepes.backend.seed;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sigepes.backend.closures.ClosureEntity;
import com.sigepes.backend.closures.ClosureRepository;
import com.sigepes.backend.closures.ClosureStatus;
import com.sigepes.backend.gears.FishingGearEntity;
import com.sigepes.backend.gears.FishingGearRepository;
import com.sigepes.backend.gears.GearStatus;
import com.sigepes.backend.permits.PermitEntity;
import com.sigepes.backend.permits.PermitRepository;
import com.sigepes.backend.permits.PermitStatus;
import com.sigepes.backend.species.MeasurementMethod;
import com.sigepes.backend.species.SpeciesRegion;
import com.sigepes.backend.species.SpeciesEntity;
import com.sigepes.backend.species.SpeciesRepository;
import com.sigepes.backend.species.SpeciesStatus;
import com.sigepes.backend.users.Role;
import com.sigepes.backend.users.UserEntity;
import com.sigepes.backend.users.UserRepository;
import com.sigepes.backend.users.UserService;
import com.sigepes.backend.vessels.VesselEntity;
import com.sigepes.backend.vessels.VesselRepository;
import com.sigepes.backend.vessels.VesselStatus;

/**
 * Datos de demostración del MVP.
 *
 * <p>Se ejecuta solo si {@code sigepes.seed.enabled=true} y únicamente si la base está vacía de
 * usuarios, así que es seguro reiniciar. Todo lo que crea queda marcado con {@code demo = true}.
 */
@Component
@Profile("!test")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    /** Contraseña común de las cuentas de demostración. */
    public static final String DEMO_PASSWORD = "Pesca#2026";

    private final boolean enabled;
    private final UserRepository userRepository;
    private final UserService userService;
    private final FishingGearRepository gearRepository;
    private final VesselRepository vesselRepository;
    private final SpeciesRepository speciesRepository;
    private final ClosureRepository closureRepository;
    private final PermitRepository permitRepository;

    public DemoDataSeeder(
            @org.springframework.beans.factory.annotation.Value("${sigepes.seed.enabled:true}") boolean enabled,
            UserRepository userRepository, UserService userService, FishingGearRepository gearRepository,
            VesselRepository vesselRepository, SpeciesRepository speciesRepository,
            ClosureRepository closureRepository, PermitRepository permitRepository) {
        this.enabled = enabled;
        this.userRepository = userRepository;
        this.userService = userService;
        this.gearRepository = gearRepository;
        this.vesselRepository = vesselRepository;
        this.speciesRepository = speciesRepository;
        this.closureRepository = closureRepository;
        this.permitRepository = permitRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Carga de datos DEMO desactivada (sigepes.seed.enabled=false).");
            return;
        }
        if (userRepository.count() > 0) {
            log.info("La base ya tiene datos; se omite la carga DEMO.");
            return;
        }

        log.info("Cargando datos DEMO de SIGEPES...");

        // --- Usuarios: uno por rol principal ------------------------------
        UserEntity fisher = demoUser("1001001001", "DEMO Pescador Aníbal", "pescador@sigepes.gov.co", Role.FISHER);
        UserEntity shipowner = demoUser("1004004004", "DEMO Armador Rodrigo", "armador@sigepes.gov.co",
                Role.SHIPOWNER);
        UserEntity inspector = demoUser("1002002002", "DEMO Inspectora Carolina", "inspector@sigepes.gov.co",
                Role.INSPECTOR);
        demoUser("1005005005", "DEMO Analista Julián", "analista@sigepes.gov.co", Role.ANALYST);
        demoUser("1003003003", "DEMO Administrador", "admin@sigepes.gov.co", Role.ADMIN);

        // --- Artes: permitidas y una prohibida para probar RN-04 ----------
        FishingGearEntity handline = gear("Línea de mano", "Arte selectiva de anzuelo, propia de la pesca artesanal.",
                false, GearStatus.ACTIVE);
        FishingGearEntity trap = gear("Nasa", "Trampa fija para crustáceos y peces de fondo.", false,
                GearStatus.ACTIVE);
        FishingGearEntity longline = gear("Palangre", "Línea madre con anzuelos; requiere control de captura.", false,
                GearStatus.ACTIVE);
        gear("Pesca con dinamita", "Arte destructiva prohibida por la Ley 13 de 1990.", true, GearStatus.BLOCKED);
        gear("Red de arrastre de fondo", "Arte de alto impacto sobre el fondo marino; prohibida.", true,
                GearStatus.BLOCKED);

        // --- Embarcaciones ------------------------------------------------
        vessel("DEMO La Sirena", "DIMAR-CO-10231", "Bahía Solano", fisher, VesselStatus.ACTIVE,
                Set.of(handline, trap), "Lancha artesanal", new BigDecimal("3.50"));
        vessel("DEMO Estrella del Mar", "DIMAR-CO-11875", "Buenaventura", fisher, VesselStatus.UNDER_MAINTENANCE,
                Set.of(longline), "Lancha artesanal", new BigDecimal("4.20"));
        vessel("DEMO Mar Abierto", "DIMAR-CO-20044", "Tumaco", shipowner, VesselStatus.ACTIVE, Set.of(longline),
                "Embarcación industrial", new BigDecimal("42.00"));

        // --- Especies y tallas mínimas -----------------------------------
        SpeciesEntity snapper = species("Pargo rojo", "Lutjanus purpureus", new BigDecimal("35.00"));
        SpeciesEntity shrimp = species("Camarón blanco", "Litopenaeus vannamei", new BigDecimal("12.00"));
        species("Atún aleta amarilla", "Thunnus albacares", new BigDecimal("50.00"));
        species("Sierra", "Scomberomorus sierra", new BigDecimal("40.00"));
        species("Corvina", "Cynoscion phoxocephalus", new BigDecimal("30.00"));

        // --- Vedas: una vigente hoy y otra programada --------------------
        LocalDate today = LocalDate.now();
        closure(shrimp, "Pacífico Norte", today.minusDays(10), today.plusDays(45),
                "Periodo reproductivo del camarón blanco.", inspector);
        closure(snapper, "Caribe", today.plusMonths(3), today.plusMonths(4).plusDays(15),
                "Veda preventiva programada para el pargo rojo.", inspector);

        // --- Permisos: vigente, vencido y pendiente ----------------------
        permit("PES-0231", "Pesca artesanal", fisher, today.minusMonths(6), today.plusMonths(6),
                PermitStatus.APPROVED, null);
        permit("PES-0198", "Pesca artesanal", fisher, today.minusYears(2), today.minusMonths(2),
                PermitStatus.APPROVED, null);
        permit("PES-0244", "Pesca artesanal", fisher, today, today.plusYears(1), PermitStatus.PENDING, null);
        permit("PES-0301", "Pesca industrial", shipowner, today.minusMonths(2), today.plusDays(20),
                PermitStatus.APPROVED, null);
        permit("PES-0177", "Pesca artesanal", shipowner, today.minusYears(1), today.plusMonths(3),
                PermitStatus.SUSPENDED, "Incumplimiento reiterado del reporte de desembarco.");

        log.info("Datos DEMO cargados. Usuarios de prueba con contraseña «{}».", DEMO_PASSWORD);
    }

    private UserEntity demoUser(String nationalId, String fullName, String email, Role role) {
        UserEntity user = userService.create(nationalId, fullName, email, role, DEMO_PASSWORD, true);
        return userRepository.save(user);
    }

    private FishingGearEntity gear(String name, String description, boolean destructive, GearStatus status) {
        FishingGearEntity gear = new FishingGearEntity(prefixedId("art", name), name, description, destructive,
                status);
        gear.setDemo(true);
        return gearRepository.save(gear);
    }

    private VesselEntity vessel(String name, String registration, String port, UserEntity owner, VesselStatus status,
            Set<FishingGearEntity> gears, String type, BigDecimal capacity) {
        VesselEntity vessel = new VesselEntity(prefixedId("emb", registration), name, registration, port, owner);
        vessel.setStatus(status);
        vessel.setGears(new LinkedHashSet<>(gears));
        vessel.setVesselType(type);
        vessel.setCapacityTons(capacity);
        vessel.setDemo(true);
        return vesselRepository.save(vessel);
    }

    private SpeciesEntity species(String commonName, String scientificName, BigDecimal minSizeCm) {
        SpeciesEntity species = new SpeciesEntity(prefixedId("esp", commonName), commonName, scientificName,
                minSizeCm, SpeciesStatus.ACTIVE, MeasurementMethod.TOTAL_LENGTH, SpeciesRegion.AMBAS,
                "https://placehold.co/400x300?text=" + commonName.replace(" ", "+"));
        species.setDemo(true);
        return speciesRepository.save(species);
    }

    private ClosureEntity closure(SpeciesEntity species, String zone, LocalDate from, LocalDate to, String reason,
            UserEntity createdBy) {
        ClosureEntity closure = new ClosureEntity(prefixedId("ved", species.getCommonName() + zone), species, zone,
                from, to, ClosureStatus.SCHEDULED);
        closure.setStatus(closure.effectiveStatus(LocalDate.now()));
        closure.setReason(reason);
        closure.setNotes("Registro DEMO de veda.");
        closure.setCreatedBy(createdBy);
        closure.setDemo(true);
        return closureRepository.save(closure);
    }

    private PermitEntity permit(String number, String type, UserEntity owner, LocalDate issued, LocalDate expires,
            PermitStatus status, String reason) {
        PermitEntity permit = new PermitEntity(prefixedId("per", number), number, type, owner.getFullName(), owner,
                issued, expires, status, "SIGEPES-" + number.replace("-", "") + "-DEMO");
        permit.setReason(reason);
        permit.setDemo(true);
        return permitRepository.save(permit);
    }

    /** Identificadores estables y legibles para los datos DEMO. */
    private static String prefixedId(String prefix, String seed) {
        String slug = java.text.Normalizer.normalize(seed.toLowerCase(java.util.Locale.ROOT),
                java.text.Normalizer.Form.NFD)
                .replaceAll("[^a-z0-9]", "");
        return prefix + "-demo-" + slug.substring(0, Math.min(slug.length(), 16));
    }

    /** Correos de las cuentas de demostración, para la documentación y las pruebas. */
    public static List<String> demoEmails() {
        return List.of("pescador@sigepes.gov.co", "armador@sigepes.gov.co", "inspector@sigepes.gov.co",
                "analista@sigepes.gov.co", "admin@sigepes.gov.co");
    }
}
