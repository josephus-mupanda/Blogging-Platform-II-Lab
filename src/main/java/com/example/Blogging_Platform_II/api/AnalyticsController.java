
package com.example.Blogging_Platform_II.api;

import static com.example.Blogging_Platform_II.api.ApiEnvelope.ok;

import com.example.Blogging_Platform_II.model.Blog.Dashboard;
import com.example.Blogging_Platform_II.service.AnalyticsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AnalyticsController {
    private final AnalyticsService analytics;

    public AnalyticsController(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    @GetMapping("/dashboard")
    @SecurityRequirement(name = "basicAuth")
    ApiEnvelope<Dashboard> dashboard() {
        return ok("Dashboard calculated.", analytics.dashboard());
    }
}
