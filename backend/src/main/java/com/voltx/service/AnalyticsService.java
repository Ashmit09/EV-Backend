package com.voltx.service;

import com.voltx.dto.response.AdminOverviewResponse;
import com.voltx.dto.response.CustomerAnalyticsResponse;
import com.voltx.dto.response.EnergyAnalyticsResponse;
import com.voltx.dto.response.RevenueAnalyticsResponse;

public interface AnalyticsService {

    CustomerAnalyticsResponse customer(Long userId);

    AdminOverviewResponse overview();

    RevenueAnalyticsResponse revenue(int months);

    EnergyAnalyticsResponse energy(int months);
}
