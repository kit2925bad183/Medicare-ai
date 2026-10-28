package com.medicare.caregiver;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CaregiverController {

    private final CaregiverService caregiverService;

    public CaregiverController(CaregiverService caregiverService) {
        this.caregiverService = caregiverService;
    }

    @GetMapping("/caregivers")
    public String caregivers(Model model) {
        model.addAttribute("invited", caregiverService.invited());
        model.addAttribute("pending", caregiverService.pending());
        model.addAttribute("authorizedPatients", caregiverService.authorizedPatients());
        return "caregivers/index";
    }

    @PostMapping("/caregivers/invite")
    public String invite(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        try {
            caregiverService.invite(email);
            redirectAttributes.addFlashAttribute("success", "Caregiver invitation sent to " + email);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/caregivers";
    }

    @PostMapping("/caregivers/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            caregiverService.approve(id);
            redirectAttributes.addFlashAttribute("success", "Caregiver invitation accepted.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/caregivers";
    }

    @PostMapping("/caregivers/{id}/revoke")
    public String revoke(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            caregiverService.revoke(id);
            redirectAttributes.addFlashAttribute("success", "Caregiver relationship revoked.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/caregivers";
    }

    @GetMapping("/caregivers/patient/{id}")
    public String viewPatientData(@PathVariable Long id, Model model) {
        model.addAttribute("patientId", id);
        model.addAttribute("schedules", caregiverService.getPatientSchedules(id));
        model.addAttribute("appointments", caregiverService.getPatientAppointments(id));
        model.addAttribute("doses", caregiverService.getPatientUpcomingDoses(id));
        return "caregivers/patient";
    }
}
