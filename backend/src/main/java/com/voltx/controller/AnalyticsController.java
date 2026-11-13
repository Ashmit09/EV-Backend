package com.voltx.controller;

import com.voltx.dto.response.CustomerAnalyticsResponse;
import com.voltx.security.VoltXUserDetails;
import com.voltx.service.AnalyticsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/api/analytics/customer")
    public CustomerAnalyticsResponse customer(@AuthenticationPrincipal VoltXUserDetails me) {
        return analyticsService.customer(me.getId());
    }
}
