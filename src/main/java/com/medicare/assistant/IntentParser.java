package com.medicare.assistant;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class IntentParser {

    public static final Pattern DAILY_PLAN = Pattern.compile(
        "^plan daily reminder for (.{1,120}) at ([01]?[0-9]|2[0-3]):([0-5][0-9])$",
        Pattern.CASE_INSENSITIVE
    );

    public static final Pattern WEEKLY_PLAN = Pattern.compile(
        "^plan weekly reminder for (.{1,120}) on (monday|tuesday|wednesday|thursday|friday|saturday|sunday) at ([01]?[0-9]|2[0-3]):([0-5][0-9])$",
        Pattern.CASE_INSENSITIVE
    );

    public ParsedIntent parse(String command) {
        if (command == null) {
            return new ParsedIntent(IntentType.UNKNOWN, "", null);
        }

        String normalized = command.trim().replaceAll("\\s+", " ");
        String lower = normalized.toLowerCase(Locale.ROOT);

        if (lower.equals("show today's medicine schedule")
            || lower.equals("show today schedule")
            || lower.equals("today's medicine schedule")
            || lower.equals("today schedule")
            || lower.equals("today's schedule")) {
            return new ParsedIntent(IntentType.SHOW_TODAY_SCHEDULE, normalized, null);
        }

        if (lower.equals("summarize upcoming appointments")
            || lower.equals("upcoming appointments")
            || lower.equals("show appointments")
            || lower.equals("my appointments")
            || lower.equals("appointments summary")) {
            return new ParsedIntent(IntentType.SUMMARIZE_APPOINTMENTS, normalized, null);
        }

        if (lower.equals("explain saved reminder settings")
            || lower.equals("explain my reminder settings")
            || lower.equals("reminder settings")
            || lower.equals("explain settings")) {
            return new ParsedIntent(IntentType.EXPLAIN_REMINDER_SETTINGS, normalized, null);
        }

        if (lower.equals("propose notification preferences")
            || lower.equals("propose notification settings")
            || lower.equals("suggest notification preferences")) {
            return new ParsedIntent(IntentType.PROPOSE_NOTIFICATION_PREFERENCES, normalized, null);
        }

        if (lower.equals("identify conflicting or incomplete schedules")
            || lower.equals("identify conflicts")
            || lower.equals("check schedule conflicts")
            || lower.equals("check conflicts")
            || lower.equals("conflicting schedules")) {
            return new ParsedIntent(IntentType.IDENTIFY_CONFLICTS, normalized, null);
        }

        Matcher dailyMatcher = DAILY_PLAN.matcher(normalized);
        if (dailyMatcher.matches()) {
            return new ParsedIntent(IntentType.PREPARE_REMINDER_PLAN, normalized, dailyMatcher);
        }

        Matcher weeklyMatcher = WEEKLY_PLAN.matcher(normalized);
        if (weeklyMatcher.matches()) {
            return new ParsedIntent(IntentType.PREPARE_REMINDER_PLAN, normalized, weeklyMatcher);
        }

        return new ParsedIntent(IntentType.UNKNOWN, normalized, null);
    }

    public record ParsedIntent(IntentType type, String normalizedCommand, Matcher matcher) {
    }
}
