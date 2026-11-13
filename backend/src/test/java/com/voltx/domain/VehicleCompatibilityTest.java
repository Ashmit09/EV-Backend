package com.voltx.domain;

import com.voltx.TestFixtures;
import com.voltx.entity.ChargingStation;
import com.voltx.entity.ChargingUnit;
import com.voltx.entity.ElectricCar;
import com.voltx.entity.User;
import com.voltx.entity.Vehicle;
import com.voltx.enums.ConnectorType;
import com.voltx.enums.PricingPlan;
import com.voltx.exception.BusinessRuleViolationException;
import com.voltx.exception.IncompatibleConnectorException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Inheritance + runtime polymorphism: each Vehicle subclass applies its own charging rule. */
class VehicleCompatibilityTest {

    private final User owner = TestFixtures.customer();
    private final ChargingStation station = TestFixtures.station24h();
    private final ChargingUnit dc60 = TestFixtures.unit(station, "DC-60", ConnectorType.CCS2, "60", PricingPlan.STANDARD);
    private final ChargingUnit dc120 = TestFixtures.unit(station, "DC-120", ConnectorType.CCS2, "120", PricingPlan.STANDARD);
    private final ChargingUnit ac22 = TestFixtures.unit(station, "AC-22", ConnectorType.TYPE_2, "22", PricingPlan.STANDARD);
    private final ChargingUnit chademo = TestFixtures.unit(station, "CHA-50", ConnectorType.CHADEMO, "50", PricingPlan.STANDARD);

    @Test
    void connectorMismatchIsRejected() {
        Vehicle car = TestFixtures.nexon(owner);
        assertThatThrownBy(() -> car.checkCompatibility(chademo))
                .isInstanceOf(IncompatibleConnectorException.class)
                .hasMessageContaining("CHAdeMO");
    }

    @Test
    void sameCallBehavesDifferentlyPerSubclass() {
        // All three are held in a List<Vehicle>; the JVM dispatches to each subclass at runtime.
        List<Vehicle> vehicles = List.of(TestFixtures.nexon(owner), TestFixtures.scooter(owner), TestFixtures.bike(owner));
        List<Boolean> canUseDc120 = vehicles.stream().map(v -> v.findCompatibilityIssue(dc120).isEmpty()).toList();
        assertThat(canUseDc120).containsExactly(true, false, false);
    }

    @Test
    void scooterRejectsEveryDcChargerEvenWithMatchingConnector() {
        Vehicle scooter = TestFixtures.scooter(owner); // supports CCS2 connector physically
        assertThatThrownBy(() -> scooter.checkCompatibility(dc60))
                .isInstanceOf(IncompatibleConnectorException.class)
                .hasMessageContaining("AC charging only");
        assertThatCode(() -> scooter.checkCompatibility(ac22)).doesNotThrowAnyException();
    }

    @Test
    void bikeAcceptsFastButNotUltraFast() {
        Vehicle bike = TestFixtures.bike(owner);
        assertThatCode(() -> bike.checkCompatibility(dc60)).doesNotThrowAnyException();
        assertThatThrownBy(() -> bike.checkCompatibility(dc120)).hasMessageContaining("ultra-fast");
    }

    @Test
    void acOnlyCarCannotUseDc() {
        Vehicle acCar = new ElectricCar(owner, "Tata", "Tiago EV", "KA02CD1111", new BigDecimal("24"),
                new BigDecimal("7.2"), 20, Set.of(ConnectorType.CCS2, ConnectorType.TYPE_2));
        assertThatThrownBy(() -> acCar.checkCompatibility(dc60)).hasMessageContaining("only supports AC");
    }

    @Test
    void carStopsAt80OnDcButNotOnAc() {
        Vehicle car = TestFixtures.nexon(owner);
        assertThat(car.defaultTargetPercentage(dc60)).isEqualTo(80);
        assertThat(car.defaultTargetPercentage(ac22)).isEqualTo(100);
        assertThat(TestFixtures.scooter(owner).defaultTargetPercentage(ac22)).isEqualTo(100);
    }

    @Test
    void effectivePowerIsLimitedByTheWeakerSide() {
        assertThat(TestFixtures.nexon(owner).effectiveChargingPower(new BigDecimal("120"))).isEqualByComparingTo("50");
        assertThat(TestFixtures.nexon(owner).effectiveChargingPower(new BigDecimal("22"))).isEqualByComparingTo("22");
    }

    @Test
    void subclassSpecificCapacityLimitsAreEnforced() {
        assertThatThrownBy(() -> new ElectricCar(owner, "X", "Y", "KA01ZZ0001", new BigDecimal("5"),
                new BigDecimal("7"), 10, Set.of(ConnectorType.TYPE_2)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("between 15 and 200");
    }

    @Test
    void batteryLevelIsValidated() {
        Vehicle car = TestFixtures.nexon(owner);
        assertThatThrownBy(() -> car.updateBatteryLevel(101)).isInstanceOf(BusinessRuleViolationException.class);
        car.updateBatteryLevel(75);
        assertThat(car.getCurrentBatteryPercentage()).isEqualTo(75);
    }

    @Test
    void registrationNumberIsNormalised() {
        Vehicle car = new ElectricCar(owner, "MG", "ZS", "mh 02-fz 1209", new BigDecimal("50"),
                new BigDecimal("76"), 10, Set.of(ConnectorType.CCS2));
        assertThat(car.getRegistrationNumber()).isEqualTo("MH02FZ1209");
    }
}
