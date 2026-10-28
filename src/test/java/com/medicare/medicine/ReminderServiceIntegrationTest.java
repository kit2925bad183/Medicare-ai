package com.medicare.medicine;

import com.medicare.identity.Role;
import com.medicare.identity.User;
import com.medicare.identity.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class ReminderServiceIntegrationTest {

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private MedicineScheduleRepository scheduleRepository;

    @Autowired
    private ReminderOccurrenceRepository occurrenceRepository;

    @Autowired
    private NotificationDeliveryRepository deliveryRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void occurrenceGenerationIsPersistentAndIdempotent() {
        User user = createUser("UTC");
        authenticate(user);
        MedicineScheduleRequest request = requestForTomorrow(ScheduleFrequency.DAILY, null, "Vitamin D");
        MedicineSchedule savedSchedule = scheduleRepository.findById(reminderService.create(request).id()).orElseThrow();

        reminderService.generateUpcomingOccurrences();
        reminderService.generateUpcomingOccurrences();

        assertEquals(8, occurrenceRepository.findAllByScheduleIdAndStateOrderByDueAtAsc(
            savedSchedule.getId(), OccurrenceState.PENDING).size());
    }

    @Test
    void localScheduleResolvesDaylightSavingGapUsingZoneRules() {
        User user = createUser("America/New_York");
        authenticate(user);
        MedicineScheduleRequest request = new MedicineScheduleRequest();
        request.setMedicineName("Prescription medicine");
        request.setFrequency(ScheduleFrequency.DAILY);
        request.setLocalTime(LocalTime.of(2, 30));
        request.setStartDate(LocalDate.of(2027, 3, 14));

        MedicineSchedule schedule = scheduleRepository.findById(reminderService.create(request).id()).orElseThrow();
        List<ReminderOccurrence> occurrences =
            occurrenceRepository.findAllByScheduleIdAndStateOrderByDueAtAsc(schedule.getId(), OccurrenceState.PENDING);

        assertTrue(occurrences.stream().anyMatch(occurrence ->
            occurrence.getDueAt().equals(Instant.parse("2027-03-14T07:30:00Z"))));
        assertEquals("America/New_York", occurrences.get(0).getTimeZone());
    }

    @Test
    void scheduleListsAreScopedToTheAuthenticatedOwner() {
        User firstUser = createUser("UTC");
        authenticate(firstUser);
        reminderService.create(requestForTomorrow(ScheduleFrequency.DAILY, null, "Owner one medicine"));
        assertEquals(1, reminderService.schedules().size());

        User secondUser = createUser("UTC");
        authenticate(secondUser);
        assertTrue(reminderService.schedules().isEmpty());
    }

    @Test
    void dueNotificationsAreCreatedOnceAndRemainInAppOnly() {
        User user = createUser("UTC");
        authenticate(user);
        ReminderService.ScheduleView created =
            reminderService.create(requestForTomorrow(ScheduleFrequency.DAILY, null, "Due medicine"));
        MedicineSchedule schedule = scheduleRepository.findById(created.id()).orElseThrow();
        ReminderOccurrence due = occurrenceRepository.saveAndFlush(new ReminderOccurrence(
            schedule, Instant.now().minusSeconds(1), "test-due:" + UUID.randomUUID()));

        reminderService.publishDueInAppNotifications();
        reminderService.publishDueInAppNotifications();

        ReminderOccurrence stored = occurrenceRepository.findById(due.getId()).orElseThrow();
        assertEquals(OccurrenceState.AVAILABLE, stored.getState());
        assertTrue(deliveryRepository.existsByOccurrenceIdAndChannel(due.getId(), "IN_APP"));
    }

    @Test
    void cancelledSchedulesCancelPendingOccurrencesAndCannotBeChangedByAnotherUser() {
        User owner = createUser("UTC");
        authenticate(owner);
        ReminderService.ScheduleView created =
            reminderService.create(requestForTomorrow(ScheduleFrequency.DAILY, null, "Private medicine"));

        User otherUser = createUser("UTC");
        authenticate(otherUser);
        assertThrows(jakarta.persistence.EntityNotFoundException.class,
            () -> reminderService.deactivate(created.id()));

        authenticate(owner);
        reminderService.deactivate(created.id());
        List<ReminderOccurrence> occurrences =
            occurrenceRepository.findAllByScheduleIdAndStateOrderByDueAtAsc(
                created.id(), OccurrenceState.CANCELLED);
        assertEquals(8, occurrences.size());
    }

    private MedicineScheduleRequest requestForTomorrow(ScheduleFrequency frequency, Integer weekday, String name) {
        MedicineScheduleRequest request = new MedicineScheduleRequest();
        request.setMedicineName(name);
        request.setFrequency(frequency);
        request.setWeekday(weekday);
        request.setLocalTime(LocalTime.of(9, 0));
        request.setStartDate(LocalDate.now(ZoneId.of("UTC")).plusDays(1));
        return request;
    }

    private User createUser(String timeZone) {
        String email = "reminder-" + UUID.randomUUID() + "@example.test";
        User user = new User(email, "encoded", "Test", "Patient", Role.PATIENT);
        user.setTimeZone(timeZone);
        return userRepository.saveAndFlush(user);
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(user.getEmail(), "test", List.of()));
    }
}
