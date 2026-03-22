package de.elyseeevents.portal.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InputValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "demo@elysee-events.de", roles = "CUSTOMER")
    void submitInquiryWithValidDataRedirects() throws Exception {
        mockMvc.perform(post("/portal/anfrage")
                        .param("bookingType", "HOCHZEIT")
                        .param("eventDate", "2026-12-01")
                        .param("guestCount", "50")
                        .param("budget", "5000.00")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "demo@elysee-events.de", roles = "CUSTOMER")
    void submitInquiryWithoutBookingTypeReturnsBadRequest() throws Exception {
        // bookingType is @RequestParam (required=true by default), so omitting it returns 400
        mockMvc.perform(post("/portal/anfrage")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCreateBookingWithoutRequiredFieldsReturnsBadRequest() throws Exception {
        // customerId and bookingType are required @RequestParam
        mockMvc.perform(post("/portal/admin/buchung/neu")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCreateCustomerWithoutEmailReturnsBadRequest() throws Exception {
        // email, firstName, lastName are required @RequestParam
        mockMvc.perform(post("/portal/admin/kunde/neu")
                        .param("firstName", "Test")
                        .param("lastName", "User")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCreateCustomerWithValidDataRedirects() throws Exception {
        mockMvc.perform(post("/portal/admin/kunde/neu")
                        .param("email", "newcustomer@example.de")
                        .param("firstName", "Max")
                        .param("lastName", "Mustermann")
                        .param("company", "Test GmbH")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "admin@test.de", roles = "ADMIN")
    void passwordChangeWithEmptyFieldsReturnsBadRequest() throws Exception {
        // All three params are required @RequestParam
        mockMvc.perform(post("/portal/passwort-aendern")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }
}
