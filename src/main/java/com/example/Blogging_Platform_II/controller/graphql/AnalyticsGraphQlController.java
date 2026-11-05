package com.example.Blogging_Platform_II.controller.graphql;

import com.example.Blogging_Platform_II.model.Dashboard;
import com.example.Blogging_Platform_II.service.AnalyticsService;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class AnalyticsGraphQlController {
    private final AnalyticsService analytics;

    public AnalyticsGraphQlController(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    @QueryMapping
    public Dashboard dashboard() {
        return analytics.dashboard();
    }
}
