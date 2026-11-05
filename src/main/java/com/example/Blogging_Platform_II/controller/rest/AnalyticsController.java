package com.example.Blogging_Platform_II.controller.rest;

import static com.example.Blogging_Platform_II.controller.rest.ApiEnvelope.ok;

import com.example.Blogging_Platform_II.model.Dashboard;
import com.example.Blogging_Platform_II.service.AnalyticsService;
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
    ApiEnvelope<Dashboard> dashboard() {
        return ok("Dashboard calculated.", analytics.dashboard());
    }
}
