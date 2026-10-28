package com.medicare.config;

import com.medicare.identity.CustomUserDetailsService;
import com.medicare.identity.IdentityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
    com.medicare.identity.IdentityController.class,
    CsrfProtectionIntegrationTest.TestViews.LogoutFormController.class
}, excludeAutoConfiguration = JacksonAutoConfiguration.class)
@Import({SecurityConfig.class, CsrfProtectionIntegrationTest.TestViews.class})
class CsrfProtectionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IdentityService identityService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void browserFormsRenderCsrfTokens() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));

        mockMvc.perform(get("/register"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));

        mockMvc.perform(get("/test/logout-form").with(user("patient")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    @Test
    void registrationRejectsMissingCsrfTokenAndAcceptsValidToken() throws Exception {
        mockMvc.perform(post("/register")
                .param("firstName", "Taylor")
                .param("lastName", "Patient")
                .param("email", "taylor@example.test")
                .param("password", "StrongPassword123!")
                .param("timeZone", "UTC"))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/register")
                .with(csrf())
                .param("firstName", "Taylor")
                .param("lastName", "Patient")
                .param("email", "taylor@example.test")
                .param("password", "StrongPassword123!")
                .param("timeZone", "UTC"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    void loginRejectsMissingCsrfTokenAndProcessesRequestWithValidToken() throws Exception {
        doThrow(new UsernameNotFoundException("No matching user"))
            .when(customUserDetailsService).loadUserByUsername(anyString());

        mockMvc.perform(post("/login")
                .param("username", "missing@example.test")
                .param("password", "not-a-password"))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/login")
                .with(csrf())
                .param("username", "missing@example.test")
                .param("password", "not-a-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void logoutRejectsMissingCsrfTokenAndLogsOutWithValidToken() throws Exception {
        mockMvc.perform(post("/logout").with(user("patient")))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/logout").with(user("patient")).with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?logout"));
    }

    @TestConfiguration
    static class TestViews {
        @Controller
        static class LogoutFormController {
            @GetMapping("/test/logout-form")
            String logoutForm() {
                return "test/logout-form";
            }
        }
    }
}
