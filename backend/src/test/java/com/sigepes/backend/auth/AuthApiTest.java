package com.sigepes.backend.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.sigepes.backend.support.ApiTestSupport;
import com.sigepes.backend.users.Role;

/** RF-01 · Registro, autenticación y RN-01 (documento único). */
class AuthApiTest extends ApiTestSupport {

    private static final String REGISTER = """
            {"nationalId":"%s","fullName":"Aníbal Mosquera","email":"%s","role":"FISHER","password":"Pesca#2026"}""";

    @Test
    @DisplayName("Registra un usuario y devuelve token y usuario sin contraseña")
    void registersUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REGISTER.formatted("1234567890", "nuevo@sigepes.gov.co")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("nuevo@sigepes.gov.co"))
                .andExpect(jsonPath("$.user.role").value("FISHER"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("RN-01: rechaza un documento de identidad ya registrado")
    void rejectsDuplicateDocument() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REGISTER.formatted("1111111111", "primero@sigepes.gov.co")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REGISTER.formatted("1111111111", "segundo@sigepes.gov.co")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DOCUMENT_ALREADY_REGISTERED"));
    }

    @Test
    @DisplayName("Rechaza un correo ya registrado")
    void rejectsDuplicateEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REGISTER.formatted("2222222222", "repetido@sigepes.gov.co")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REGISTER.formatted("3333333333", "repetido@sigepes.gov.co")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    @DisplayName("Valida el formato de la identificación")
    void rejectsInvalidNationalId() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REGISTER.formatted("12", "corto@sigepes.gov.co")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("nationalId"));
    }

    @Test
    @DisplayName("Inicia sesión con credenciales correctas")
    void logsIn() throws Exception {
        givenUser("4444444444", "login@sigepes.gov.co", Role.FISHER);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"login@sigepes.gov.co","password":"Pesca#2026"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("login@sigepes.gov.co"));
    }

    @Test
    @DisplayName("Rechaza credenciales incorrectas")
    void rejectsBadCredentials() throws Exception {
        givenUser("5555555555", "malo@sigepes.gov.co", Role.FISHER);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"malo@sigepes.gov.co","password":"OtraClave#1"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("No almacena la contraseña en claro")
    void hashesPassword() throws Exception {
        var user = givenUser("6666666666", "hash@sigepes.gov.co", Role.FISHER);
        org.junit.jupiter.api.Assertions.assertNotEquals(PASSWORD, user.getPasswordHash());
        org.junit.jupiter.api.Assertions.assertTrue(user.getPasswordHash().startsWith("$2"));
    }

    @Test
    @DisplayName("Devuelve 401 sin token en un endpoint protegido")
    void requiresToken() throws Exception {
        mockMvc.perform(get("/api/v1/faenas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("/auth/me devuelve la sesión del token")
    void returnsSession() throws Exception {
        givenUser("7777777777", "me@sigepes.gov.co", Role.INSPECTOR);
        String token = bearerFor("me@sigepes.gov.co");

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("INSPECTOR"));
    }
}
