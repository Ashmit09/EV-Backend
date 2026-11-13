package com.voltx;

import com.voltx.entity.ChargingStation;
import com.voltx.entity.ChargingUnit;
import com.voltx.entity.ElectricBike;
import com.voltx.entity.ElectricCar;
import com.voltx.entity.ElectricScooter;
import com.voltx.entity.User;
import com.voltx.enums.ConnectorType;
import com.voltx.enums.PricingPlan;
import com.voltx.enums.Role;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Set;

/** Plain-Java object builders for unit tests (no Spring, no database). */
public final class TestFixtures {

    private TestFixtures() {
    }

    public static User customer() {
        return new User("Test Customer", "test@voltx.in", "hash", "9999999999", Role.CUSTOMER);
    }

    public static ChargingStation station24h() {
        return new ChargingStation("Test Hub", "1 Test Road", "Bengaluru", new BigDecimal("12.9"),
                new BigDecimal("77.6"), null, null, true);
    }

    public static ChargingStation stationOpen(int fromHour, int toHour) {
        return new ChargingStation("Day Hub", "2 Test Road", "Pune", new BigDecimal("18.5"),
                new BigDecimal("73.8"), LocalTime.of(fromHour, 0), LocalTime.of(toHour, 0), false);
    }

    public static ChargingUnit unit(ChargingStation station, String code, ConnectorType type, String kw, PricingPlan plan) {
        return new ChargingUnit(station, code, type, new BigDecimal(kw), new BigDecimal("20.00"), plan);
    }

    public static ElectricCar nexon(User owner) {
        return new ElectricCar(owner, "Tata", "Nexon EV", "KA01AB1234", new BigDecimal("40.5"),
                new BigDecimal("50"), 30, Set.of(ConnectorType.CCS2, ConnectorType.TYPE_2));
    }

    public static ElectricScooter scooter(User owner) {
        return new ElectricScooter(owner, "Ather", "450X", "KA05XY4321", new BigDecimal("3.7"),
                new BigDecimal("3.3"), 40, Set.of(ConnectorType.TYPE_2, ConnectorType.CCS2));
    }

    public static ElectricBike bike(User owner) {
        return new ElectricBike(owner, "Ultraviolette", "F77", "TS09EV3344", new BigDecimal("10.3"),
                new BigDecimal("10"), 50, Set.of(ConnectorType.CCS2));
    }
}
