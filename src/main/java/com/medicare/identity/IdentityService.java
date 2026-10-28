package com.medicare.identity;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;

@Service
public class IdentityService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public IdentityService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegistrationRequest request) {
        String normalizedEmail = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("An account already exists for this email.");
        }

        User user = new User();
        try {
            user.setTimeZone(ZoneId.of(request.getTimeZone()).getId());
        } catch (DateTimeException ex) {
            throw new IllegalArgumentException("Choose a valid time zone.");
        }
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setRole(Role.PATIENT);
        return userRepository.save(user);
    }
}
