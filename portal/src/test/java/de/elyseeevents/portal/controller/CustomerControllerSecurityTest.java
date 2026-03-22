package de.elyseeevents.portal.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "demo@elysee-events.de", roles = "CUSTOMER")
    void customerCannotAccessAdminRoutes() throws Exception {
        mockMvc.perform(get("/portal/admin"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/portal/admin/buchungen"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/portal/admin/kunden"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/portal/admin/rechnungen"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotAccessDashboard() throws Exception {
        mockMvc.perform(get("/portal/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/portal/login"));
    }
}
