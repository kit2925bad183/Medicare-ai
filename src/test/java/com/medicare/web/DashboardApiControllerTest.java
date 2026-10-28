package com.medicare.web;

import com.medicare.appointment.Appointment;
import com.medicare.appointment.AppointmentService;
import com.medicare.common.CurrentUserService;
import com.medicare.identity.Role;
import com.medicare.identity.User;
import com.medicare.medicine.DoseLogRepository;
import com.medicare.medicine.OccurrenceState;
import com.medicare.medicine.ReminderService;
import com.medicare.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DashboardApiControllerTest {

    @Mock
    private ReminderService reminderService;

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private DoseLogRepository doseLogRepository;

    @Mock
    private CurrentUserService currentUserService;

    private MockMvc mockMvc;
    private User user;

    @BeforeEach
    void setUp() {
        DashboardApiController controller = new DashboardApiController(
            reminderService, appointmentService, notificationService,
            doseLogRepository, currentUserService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        user = new User("patient@test.local", "hash", "Alice", "Wonder", Role.PATIENT);
        user.setId(7L);
    }

    @Test
    void testDashboardSummaryEndpoint() throws Exception {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(reminderService.upcoming()).thenReturn(List.of());
        when(appointmentService.mine()).thenReturn(List.of());
        when(reminderService.notifications()).thenReturn(List.of());
        when(reminderService.todayOccurrences()).thenReturn(List.of());
        when(doseLogRepository.countByUserIdAndActionAndActionTimeBetween(eq(7L), eq("TAKEN"), any(), any())).thenReturn(5L);
        when(doseLogRepository.countByUserIdAndActionTimeBetween(eq(7L), any(), any())).thenReturn(5L);

        mockMvc.perform(get("/api/dashboard/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.upcomingRemindersCount").value(0))
            .andExpect(jsonPath("$.thirtyDayAdherenceRate").value(100.0))
            .andExpect(jsonPath("$.adherenceDisclaimer").exists());
    }

    @Test
    void testTodayDosesEndpoint() throws Exception {
        ReminderService.OccurrenceView view = new ReminderService.OccurrenceView(
            1L, "Insulin", "Take with water", Instant.now(), "UTC", "AVAILABLE"
        );
        when(reminderService.todayOccurrences()).thenReturn(List.of(view));

        mockMvc.perform(get("/api/doses/today"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].medicineName").value("Insulin"))
            .andExpect(jsonPath("$[0].state").value("AVAILABLE"));
    }

    @Test
    void testRecordDoseActionEndpoint() throws Exception {
        mockMvc.perform(post("/api/doses/1/actions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\": \"TAKEN\", \"notes\": \"with breakfast\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.occurrenceId").value(1))
            .andExpect(jsonPath("$.state").value("TAKEN"));

        verify(reminderService).recordDoseAction(1L, "TAKEN", "with breakfast");
    }

    @Test
    void testPushSubscribeEndpoint() throws Exception {
        mockMvc.perform(post("/api/push/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"endpoint\": \"https://push.service.com/sub/123\", \"p256dh\": \"key123\", \"auth\": \"auth123\"}"))
            .andExpect(status().isOk());

        verify(notificationService).subscribe("https://push.service.com/sub/123", "key123", "auth123");
    }

    @Test
    void testPushUnsubscribeEndpoint() throws Exception {
        mockMvc.perform(post("/api/push/unsubscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"endpoint\": \"https://push.service.com/sub/123\"}"))
            .andExpect(status().isOk());

        verify(notificationService).unsubscribe("https://push.service.com/sub/123");
    }
}
