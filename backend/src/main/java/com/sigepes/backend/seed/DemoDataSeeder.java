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
import com.sigepes.backend.closures.ClosureType;
import com.sigepes.backend.gears.FishingGearEntity;
import com.sigepes.backend.gears.FishingGearRepository;
import com.sigepes.backend.gears.GearStatus;
import com.sigepes.backend.permits.PermitEntity;
import com.sigepes.backend.permits.PermitRepository;
import com.sigepes.backend.permits.PermitStatus;
import com.sigepes.backend.geo.Polygon;
import com.sigepes.backend.geo.RestrictedZoneEntity;
import com.sigepes.backend.geo.RestrictedZoneRepository;
import com.sigepes.backend.geo.ZoneType;
import com.sigepes.backend.quotas.QuotaEntity;
import com.sigepes.backend.quotas.QuotaRepository;
import com.sigepes.backend.reports.IllegalReportEntity;
import com.sigepes.backend.reports.IllegalReportRepository;
import com.sigepes.backend.reports.ReportStatus;
import com.sigepes.backend.reports.ReportType;
import com.sigepes.backend.species.MeasurementMethod;
import com.sigepes.backend.species.SpeciesEntity;
import com.sigepes.backend.species.SpeciesRegion;
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
    private final QuotaRepository quotaRepository;
    private final RestrictedZoneRepository zoneRepository;
    private final IllegalReportRepository reportRepository;

    public DemoDataSeeder(
            @org.springframework.beans.factory.annotation.Value("${sigepes.seed.enabled:true}") boolean enabled,
            UserRepository userRepository, UserService userService, FishingGearRepository gearRepository,
            VesselRepository vesselRepository, SpeciesRepository speciesRepository,
            ClosureRepository closureRepository, PermitRepository permitRepository, QuotaRepository quotaRepository,
            RestrictedZoneRepository zoneRepository, IllegalReportRepository reportRepository) {
        this.enabled = enabled;
        this.userRepository = userRepository;
        this.userService = userService;
        this.gearRepository = gearRepository;
        this.vesselRepository = vesselRepository;
        this.speciesRepository = speciesRepository;
        this.closureRepository = closureRepository;
        this.permitRepository = permitRepository;
        this.quotaRepository = quotaRepository;
        this.zoneRepository = zoneRepository;
        this.reportRepository = reportRepository;
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
                Set.of(handline, trap), "Lancha artesanal", "Fibra de vidrio", new BigDecimal("75.00"),
                new BigDecimal("3.50"));
        vessel("DEMO Estrella del Mar", "DIMAR-CO-11875", "Buenaventura", fisher, VesselStatus.UNDER_MAINTENANCE,
                Set.of(longline), "Lancha artesanal", "Madera", new BigDecimal("60.00"), new BigDecimal("4.20"));
        vessel("DEMO Mar Abierto", "DIMAR-CO-20044", "Tumaco", shipowner, VesselStatus.ACTIVE, Set.of(longline),
                "Embarcación industrial", "Acero", new BigDecimal("450.00"), new BigDecimal("42.00"));

        // --- Especies y tallas mínimas -----------------------------------
        SpeciesEntity snapper = species("Pargo rojo", "Lutjanus purpureus", new BigDecimal("35.00"),
                MeasurementMethod.TOTAL_LENGTH, SpeciesRegion.CARIBE);
        SpeciesEntity shrimp = species("Camarón blanco", "Litopenaeus vannamei", new BigDecimal("12.00"),
                MeasurementMethod.TOTAL_LENGTH, SpeciesRegion.PACIFICO);
        SpeciesEntity tuna = species("Atún aleta amarilla", "Thunnus albacares", new BigDecimal("50.00"),
                MeasurementMethod.FORK_LENGTH, SpeciesRegion.AMBAS);
        SpeciesEntity sierra = species("Sierra", "Scomberomorus sierra", new BigDecimal("40.00"),
                MeasurementMethod.TOTAL_LENGTH, SpeciesRegion.PACIFICO);
        species("Corvina", "Cynoscion phoxocephalus", new BigDecimal("30.00"),
                MeasurementMethod.TOTAL_LENGTH, SpeciesRegion.PACIFICO);

        // --- Vedas: una vigente hoy (total) y otra programada (parcial) --
        LocalDate today = LocalDate.now();
        closure(shrimp, "Pacífico Norte", today.minusDays(10), today.plusDays(45),
                "Periodo reproductivo del camarón blanco.", inspector, "AUNAP-0456-2025", ClosureType.TOTAL);
        closure(snapper, "Caribe", today.plusMonths(3), today.plusMonths(4).plusDays(15),
                "Veda preventiva programada para el pargo rojo; se exceptúa la pesca de subsistencia.", inspector,
                "AUNAP-0512-2025", ClosureType.PARTIAL);

        // --- Cuotas (RF-07): una holgada, una cerca del límite y una por embarcación
        quota(snapper, "Caribe", today.withDayOfYear(1), today.withDayOfYear(today.lengthOfYear()),
                new BigDecimal("2000.00"), BigDecimal.ZERO, null, "AUNAP-0610-2025");
        quota(tuna, "TODAS", today.withDayOfYear(1), today.withDayOfYear(today.lengthOfYear()),
                new BigDecimal("5000.00"), new BigDecimal("4120.00"), null, "AUNAP-0611-2025");
        quota(sierra, "Pacífico Norte", today.withDayOfMonth(1), today.withDayOfMonth(1).plusMonths(3).minusDays(1),
                new BigDecimal("800.00"), new BigDecimal("150.00"),
                vesselRepository.findById(prefixedId("emb", "DIMAR-CO-20044")).orElse(null), "AUNAP-0612-2025");

        // --- Zonas restringidas (RF-04): delimitaciones aproximadas, solo para demostración
        zone("DEMO Veda camarón · Pacífico Norte", ZoneType.CLOSURE_ZONE, List.of(
                new double[] { 6.05, -77.62 }, new double[] { 6.45, -77.62 }, new double[] { 6.45, -77.44 },
                new double[] { 6.05, -77.44 }), "AUNAP-0456-2025",
                "Franja costera frente a Bahía Solano cubierta por la veda del camarón blanco (delimitación de demostración).");
        zone("DEMO Área Marina Protegida · Ensenada de Utría", ZoneType.MPA, List.of(
                new double[] { 5.93, -77.40 }, new double[] { 6.08, -77.40 }, new double[] { 6.08, -77.29 },
                new double[] { 5.98, -77.27 }, new double[] { 5.93, -77.30 }), "MADS-PNN-UTRIA-DEMO",
                "Polígono aproximado de un área protegida para pruebas; no corresponde a la cartografía oficial.");

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

        // --- Denuncias (RF-10): una anónima recibida y una en investigación
        report("DEN-DEMO2345", ReportType.MPA_INTRUSION, today.minusDays(2), 6.01, -77.34,
                "Lancha sin nombre visible calando red dentro de la ensenada de Utría al amanecer.",
                ReportStatus.RECEIVED);
        report("DEN-DEMO6789", ReportType.UNDERSIZED_CATCH, today.minusDays(6), null, null,
                "En el muelle de Bahía Solano se ofrece pargo rojo muy por debajo de la talla mínima.",
                ReportStatus.INVESTIGATING);

        log.info("Datos DEMO cargados. Usuarios de prueba con contraseña «{}».", DEMO_PASSWORD);
    }

    private void report(String code, ReportType type, LocalDate occurredOn, Double latitude, Double longitude,
            String description, ReportStatus status) {
        IllegalReportEntity report = new IllegalReportEntity(prefixedId("den", code), code, type, description,
                occurredOn, null);
        if (latitude != null) {
            report.setLocation(BigDecimal.valueOf(latitude), BigDecimal.valueOf(longitude));
        } else {
            report.setLocationText("Muelle principal de Bahía Solano");
        }
        report.setStatus(status);
        report.setDemo(true);
        reportRepository.save(report);
    }

    private void zone(String name, ZoneType type, List<double[]> vertices, String resolution, String description) {
        RestrictedZoneEntity zone = new RestrictedZoneEntity(prefixedId("zon", name), name, type,
                Polygon.validated(vertices), resolution);
        zone.setDescription(description);
        zone.setDemo(true);
        zoneRepository.save(zone);
    }

    private void quota(SpeciesEntity species, String zone, LocalDate from, LocalDate to, BigDecimal totalKg,
            BigDecimal consumedKg, VesselEntity vessel, String resolution) {
        QuotaEntity quota = new QuotaEntity(prefixedId("cuo", species.getCommonName() + zone), species, zone, from, to,
                totalKg);
        quota.consume(consumedKg);
        quota.setVessel(vessel);
        quota.setResolutionNumber(resolution);
        quota.setLastThreshold(quota.consumedPercent().compareTo(new BigDecimal("80")) >= 0 ? 80 : 0);
        quota.setDemo(true);
        quotaRepository.save(quota);
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
            Set<FishingGearEntity> gears, String type, String hullType, BigDecimal enginePower, BigDecimal capacity) {
        VesselEntity vessel = new VesselEntity(prefixedId("emb", registration), name, registration, port, owner);
        vessel.setStatus(status);
        vessel.setGears(new LinkedHashSet<>(gears));
        vessel.setVesselType(type);
        vessel.setHullType(hullType);
        vessel.setEnginePower(enginePower);
        vessel.setCapacityTons(capacity);
        vessel.setDemo(true);
        return vesselRepository.save(vessel);
    }

    private SpeciesEntity species(String commonName, String scientificName, BigDecimal minSizeCm,
            MeasurementMethod measurementMethod, SpeciesRegion region) {
        SpeciesEntity species = new SpeciesEntity(prefixedId("esp", commonName), commonName, scientificName,
                minSizeCm, SpeciesStatus.ACTIVE, measurementMethod, region,
                "https://placehold.co/400x300?text=" + commonName.replace(" ", "+"));
        species.setDemo(true);
        return speciesRepository.save(species);
    }

    private ClosureEntity closure(SpeciesEntity species, String zone, LocalDate from, LocalDate to, String reason,
            UserEntity createdBy, String resolutionNumber, ClosureType closureType) {
        ClosureEntity closure = new ClosureEntity(prefixedId("ved", species.getCommonName() + zone), species, zone,
                from, to, ClosureStatus.SCHEDULED, resolutionNumber, closureType);
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
