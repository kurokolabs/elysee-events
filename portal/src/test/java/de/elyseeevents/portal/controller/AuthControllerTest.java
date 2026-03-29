package de.elyseeevents.portal.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginWithInvalidCredentialsRedirectsWithError() throws Exception {
        mockMvc.perform(post("/portal/login")
                        .param("username", "invalid@test.de")
                        .param("password", "wrongpassword")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/portal/login?error=true"));
    }

    @Test
    @WithMockUser(username = "admin@test.de", roles = "ADMIN")
    void passwordChangeRejectsShortPassword() throws Exception {
        mockMvc.perform(post("/portal/passwort-aendern")
                        .param("currentPassword", "Test-0nly-N0t-Pr0d!")
                        .param("newPassword", "short")
                        .param("confirmPassword", "short")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/portal/passwort-aendern"));
    }

    @Test
    @WithMockUser(username = "admin@test.de", roles = "ADMIN")
    void passwordChangeRejectsMismatch() throws Exception {
        mockMvc.perform(post("/portal/passwort-aendern")
                        .param("currentPassword", "Test-0nly-N0t-Pr0d!")
                        .param("newPassword", "NewValidPassword1!")
                        .param("confirmPassword", "DifferentPassword1!")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/portal/passwort-aendern"));
    }

    @Test
    @WithMockUser(username = "admin@test.de", roles = "ADMIN")
    void alreadyLoggedInUserRedirectedFromLoginPage() throws Exception {
        mockMvc.perform(get("/portal/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/portal/admin"));
    }

    @Test
    @WithMockUser(username = "demo@elysee-events.de", roles = "CUSTOMER")
    void alreadyLoggedInCustomerRedirectedFromLoginPage() throws Exception {
        mockMvc.perform(get("/portal/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/portal/dashboard"));
    }
}
