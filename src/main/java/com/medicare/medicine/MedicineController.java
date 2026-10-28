package com.medicare.medicine;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MedicineController {

    private final ReminderService reminderService;

    public MedicineController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @GetMapping("/medicines")
    public String medicines(Model model) {
        model.addAttribute("pageTitle", "Medicine reminders");
        model.addAttribute("schedules", reminderService.schedules());
        return "medicine/list";
    }

    @GetMapping("/medicines/new")
    public String newMedicine(Model model) {
        MedicineScheduleRequest request = new MedicineScheduleRequest();
        request.setStartDate(reminderService.todayInUserTimeZone());
        model.addAttribute("scheduleRequest", request);
        model.addAttribute("weekdays", java.time.DayOfWeek.values());
        model.addAttribute("pageTitle", "Add reminder");
        return "medicine/form";
    }

    @PostMapping("/medicines")
    public String createMedicine(@Valid @ModelAttribute("scheduleRequest") MedicineScheduleRequest request,
                                 BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("weekdays", java.time.DayOfWeek.values());
            model.addAttribute("pageTitle", "Add reminder");
            return "medicine/form";
        }
        try {
            reminderService.create(request);
            return "redirect:/medicines?created";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("scheduleError", ex.getMessage());
            model.addAttribute("weekdays", java.time.DayOfWeek.values());
            model.addAttribute("pageTitle", "Add reminder");
            return "medicine/form";
        }
    }

    @PostMapping("/medicines/{id}/deactivate")
    public String deactivate(@PathVariable long id) {
        reminderService.deactivate(id);
        return "redirect:/medicines?cancelled";
    }

    @PostMapping("/notifications/{id}/dismiss")
    public String dismissNotification(@PathVariable long id) {
        reminderService.dismissNotification(id);
        return "redirect:/dashboard";
    }

    @PostMapping("/doses/{id}/action")
    public String recordDoseAction(@PathVariable long id,
                                   @RequestParam("action") String action,
                                   @RequestParam(value = "notes", required = false) String notes) {
        reminderService.recordDoseAction(id, action, notes);
        return "redirect:/dashboard?doseRecorded";
    }
}
