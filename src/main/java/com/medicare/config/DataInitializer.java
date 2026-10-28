package com.medicare.config;

import com.medicare.appointment.Appointment;
import com.medicare.appointment.AppointmentRepository;
import com.medicare.appointment.Doctor;
import com.medicare.appointment.DoctorRepository;
import com.medicare.identity.Role;
import com.medicare.identity.User;
import com.medicare.identity.UserRepository;
import com.medicare.medicine.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicineScheduleRepository scheduleRepository;
    private final ReminderOccurrenceRepository occurrenceRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           DoctorRepository doctorRepository,
                           AppointmentRepository appointmentRepository,
                           MedicineScheduleRepository scheduleRepository,
                           ReminderOccurrenceRepository occurrenceRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.scheduleRepository = scheduleRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        User demoUser = seedDemoUser();
        List<Doctor> doctors = seedDoctors();
        if (demoUser != null) {
            seedMedicineSchedules(demoUser);
            if (!doctors.isEmpty()) {
                seedAppointment(demoUser, doctors.get(0));
            }
        }
    }

    private User seedDemoUser() {
        if (!userRepository.existsByEmailIgnoreCase("demo@medicare.local")) {
            User demo = new User(
                "demo@medicare.local",
                passwordEncoder.encode("Password123!"),
                "Alex",
                "Smith",
                Role.PATIENT
            );
            User saved = userRepository.save(demo);
            log.info("Seeded demo patient account: demo@medicare.local");
            return saved;
        }
        return userRepository.findByEmailIgnoreCase("demo@medicare.local").orElse(null);
    }

    private List<Doctor> seedDoctors() {
        if (doctorRepository.count() == 0) {
            Doctor doc1 = new Doctor("Dr. Sarah Chen, MD", "Cardiology");
            doc1.setClinicName("Metro Heart & Vascular Center");
            doc1.setClinicAddress("100 Healthcare Ave, Suite 400");
            doc1.setVerified(true);

            Doctor doc2 = new Doctor("Dr. James Wilson, MD", "Internal Medicine");
            doc2.setClinicName("Downtown Community Health");
            doc2.setClinicAddress("250 Central Blvd");
            doc2.setVerified(true);

            Doctor doc3 = new Doctor("Dr. Elena Rostova, MD", "Neurology");
            doc3.setClinicName("City Brain & Spine Institute");
            doc3.setClinicAddress("78 Medical Plaza, West Wing");
            doc3.setVerified(true);

            List<Doctor> saved = doctorRepository.saveAll(List.of(doc1, doc2, doc3));
            log.info("Seeded verified doctor directory");
            return saved;
        }
        return doctorRepository.findAll();
    }

    private void seedMedicineSchedules(User user) {
        if (scheduleRepository.findAllByOwnerIdOrderByLocalTimeAsc(user.getId()).isEmpty()) {
            LocalDate today = LocalDate.now();
            MedicineSchedule med1 = new MedicineSchedule(
                user,
                "Lisinopril 10mg",
                "Take 1 tablet every morning with a full glass of water",
                ScheduleFrequency.DAILY,
                LocalTime.of(8, 0),
                null,
                today,
                "UTC"
            );
            MedicineSchedule med2 = new MedicineSchedule(
                user,
                "Metformin 500mg",
                "Take 1 tablet with evening meal",
                ScheduleFrequency.DAILY,
                LocalTime.of(19, 0),
                null,
                today,
                "UTC"
            );
            List<MedicineSchedule> saved = scheduleRepository.saveAll(List.of(med1, med2));

            // Seed occurrences for today
            Instant due1 = today.atTime(8, 0).toInstant(ZoneOffset.UTC);
            Instant due2 = today.atTime(19, 0).toInstant(ZoneOffset.UTC);

            ReminderOccurrence occ1 = new ReminderOccurrence(saved.get(0), due1, saved.get(0).getId() + ":" + today + ":08:00");
            occ1.setState(OccurrenceState.AVAILABLE);

            ReminderOccurrence occ2 = new ReminderOccurrence(saved.get(1), due2, saved.get(1).getId() + ":" + today + ":19:00");
            occ2.setState(OccurrenceState.AVAILABLE);

            occurrenceRepository.saveAll(List.of(occ1, occ2));
            log.info("Seeded sample medication schedules and today's doses for demo user");
        }
    }

    private void seedAppointment(User user, Doctor doctor) {
        if (appointmentRepository.findAllByPatientIdOrderByAppointmentAtDesc(user.getId()).isEmpty()) {
            Appointment appt = new Appointment(
                user,
                doctor,
                Instant.now().plus(3, ChronoUnit.DAYS),
                "UTC",
                "Quarterly cardiovascular and blood pressure consultation"
            );
            appointmentRepository.save(appt);
            log.info("Seeded upcoming doctor consultation for demo user");
        }
    }
}
