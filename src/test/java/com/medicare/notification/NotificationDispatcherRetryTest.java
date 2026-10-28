package com.medicare.notification;

import com.medicare.identity.Role;
import com.medicare.identity.User;
import com.medicare.medicine.NotificationDelivery;
import com.medicare.medicine.NotificationDeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherRetryTest {

    @Mock
    private NotificationChannel mockChannel;

    @Mock
    private NotificationDeliveryRepository deliveryRepository;

    private Clock clock;
    private NotificationDispatcherService dispatcher;
    private User recipient;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-11-01T12:00:00Z"), ZoneId.of("UTC"));
        when(mockChannel.getChannelName()).thenReturn("EMAIL");
        dispatcher = new NotificationDispatcherService(List.of(mockChannel), deliveryRepository, clock);

        recipient = new User("user@test.local", "pass", "John", "Doe", Role.PATIENT);
        recipient.setId(10L);
    }

    @Test
    void testSuccessfulDeliveryClearsRetryAndSetsAttempt() throws Exception {
        NotificationDelivery delivery = new NotificationDelivery(recipient, "EMAIL", "PENDING");
        delivery.setId(1L);

        dispatcher.dispatch(delivery);

        verify(mockChannel).deliver(delivery);
        assertEquals(1, delivery.getAttemptCount());
        assertNull(delivery.getNextRetryAt());
        assertNull(delivery.getLastErrorCategory());
        verify(deliveryRepository).save(delivery);
    }

    @Test
    void testFirstFailureSchedulesRetryInTwoMinutes() throws Exception {
        NotificationDelivery delivery = new NotificationDelivery(recipient, "EMAIL", "PENDING");
        delivery.setId(2L);
        delivery.setAttemptCount(0);

        doThrow(new NotificationDeliveryException("SMTP_DISPATCH_FAILURE", "Transient network timeout"))
            .when(mockChannel).deliver(delivery);

        dispatcher.dispatch(delivery);

        assertEquals(1, delivery.getAttemptCount());
        assertEquals("RETRYING", delivery.getState());
        assertEquals("SMTP_DISPATCH_FAILURE", delivery.getLastErrorCategory());
        assertEquals(Instant.parse("2026-11-01T12:02:00Z"), delivery.getNextRetryAt());
        verify(deliveryRepository).save(delivery);
    }

    @Test
    void testSecondFailureSchedulesRetryInFiveMinutes() throws Exception {
        NotificationDelivery delivery = new NotificationDelivery(recipient, "EMAIL", "RETRYING");
        delivery.setId(3L);
        delivery.setAttemptCount(1);

        doThrow(new NotificationDeliveryException("SMTP_DISPATCH_FAILURE", "Transient network timeout"))
            .when(mockChannel).deliver(delivery);

        dispatcher.dispatch(delivery);

        assertEquals(2, delivery.getAttemptCount());
        assertEquals("RETRYING", delivery.getState());
        assertEquals(Instant.parse("2026-11-01T12:05:00Z"), delivery.getNextRetryAt());
        verify(deliveryRepository).save(delivery);
    }

    @Test
    void testThirdFailureTransitionsToTerminalFailedState() throws Exception {
        NotificationDelivery delivery = new NotificationDelivery(recipient, "EMAIL", "RETRYING");
        delivery.setId(4L);
        delivery.setAttemptCount(2);

        doThrow(new NotificationDeliveryException("SMTP_DISPATCH_FAILURE", "Permanent rejection"))
            .when(mockChannel).deliver(delivery);

        dispatcher.dispatch(delivery);

        assertEquals(3, delivery.getAttemptCount());
        assertEquals("FAILED", delivery.getState());
        assertNull(delivery.getNextRetryAt());
        verify(deliveryRepository).save(delivery);
    }

    @Test
    void testChannelDisabledImmediatelyTerminatesWithoutRetry() throws Exception {
        NotificationDelivery delivery = new NotificationDelivery(recipient, "EMAIL", "PENDING");
        delivery.setId(5L);
        delivery.setAttemptCount(0);

        doThrow(new NotificationDeliveryException("CHANNEL_DISABLED", "User opted out"))
            .when(mockChannel).deliver(delivery);

        dispatcher.dispatch(delivery);

        assertEquals(1, delivery.getAttemptCount());
        assertEquals("FAILED", delivery.getState());
        assertNull(delivery.getNextRetryAt());
        verify(deliveryRepository).save(delivery);
    }

    @Test
    void testUnknownChannelTransitionsToFailed() {
        NotificationDelivery delivery = new NotificationDelivery(recipient, "CARRIER_PIGEON", "PENDING");
        delivery.setId(6L);

        dispatcher.dispatch(delivery);

        assertEquals("FAILED", delivery.getState());
        assertEquals("UNKNOWN_CHANNEL", delivery.getLastErrorCategory());
        verify(deliveryRepository).save(delivery);
    }
}
