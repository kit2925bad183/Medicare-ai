package com.medicare.medicine;

import com.medicare.appointment.AppointmentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final ReminderService reminderService;
    private final AppointmentService appointmentService;

    public DashboardController(ReminderService reminderService, AppointmentService appointmentService) {
        this.reminderService = reminderService;
        this.appointmentService = appointmentService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        var upcoming = reminderService.upcoming();
        var appointments = appointmentService.mine();
        var notifications = reminderService.notifications();
        model.addAttribute("upcoming", upcoming);
        model.addAttribute("appointments", appointments);
        model.addAttribute("notifications", notifications);
        model.addAttribute("upcomingCount", upcoming.size());
        model.addAttribute("appointmentCount", appointments.stream().filter(a -> "BOOKED".equals(a.getStatus())).count());
        model.addAttribute("notificationCount", notifications.size());
        return "dashboard/index";
    }
}
