package com.sigepes.backend.species;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.sigepes.backend.gears.FishingGearEntity;
import com.sigepes.backend.permits.PermitStatus;
import com.sigepes.backend.support.ApiTestSupport;
import com.sigepes.backend.users.Role;
import com.sigepes.backend.users.UserEntity;
import com.sigepes.backend.vessels.VesselEntity;
import com.sigepes.backend.vessels.VesselStatus;

/** RF-06 · Catálogo de especies: atributos obligatorios y versionado (AC-06.2 / AC-06.4). */
class SpeciesControllerTest extends ApiTestSupport {

    private UserEntity admin;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        admin = givenUser("7070707070", "admin.especies@sigepes.gov.co", Role.ADMIN);
        adminToken = bearerFor("admin.especies@sigepes.gov.co");
    }

    @Test
    @DisplayName("AC-06.2: rechaza el alta sin método de medición, región o imagen de referencia")
    void rejectsIncompleteSpecies() throws Exception {
        mockMvc.perform(post("/api/v1/especies").header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"commonName":"Mero","scientificName":"Epinephelus itajara","minSizeCm":60}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("AC-06.2: registra una especie con todos los atributos requeridos")
    void createsCompleteSpecies() throws Exception {
        mockMvc.perform(post("/api/v1/especies").header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"commonName":"Mero","scientificName":"Epinephelus itajara","minSizeCm":60,
                        "measurementMethod":"FORK_LENGTH","region":"CARIBE",
                        "imageUrl":"https://example.org/mero.jpg"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.measurementMethod").value("FORK_LENGTH"))
                .andExpect(jsonPath("$.region").value("CARIBE"))
                .andExpect(jsonPath("$.imageUrl").value("https://example.org/mero.jpg"));
    }

    @Test
    @DisplayName("AC-06.4: cada alta y edición agrega una versión al historial")
    void tracksVersionHistory() throws Exception {
        String speciesId = extract(mockMvc.perform(post("/api/v1/especies").header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"commonName":"Mero","scientificName":"Epinephelus itajara","minSizeCm":60,
                        "measurementMethod":"FORK_LENGTH","region":"CARIBE",
                        "imageUrl":"https://example.org/mero.jpg"}"""))
                .andReturn().getResponse().getContentAsString(), "id");

        mockMvc.perform(get("/api/v1/especies/" + speciesId + "/versiones").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].version").value(1));

        mockMvc.perform(patch("/api/v1/especies/" + speciesId).header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"minSizeCm":65}"""))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/especies/" + speciesId + "/versiones").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].version").value(2))
                .andExpect(jsonPath("$[0].minSizeCm").value(65))
                .andExpect(jsonPath("$[1].version").value(1))
                .andExpect(jsonPath("$[1].minSizeCm").value(60));
    }

    @Test
    @DisplayName("AC-06.4: editar la talla mínima no altera el registro histórico de una faena ya cerrada")
    void doesNotAlterPastCatchesOnSpeciesUpdate() throws Exception {
        UserEntity fisher = givenUser("7171717171", "pescador.especies@sigepes.gov.co", Role.FISHER);
        String fisherToken = bearerFor("pescador.especies@sigepes.gov.co");
        FishingGearEntity gear = givenGear("Línea especies test", false);
        VesselEntity vessel = givenVessel(fisher, "DIMAR-ESP-1", VesselStatus.ACTIVE, gear);
        givenPermit(fisher, "PES-ESP-1", PermitStatus.APPROVED, LocalDate.now().minusDays(1),
                LocalDate.now().plusYears(1));
        SpeciesEntity snapper = givenSpecies("Pargo especies test", new BigDecimal("35.00"));

        String tripId = extract(mockMvc.perform(post("/api/v1/faenas").header("Authorization", fisherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"vesselId":"%s","zone":"Pacífico Norte","originPort":"Bahía Solano"}"""
                        .formatted(vessel.getId())))
                .andReturn().getResponse().getContentAsString(), "id");

        mockMvc.perform(post("/api/v1/faenas/" + tripId + "/capturas").header("Authorization", fisherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"speciesId":"%s","quantity":2,"weightKg":10,"averageSizeCm":40}"""
                        .formatted(snapper.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.captures[0].minSizeCmAtCapture").value(35.0));

        // La autoridad sube la talla mínima legal de 35 a 45 cm después de cerrada la faena.
        mockMvc.perform(patch("/api/v1/especies/" + snapper.getId()).header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"minSizeCm":45}"""))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/faenas/" + tripId).header("Authorization", fisherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.captures[0].minSizeCmAtCapture").value(35.0));

        mockMvc.perform(get("/api/v1/especies/" + snapper.getId()).header("Authorization", fisherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minSizeCm").value(45.0));
    }
}
