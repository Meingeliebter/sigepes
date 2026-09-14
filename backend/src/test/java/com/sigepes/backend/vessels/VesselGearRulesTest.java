package com.sigepes.backend.vessels;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.sigepes.backend.gears.FishingGearEntity;
import com.sigepes.backend.support.ApiTestSupport;
import com.sigepes.backend.users.Role;
import com.sigepes.backend.users.UserEntity;

/** RF-02 · Embarcaciones y RN-04 (artes destructivas o prohibidas). */
class VesselGearRulesTest extends ApiTestSupport {

    private UserEntity fisher;
    private FishingGearEntity allowed;
    private FishingGearEntity prohibited;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        fisher = givenUser("3030303030", "armador.test@sigepes.gov.co", Role.FISHER);
        allowed = givenGear("Nasa test", false);
        prohibited = givenGear("Dinamita test", true);
        token = bearerFor("armador.test@sigepes.gov.co");
    }

    @Test
    @DisplayName("Registra una embarcación con un arte permitida")
    void registersVesselWithAllowedGear() throws Exception {
        mockMvc.perform(post("/api/v1/embarcaciones").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"La Sirena","registration":"DIMAR-OK-1","port":"Bahía Solano","gears":["%s"]}"""
                        .formatted(allowed.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.gears[0]").value("Nasa test"))
                .andExpect(jsonPath("$.ownerId").value(fisher.getId()));
    }

    @Test
    @DisplayName("Acepta artes indicadas por nombre, como hace la interfaz")
    void resolvesGearByName() throws Exception {
        mockMvc.perform(post("/api/v1/embarcaciones").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Por nombre","registration":"DIMAR-OK-2","port":"Tumaco","gears":["Nasa test"]}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gears[0]").value("Nasa test"));
    }

    @Test
    @DisplayName("RN-04: rechaza una embarcación con arte destructiva")
    void rejectsProhibitedGear() throws Exception {
        mockMvc.perform(post("/api/v1/embarcaciones").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Ilegal","registration":"DIMAR-NO-1","port":"Tumaco","gears":["%s"]}"""
                        .formatted(prohibited.getId())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PROHIBITED_GEAR"));
    }

    @Test
    @DisplayName("Rechaza una matrícula duplicada")
    void rejectsDuplicateRegistration() throws Exception {
        givenVessel(fisher, "DIMAR-DUP-1", VesselStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/embarcaciones").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Repetida","registration":"DIMAR-DUP-1","port":"Tumaco"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REGISTRATION_ALREADY_REGISTERED"));
    }

    @Test
    @DisplayName("Un pescador solo ve sus propias embarcaciones")
    void listsOnlyOwnVessels() throws Exception {
        givenVessel(fisher, "DIMAR-MIA-1", VesselStatus.ACTIVE);
        UserEntity other = givenUser("4040404040", "ajeno@sigepes.gov.co", Role.FISHER);
        givenVessel(other, "DIMAR-AJENA-1", VesselStatus.ACTIVE);

        mockMvc.perform(get("/api/v1/embarcaciones").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].registration").value("DIMAR-MIA-1"));
    }

    @Test
    @DisplayName("La autoridad ve todas las embarcaciones")
    void authoritySeesEverything() throws Exception {
        givenVessel(fisher, "DIMAR-A-1", VesselStatus.ACTIVE);
        UserEntity other = givenUser("5050505050", "ajeno2@sigepes.gov.co", Role.FISHER);
        givenVessel(other, "DIMAR-B-1", VesselStatus.ACTIVE);
        givenUser("6060606060", "inspector.test@sigepes.gov.co", Role.INSPECTOR);

        mockMvc.perform(get("/api/v1/embarcaciones")
                .header("Authorization", bearerFor("inspector.test@sigepes.gov.co")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
