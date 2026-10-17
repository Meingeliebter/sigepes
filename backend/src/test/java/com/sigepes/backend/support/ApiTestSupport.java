package com.sigepes.backend.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import com.sigepes.backend.alerts.AlertSweeper;
import com.sigepes.backend.closures.ClosureEntity;
import com.sigepes.backend.closures.ClosureRepository;
import com.sigepes.backend.closures.ClosureStatus;
import com.sigepes.backend.closures.ClosureType;
import com.sigepes.backend.common.Ids;
import com.sigepes.backend.gears.FishingGearEntity;
import com.sigepes.backend.gears.FishingGearRepository;
import com.sigepes.backend.gears.GearStatus;
import com.sigepes.backend.permits.PermitEntity;
import com.sigepes.backend.permits.PermitRepository;
import com.sigepes.backend.permits.PermitStatus;
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
import com.sigepes.backend.common.AppClock;

/**
 * Base de las pruebas de API: contexto completo, MockMvc y utilidades para preparar datos.
 *
 * <p>Cada prueba corre dentro de una transacción que se revierte al terminar, así que las clases no
 * se contaminan entre sí.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class ApiTestSupport {

    public static final String PASSWORD = "Pesca#2026";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserService userService;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected FishingGearRepository gearRepository;

    @Autowired
    protected VesselRepository vesselRepository;

    @Autowired
    protected SpeciesRepository speciesRepository;

    @Autowired
    protected ClosureRepository closureRepository;

    @Autowired
    protected PermitRepository permitRepository;

    /** Las alertas por calendario no corren solas en pruebas (sweep-enabled=false): se invocan. */
    @Autowired
    protected AlertSweeper alertSweeper;

    protected UserEntity givenUser(String nationalId, String email, Role role) {
        return userService.create(nationalId, "Usuario " + role, email, role, PASSWORD, false);
    }

    protected FishingGearEntity givenGear(String name, boolean destructive) {
        return gearRepository.save(new FishingGearEntity(Ids.generate("art"), name, null, destructive,
                destructive ? GearStatus.BLOCKED : GearStatus.ACTIVE));
    }

    protected VesselEntity givenVessel(UserEntity owner, String registration, VesselStatus status,
            FishingGearEntity... gears) {
        VesselEntity vessel = new VesselEntity(Ids.generate("emb"), "Embarcación " + registration, registration,
                "Bahía Solano", owner);
        vessel.setStatus(status);
        vessel.setGears(new LinkedHashSet<>(Set.of(gears)));
        vessel.setHullType("Fibra de vidrio");
        vessel.setEnginePower(new BigDecimal("75.00"));
        vessel.setCapacityTons(new BigDecimal("5.00"));
        return vesselRepository.save(vessel);
    }

    protected SpeciesEntity givenSpecies(String commonName, BigDecimal minSizeCm) {
        return speciesRepository.save(new SpeciesEntity(Ids.generate("esp"), commonName,
                "Scientificus " + commonName.toLowerCase(), minSizeCm, SpeciesStatus.ACTIVE,
                MeasurementMethod.TOTAL_LENGTH, SpeciesRegion.AMBAS, "https://placehold.co/400x300?text=Especie"));
    }

    protected PermitEntity givenPermit(UserEntity owner, String number, PermitStatus status, LocalDate issued,
            LocalDate expires) {
        return givenPermit(owner, number, status, issued, expires, "Pesca artesanal");
    }

    protected PermitEntity givenPermit(UserEntity owner, String number, PermitStatus status, LocalDate issued,
            LocalDate expires, String type) {
        return permitRepository.save(new PermitEntity(Ids.generate("per"), number, type, owner.getFullName(), owner,
                issued, expires, status, "SIGEPES-" + number + "-TEST"));
    }

    protected ClosureEntity givenClosure(SpeciesEntity species, String zone, LocalDate from, LocalDate to) {
        return givenClosure(species, zone, from, to, ClosureType.TOTAL);
    }

    protected ClosureEntity givenClosure(SpeciesEntity species, String zone, LocalDate from, LocalDate to,
            ClosureType closureType) {
        ClosureEntity closure = new ClosureEntity(Ids.generate("ved"), species, zone, from, to,
                ClosureStatus.SCHEDULED, "AUNAP-TEST-0001", closureType);
        closure.setStatus(closure.effectiveStatus(AppClock.today()));
        return closureRepository.save(closure);
    }

    // ------------------------------------------------------------------
    // Bitácora vía API (la reutilizan las pruebas de cuotas, geolocalización y trazabilidad)
    // ------------------------------------------------------------------

    protected ResultActions apiStartTrip(String token, String vesselId, String zone) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/faenas").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"vesselId":"%s","zone":"%s","originPort":"Bahía Solano"}""".formatted(vesselId, zone)));
    }

    protected String apiStartedTripId(String token, String vesselId, String zone) throws Exception {
        String response = apiStartTrip(token, vesselId, zone)
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return extract(response, "id");
    }

    protected ResultActions apiAddCatch(String token, String tripId, String speciesId, String size, String weight)
            throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/faenas/" + tripId + "/capturas")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"speciesId":"%s","quantity":5,"weightKg":%s,"averageSizeCm":%s}"""
                        .formatted(speciesId, weight, size)));
    }

    protected ResultActions apiCloseTrip(String token, String tripId) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/faenas/" + tripId + "/cerrar")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nullDeclaration":false,"landingLatitude":6.23,"landingLongitude":-77.4}"""));
    }

    /** Inicia sesión con el endpoint real y devuelve la cabecera Authorization. */
    protected String bearerFor(String email) throws Exception {
        String body = """
                {"email":"%s","password":"%s"}""".formatted(email, PASSWORD);
        String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/auth/login")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + extract(response, "accessToken");
    }

    /** Extracción mínima de un campo string de un JSON plano, para no depender de un parser. */
    protected static String extract(String json, String field) {
        String marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            throw new IllegalArgumentException("No se encontró el campo " + field + " en: " + json);
        }
        start += marker.length();
        return json.substring(start, json.indexOf('"', start));
    }
}
