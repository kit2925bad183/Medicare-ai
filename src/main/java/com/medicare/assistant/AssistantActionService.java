package com.medicare.assistant;

import com.medicare.common.CurrentUserService;
import com.medicare.identity.User;
import com.medicare.medicine.AuditEvent;
import com.medicare.medicine.AuditEventRepository;
import com.medicare.medicine.MedicineScheduleRequest;
import com.medicare.medicine.ReminderService;
import com.medicare.medicine.ScheduleFrequency;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class AssistantActionService {

    private final ReminderService reminderService;
    private final CurrentUserService currentUserService;
    private final AuditEventRepository auditRepository;

    public AssistantActionService(ReminderService reminderService, CurrentUserService currentUserService,
                                  AuditEventRepository auditRepository) {
        this.reminderService = reminderService;
        this.currentUserService = currentUserService;
        this.auditRepository = auditRepository;
    }

    @Transactional
    public ReminderService.ScheduleView confirm(RuleBasedAssistant.ProposedSchedule proposal) {
        User user = currentUserService.getCurrentUser();
        if (!user.getTimeZone().equals(proposal.timeZone())) {
            throw new IllegalArgumentException("Your time zone changed. Preview the reminder again.");
        }
        MedicineScheduleRequest request = new MedicineScheduleRequest();
        request.setMedicineName(proposal.medicineName());
        request.setFrequency(proposal.frequency() != null ? proposal.frequency() : ScheduleFrequency.DAILY);
        request.setWeekday(proposal.weekday());
        request.setLocalTime(proposal.localTime());
        LocalDate today = LocalDate.now(ZoneId.of(user.getTimeZone()));
        request.setStartDate(proposal.startDate().isBefore(today) ? today : proposal.startDate());
        ReminderService.ScheduleView saved = reminderService.createFromAssistant(request, proposal.timeZone());
        auditRepository.save(new AuditEvent(user, "ASSISTANT_PLAN_EXECUTED", saved.id()));
        return saved;
    }

    @Transactional
    public ReminderService.ScheduleView confirmDaily(RuleBasedAssistant.ProposedSchedule proposal) {
        return confirm(proposal);
    }
}
