package com.medicare.reporting;

import com.medicare.common.CurrentUserService;
import com.medicare.identity.User;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class ReportController {

    private final ReportingService reportingService;
    private final CurrentUserService currentUserService;

    public ReportController(ReportingService reportingService, CurrentUserService currentUserService) {
        this.reportingService = reportingService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        User user = currentUserService.getCurrentUser();
        model.addAttribute("adherence", reportingService.getAdherenceSummary(user.getId()));
        model.addAttribute("doseLogs", reportingService.getRecentDoseLogs(user.getId()));
        model.addAttribute("deliveries", reportingService.getRecentDeliveries(user.getId()));
        return "reports/index";
    }

    @GetMapping("/reports/export")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> exportData() {
        User user = currentUserService.getCurrentUser();
        Map<String, Object> data = reportingService.exportUserData(user.getId());

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"medicare-health-export.json\"")
            .contentType(MediaType.APPLICATION_JSON)
            .body(data);
    }
}
