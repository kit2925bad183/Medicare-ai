package com.medicare.appointment;

import com.medicare.common.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final CurrentUserService currentUserService;
    private final DoctorRepository doctorRepository;

    public AppointmentController(AppointmentService appointmentService, CurrentUserService currentUserService,
                                 DoctorRepository doctorRepository) {
        this.appointmentService = appointmentService;
        this.currentUserService = currentUserService;
        this.doctorRepository = doctorRepository;
    }

    @GetMapping("/doctors")
    public String doctors(@RequestParam(value = "specialty", required = false) String specialty, Model model) {
        model.addAttribute("doctors", appointmentService.doctors(specialty));
        model.addAttribute("specialty", specialty);
        return "appointments/doctors";
    }

    @GetMapping("/appointments")
    public String appointments(Model model) {
        model.addAttribute("appointments", appointmentService.mine());
        return "appointments/list";
    }

    @GetMapping("/appointments/new")
    public String newAppointment(@RequestParam("doctorId") Long doctorId, Model model) {
        model.addAttribute("doctor", doctorRepository.findById(doctorId).orElseThrow());
        model.addAttribute("form", new BookForm());
        return "appointments/form";
    }

    @PostMapping("/appointments")
    public String book(@ModelAttribute("form") @Valid BookForm form, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            doctorRepository.findById(form.doctorId).ifPresent(d -> model.addAttribute("doctor", d));
            return "appointments/form";
        }
        try {
            appointmentService.book(new AppointmentService.BookAppointmentRequest(
                form.doctorId, form.appointmentDate, form.appointmentTime, form.reason));
            return "redirect:/appointments";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            doctorRepository.findById(form.doctorId).ifPresent(d -> model.addAttribute("doctor", d));
            return "appointments/form";
        }
    }

    @PostMapping("/appointments/{id}/cancel")
    public String cancel(@PathVariable Long id) {
        appointmentService.cancel(id);
        return "redirect:/appointments";
    }

    public static class BookForm {
        @NotNull
        public Long doctorId;
        @NotNull
        public java.time.LocalDate appointmentDate;
        @NotNull
        public java.time.LocalTime appointmentTime;
        public String reason;
    }
}
