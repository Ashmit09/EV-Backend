package com.voltx.simulator;

import com.voltx.config.VoltXProperties;
import com.voltx.entity.ChargingSession;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Bridges real time and simulated time, and chooses the right {@link ChargingSimulator}.
 * <p>
 * The engine is the only place that knows about the time multiplier. The session service
 * asks "what is the progress of this session now?" and gets a {@link ChargingProgress} back,
 * keeping simulation separate from session business rules.
 */
@Component
public class ChargingSimulationEngine {

    private final List<ChargingSimulator> simulators;
    private final long timeMultiplier;

    public ChargingSimulationEngine(List<ChargingSimulator> simulators, VoltXProperties properties) {
        this.simulators = List.copyOf(simulators);
        this.timeMultiplier = Math.max(1, properties.simulation().timeMultiplier());
    }

    public ChargingProgress progressOf(ChargingSession session, LocalDateTime now) {
        Duration realElapsed = Duration.between(session.getStartTime(), now);
        if (realElapsed.isNegative()) {
            realElapsed = Duration.ZERO;
        }
        return simulate(contextOf(session), realElapsed.multipliedBy(timeMultiplier));
    }

    public ChargingProgress simulate(ChargingContext context, Duration simulatedElapsed) {
        return simulatorFor(context).simulate(context, simulatedElapsed);
    }

    public ChargingSimulator simulatorFor(ChargingContext context) {
        return simulators.stream()
                .filter(simulator -> simulator.supports(context))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No charging simulator supports this context"));
    }

    public static ChargingContext contextOf(ChargingSession session) {
        return new ChargingContext(
                session.getVehicle().getBatteryCapacityKwh(),
                session.getInitialBatteryPercentage(),
                session.getTargetBatteryPercentage(),
                session.getEffectivePowerKw(),
                session.getChargingUnit().isDcFastCharger());
    }

    public long getTimeMultiplier() {
        return timeMultiplier;
    }
}
