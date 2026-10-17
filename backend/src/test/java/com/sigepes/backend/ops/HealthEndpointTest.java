package com.sigepes.backend.ops;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sigepes.backend.support.ApiTestSupport;

/** HU-21 · AC-21.3: el health check es público, responde UP y no expone detalles internos. */
class HealthEndpointTest extends ApiTestSupport {

    @Test
    @DisplayName("TC-HU21-03 · /actuator/health responde UP sin autenticación")
    void healthIsPublicAndUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    @DisplayName("AC-21.4 · Otros endpoints de actuator no están expuestos")
    void otherActuatorEndpointsAreNotExposed() throws Exception {
        mockMvc.perform(get("/actuator/env")).andExpect(status().is4xxClientError());
    }
}
