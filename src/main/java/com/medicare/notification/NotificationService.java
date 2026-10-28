package com.medicare.notification;

import com.medicare.common.CurrentUserService;
import com.medicare.identity.User;
import com.medicare.identity.UserRepository;
import com.medicare.medicine.AuditEvent;
import com.medicare.medicine.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.ZoneId;

@Service
public class NotificationService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final AuditEventRepository auditRepository;

    public NotificationService(NotificationPreferenceRepository preferenceRepository,
                               PushSubscriptionRepository pushSubscriptionRepository,
                               UserRepository userRepository,
                               CurrentUserService currentUserService,
                               AuditEventRepository auditRepository) {
        this.preferenceRepository = preferenceRepository;
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.auditRepository = auditRepository;
    }

    @Transactional
    public NotificationPreference preferences() {
        User user = currentUserService.getCurrentUser();
        return preferenceRepository.findByUserId(user.getId())
            .orElseGet(() -> preferenceRepository.save(new NotificationPreference(user)));
    }

    @Transactional
    public void updatePreferences(boolean emailEnabled, boolean browserEnabled, boolean pushEnabled) {
        updatePreferences(emailEnabled, browserEnabled, pushEnabled, null, null, null);
    }

    @Transactional
    public void updatePreferences(boolean emailEnabled, boolean browserEnabled, boolean pushEnabled,
                                  LocalTime quietStart, LocalTime quietEnd, String timeZone) {
        User user = currentUserService.getCurrentUser();
        NotificationPreference pref = preferences();
        pref.setEmailEnabled(emailEnabled);
        pref.setBrowserEnabled(browserEnabled);
        pref.setPushEnabled(pushEnabled);
        pref.setQuietStart(quietStart);
        pref.setQuietEnd(quietEnd);

        if (timeZone != null && !timeZone.isBlank()) {
            ZoneId.of(timeZone); // validates IANA ZoneId
            pref.setTimeZone(timeZone);
            user.setTimeZone(timeZone);
            userRepository.save(user);
        }

        preferenceRepository.save(pref);
        auditRepository.save(new AuditEvent(user, "NOTIFICATION_PREFERENCES_UPDATED", pref.getId()));
    }

    @Transactional
    public void subscribe(String endpoint, String p256dh, String auth) {
        User user = currentUserService.getCurrentUser();
        pushSubscriptionRepository.save(new PushSubscription(user, endpoint, p256dh, auth));
        auditRepository.save(new AuditEvent(user, "PUSH_SUBSCRIPTION_REGISTERED", null));
    }

    @Transactional
    public void unsubscribe(String endpoint) {
        pushSubscriptionRepository.deleteByEndpoint(endpoint);
    }
}
