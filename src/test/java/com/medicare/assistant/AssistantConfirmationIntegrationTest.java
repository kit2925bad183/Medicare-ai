package com.medicare.assistant;

import com.medicare.identity.Role;
import com.medicare.identity.User;
import com.medicare.identity.UserRepository;
import com.medicare.medicine.MedicineScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class AssistantConfirmationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MedicineScheduleRepository scheduleRepository;

    private User patient;

    @BeforeEach
    void createPatient() {
        patient = userRepository.saveAndFlush(new User(
            "assistant-" + UUID.randomUUID() + "@example.test",
            "encoded-password",
            "Test",
            "Patient",
            Role.PATIENT
        ));
    }

    @Test
    void dashboardProvidesWorkingAuthenticatedDestination() throws Exception {
        mockMvc.perform(get("/dashboard").with(user(patient.getEmail())))
            .andExpect(status().isOk())
            .andExpect(view().name("dashboard/index"));
    }

    @Test
    void medicineChangesRequireCsrfTokens() throws Exception {
        mockMvc.perform(post("/medicines").with(user(patient.getEmail()))
                .param("medicineName", "Medicine")
                .param("frequency", "DAILY")
                .param("localTime", "08:00")
                .param("startDate", "2026-10-28"))
            .andExpect(status().isForbidden());
    }

    @Test
    void naturalLanguageOutsideTheDocumentedGrammarIsNotInterpretedAsAPlan() throws Exception {
        mockMvc.perform(post("/assistant")
                .with(user(patient.getEmail()))
                .with(csrf())
                .param("command", "should I take more medicine?"))
            .andExpect(status().isOk())
            .andExpect(model().attributeDoesNotExist("proposal", "confirmationToken"));

        assertTrue(scheduleRepository.findAllByOwnerIdOrderByLocalTimeAsc(patient.getId()).isEmpty());
    }

    @Test
    void assistantProposalIsNotSavedUntilExplicitConfirmation() throws Exception {
        MvcResult preview = mockMvc.perform(post("/assistant")
                .with(user(patient.getEmail()))
                .with(csrf())
                .param("command", "plan daily reminder for Vitamin D at 08:15"))
            .andExpect(status().isOk())
            .andExpect(view().name("assistant/index"))
            .andExpect(model().attributeExists("confirmationToken"))
            .andReturn();

        assertTrue(scheduleRepository.findAllByOwnerIdOrderByLocalTimeAsc(patient.getId()).isEmpty());
        String token = (String) preview.getModelAndView().getModel().get("confirmationToken");
        MockHttpSession session = (MockHttpSession) preview.getRequest().getSession(false);

        mockMvc.perform(post("/assistant/confirm")
                .session(session)
                .with(user(patient.getEmail()))
                .with(csrf())
                .param("token", token))
            .andExpect(status().is3xxRedirection());

        var savedSchedules = scheduleRepository.findAllByOwnerIdOrderByLocalTimeAsc(patient.getId());
        assertEquals(1, savedSchedules.size());
        assertEquals("Vitamin D", savedSchedules.get(0).getMedicineName());
    }
}
