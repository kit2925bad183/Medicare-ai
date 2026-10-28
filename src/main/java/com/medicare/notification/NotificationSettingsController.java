package com.medicare.notification;

import com.medicare.common.CurrentUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
public class NotificationSettingsController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    public NotificationSettingsController(NotificationService notificationService,
                                          CurrentUserService currentUserService) {
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/settings/notifications")
    public String settings(Model model) {
        var pref = notificationService.preferences();
        model.addAttribute("preferences", pref);
        model.addAttribute("userTimeZone", currentUserService.getCurrentUser().getTimeZone());

        List<String> zones = new ArrayList<>(ZoneId.getAvailableZoneIds());
        Collections.sort(zones);
        model.addAttribute("availableZones", zones);
        return "settings/notifications";
    }

    @PostMapping("/settings/notifications")
    public String update(@RequestParam(value = "emailEnabled", required = false) boolean emailEnabled,
                          @RequestParam(value = "browserEnabled", required = false) boolean browserEnabled,
                          @RequestParam(value = "pushEnabled", required = false) boolean pushEnabled,
                          @RequestParam(value = "quietStart", required = false) String quietStartStr,
                          @RequestParam(value = "quietEnd", required = false) String quietEndStr,
                          @RequestParam(value = "timeZone", required = false) String timeZone,
                          RedirectAttributes redirectAttributes) {
        try {
            LocalTime start = (quietStartStr != null && !quietStartStr.isBlank()) ? LocalTime.parse(quietStartStr) : null;
            LocalTime end = (quietEndStr != null && !quietEndStr.isBlank()) ? LocalTime.parse(quietEndStr) : null;

            notificationService.updatePreferences(emailEnabled, browserEnabled, pushEnabled, start, end, timeZone);
            redirectAttributes.addFlashAttribute("success", "Settings and notification preferences saved successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Failed to update settings: " + ex.getMessage());
        }
        return "redirect:/settings/notifications";
    }
}
