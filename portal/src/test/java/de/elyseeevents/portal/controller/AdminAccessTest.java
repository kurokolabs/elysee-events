package de.elyseeevents.portal.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAccessTest {

    @Autowired
    private MockMvc mockMvc;

    // --- CUSTOMER role should get 403 for all admin endpoints ---

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void adminDashboardForbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/portal/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void adminBuchungenForbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/portal/admin/buchungen"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void adminKundenForbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/portal/admin/kunden"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void adminRechnungenForbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/portal/admin/rechnungen"))
                .andExpect(status().isForbidden());
    }

    // --- ADMIN role should get 200 for all admin endpoints ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminDashboardAccessibleForAdmin() throws Exception {
        mockMvc.perform(get("/portal/admin"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminBuchungenAccessibleForAdmin() throws Exception {
        mockMvc.perform(get("/portal/admin/buchungen"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminKundenAccessibleForAdmin() throws Exception {
        mockMvc.perform(get("/portal/admin/kunden"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminRechnungenAccessibleForAdmin() throws Exception {
        mockMvc.perform(get("/portal/admin/rechnungen"))
                .andExpect(status().isOk());
    }
}
