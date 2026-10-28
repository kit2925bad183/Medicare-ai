package com.medicare.assistant;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Controller
public class AssistantController {

    private final RuleBasedAssistant assistant;
    private final AssistantActionService actionService;

    public AssistantController(RuleBasedAssistant assistant, AssistantActionService actionService) {
        this.assistant = assistant;
        this.actionService = actionService;
    }

    @GetMapping("/assistant")
    public String index() {
        return "assistant/index";
    }

    @PostMapping("/assistant")
    public String ask(@RequestParam("command") String command, HttpSession session, Model model) {
        RuleBasedAssistant.AssistantResult result = assistant.respond(command);
        model.addAttribute("response", result.message());
        if (result.proposal() != null) {
            String token = UUID.randomUUID().toString();
            session.setAttribute("assistant_proposal", result.proposal());
            session.setAttribute("assistant_proposal_at", Instant.now());
            session.setAttribute("assistant_token", token);
            model.addAttribute("proposal", result.proposal());
            model.addAttribute("confirmationToken", token);
        } else {
            session.removeAttribute("assistant_proposal");
            session.removeAttribute("assistant_proposal_at");
            session.removeAttribute("assistant_token");
        }
        return "assistant/index";
    }

    @PostMapping("/assistant/confirm")
    public String confirm(@RequestParam(value = "token", required = false) String token,
                          HttpSession session, Model model) {
        Object proposal = session.getAttribute("assistant_proposal");
        Object at = session.getAttribute("assistant_proposal_at");
        Object savedToken = session.getAttribute("assistant_token");

        if (savedToken != null && token != null && !Objects.equals(savedToken, token)) {
            model.addAttribute("response", "Invalid confirmation token.");
            return "assistant/index";
        }

        if (proposal instanceof RuleBasedAssistant.ProposedSchedule p) {
            if (at instanceof Instant when && when.plusSeconds(300).isBefore(Instant.now())) {
                session.removeAttribute("assistant_proposal");
                session.removeAttribute("assistant_proposal_at");
                session.removeAttribute("assistant_token");
                model.addAttribute("response", "The proposal expired. Ask the assistant again to preview it.");
                return "assistant/index";
            }
            try {
                actionService.confirmDaily(p);
                session.removeAttribute("assistant_proposal");
                session.removeAttribute("assistant_proposal_at");
                session.removeAttribute("assistant_token");
                return "redirect:/medicines";
            } catch (Exception ex) {
                model.addAttribute("response", ex.getMessage());
                return "assistant/index";
            }
        }
        model.addAttribute("response", "There is no pending proposal to confirm.");
        return "assistant/index";
    }

    @PostMapping("/assistant/cancel")
    public String cancel(HttpSession session) {
        session.removeAttribute("assistant_proposal");
        session.removeAttribute("assistant_proposal_at");
        session.removeAttribute("assistant_token");
        return "redirect:/assistant";
    }
}
