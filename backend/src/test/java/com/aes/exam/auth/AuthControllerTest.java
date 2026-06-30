package com.aes.exam.auth;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginMeAndLogoutWork() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(get("/api/auth/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.username", is("admin")))
            .andExpect(jsonPath("$.data.role", is("admin")));

        mockMvc.perform(post("/api/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)));

        mockMvc.perform(get("/api/auth/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    void roleBasedAccessControlProtectsShellRoutes() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(get("/api/admin/shell")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.role", is("admin")));

        mockMvc.perform(get("/api/teacher/shell")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code", is("FORBIDDEN")));
    }

    @Test
    void protectedRoutesRejectAnonymousUsers() throws Exception {
        mockMvc.perform(get("/api/admin/shell"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"admin","password":"wrong"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    private String loginAsAdmin() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"admin","password":"Admin@123456"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data.token", notNullValue()))
            .andExpect(jsonPath("$.data.user.role", is("admin")))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        return root.path("data").path("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
