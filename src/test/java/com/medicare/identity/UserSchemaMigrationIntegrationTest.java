package com.medicare.identity;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties =
    "spring.autoconfigure.exclude=org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration"
)
class UserSchemaMigrationIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Flyway flyway;

    @Test
    void migrationCreatesAUsersTableCompatibleWithTheEntity() {
        User user = new User(
            "migration-" + UUID.randomUUID() + "@example.test",
            "encoded-password",
            "Taylor",
            "Patient",
            Role.PATIENT
        );

        User saved = userRepository.saveAndFlush(user);

        assertNotNull(saved.getId());
        User loaded = userRepository.findById(saved.getId()).orElseThrow();
        assertEquals(user.getEmail(), loaded.getEmail());
        assertEquals(Role.PATIENT, loaded.getRole());
        assertEquals("UTC", loaded.getTimeZone());
        assertNotNull(loaded.getCreatedAt());
        assertNotNull(loaded.getUpdatedAt());
    }

    @Test
    void applyingTheMigrationRunnerAgainLeavesTheVersionedSchemaUsable() {
        User user = new User(
            "rerun-" + UUID.randomUUID() + "@example.test",
            "encoded-password",
            "Taylor",
            "Patient",
            Role.PATIENT
        );
        User saved = userRepository.saveAndFlush(user);

        flyway.migrate();

        String latestVersion = flyway.info().all()[flyway.info().all().length - 1].getVersion().getVersion();
        assertEquals(latestVersion, flyway.info().current().getVersion().getVersion());
        assertEquals(user.getEmail(), userRepository.findById(saved.getId()).orElseThrow().getEmail());
    }
}
