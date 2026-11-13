package com.voltx.dto.response;

import java.math.BigDecimal;

/** One month on a time-series chart. */
public record MonthlyPoint(String month, BigDecimal energyKwh, BigDecimal revenue, BigDecimal collected,
                           long sessions) {
}
