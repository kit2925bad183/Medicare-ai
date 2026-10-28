package com.medicare.assistant;

import com.medicare.identity.User;
import com.medicare.medicine.ReminderService;
import com.medicare.medicine.ScheduleFrequency;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import java.util.regex.Matcher;

@Component
public class SchedulePlanner {

    private final ReminderService reminderService;

    public SchedulePlanner(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    public RuleBasedAssistant.ProposedSchedule planFromIntent(IntentParser.ParsedIntent intent, User user) {
        Matcher matcher = intent.matcher();
        if (matcher == null) {
            return null;
        }

        LocalDate today = reminderService.todayInUserTimeZone();
        String command = intent.normalizedCommand().toLowerCase(Locale.ROOT);

        if (command.startsWith("plan daily")) {
            String name = matcher.group(1).trim();
            LocalTime time = LocalTime.of(Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
            return new RuleBasedAssistant.ProposedSchedule(name, time, today, user.getTimeZone(), ScheduleFrequency.DAILY, null);
        } else if (command.startsWith("plan weekly")) {
            String name = matcher.group(1).trim();
            String dayName = matcher.group(2).toUpperCase(Locale.ROOT);
            int weekday = DayOfWeek.valueOf(dayName).getValue();
            LocalTime time = LocalTime.of(Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(4)));
            return new RuleBasedAssistant.ProposedSchedule(name, time, today, user.getTimeZone(), ScheduleFrequency.WEEKLY, weekday);
        }

        return null;
    }
}
