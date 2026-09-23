package com.sigepes.backend.trips;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.sigepes.backend.common.Ids;
import com.sigepes.backend.gears.FishingGearRepository;
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

/**
 * Recorre el ciclo completo de una faena <b>sin</b> transacción de test envolvente.
 *
 * <p>El resto de pruebas hereda de {@code ApiTestSupport}, que es {@code @Transactional}: esa sesión
 * abierta durante todo el test disimula los accesos perezosos fuera de transacción. Aquí cada
 * petición vive y muere como en producción ({@code spring.jpa.open-in-view=false}), de modo que un
 * {@code LazyInitializationException} al serializar la respuesta haría fallar la prueba.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TripWithoutAmbientTransactionTest {

    private static final String PASSWORD = "Pesca#2026";
    private static final String ZONE = "Pacífico Norte";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VesselRepository vesselRepository;
    @Autowired
    private SpeciesRepository speciesRepository;
    @Autowired
    private PermitRepository permitRepository;
    @Autowired
    private FishingGearRepository gearRepository;
    @Autowired
    private FishingTripRepository tripRepository;

    private UserEntity fisher;
    private VesselEntity vessel;
    private SpeciesEntity species;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        fisher = userService.create("9090001122", "Sin Transaccion", "sin.tx@sigepes.gov.co", Role.FISHER, PASSWORD,
                false);
        VesselEntity newVessel = new VesselEntity(Ids.generate("emb"), "Sin Tx", "DIMAR-NOTX-1", "Bahía Solano",
                fisher);
        newVessel.setHullType("Fibra de vidrio");
        newVessel.setEnginePower(new BigDecimal("75.00"));
        newVessel.setCapacityTons(new BigDecimal("5.00"));
        vessel = vesselRepository.save(newVessel);
        species = speciesRepository.save(new SpeciesEntity(Ids.generate("esp"), "Pargo sin tx", "Lutjanus notx",
                new BigDecimal("35.00"), SpeciesStatus.ACTIVE, MeasurementMethod.TOTAL_LENGTH, SpeciesRegion.AMBAS,
                "https://placehold.co/400x300?text=Especie"));
        permitRepository.save(new PermitEntity(Ids.generate("per"), "PES-NOTX-1", "Pesca artesanal",
                fisher.getFullName(), fisher, LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(6),
                PermitStatus.APPROVED, "SIGEPES-NOTX-1"));

        String login = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"sin.tx@sigepes.gov.co","password":"%s"}""".formatted(PASSWORD)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        token = "Bearer " + login.replaceAll(".*\"accessToken\":\"([^\"]*)\".*", "$1");
    }

    @AfterEach
    void tearDown() {
        tripRepository.deleteAll();
        permitRepository.deleteAll();
        vesselRepository.deleteAll();
        speciesRepository.deleteAll();
        gearRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Faena completa fuera de transacción: abrir, capturar, cerrar y listar")
    void fullTripLifecycleOutsideTransaction() throws Exception {
        String created = mockMvc.perform(post("/api/v1/faenas").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"vesselId":"%s","zone":"%s"}""".formatted(vessel.getId(), ZONE)))
                .andExpect(status().isCreated())
                // Serializa el permiso, que es una asociación perezosa: aquí saltaría el fallo.
                .andExpect(jsonPath("$.permitNumber").value("PES-NOTX-1"))
                .andExpect(jsonPath("$.vessel").value("Sin Tx"))
                .andExpect(jsonPath("$.ownerName").value("Sin Transaccion"))
                .andReturn().getResponse().getContentAsString();
        String tripId = created.replaceAll(".*\"id\":\"([^\"]*)\".*", "$1");

        mockMvc.perform(post("/api/v1/faenas/" + tripId + "/capturas").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"speciesId":"%s","quantity":40,"weightKg":120.5,"averageSizeCm":41}"""
                        .formatted(species.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.captures[0].species").value("Pargo sin tx"))
                .andExpect(jsonPath("$.permitNumber").value("PES-NOTX-1"));

        mockMvc.perform(post("/api/v1/faenas/" + tripId + "/cerrar").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nullDeclaration":false}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(get("/api/v1/faenas").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(tripId))
                .andExpect(jsonPath("$[0].permitNumber").value("PES-NOTX-1"))
                .andExpect(jsonPath("$[0].totalWeightKg").value(120.5));
    }

    @Test
    @DisplayName("Listado de embarcaciones fuera de transacción incluye al propietario")
    void listsVesselsOutsideTransaction() throws Exception {
        mockMvc.perform(get("/api/v1/embarcaciones").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].registration").value("DIMAR-NOTX-1"))
                .andExpect(jsonPath("$[0].ownerName").value("Sin Transaccion"));
    }

    @Test
    @DisplayName("Resumen de inicio fuera de transacción")
    void summaryOutsideTransaction() throws Exception {
        mockMvc.perform(get("/api/v1/resumen").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Sin Transaccion"))
                .andExpect(jsonPath("$.vessels.value").value(1))
                .andExpect(jsonPath("$.permits.value").value(1));
    }
}
