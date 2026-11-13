package com.voltx.simulator;

import java.time.Duration;

/**
 * Abstraction over "how does a battery fill up over time".
 * <p>
 * The session service never knows which physics model is used; it asks the
 * {@link ChargingSimulationEngine}, which picks an implementation at runtime
 * (runtime polymorphism). A new model (e.g. temperature-aware) is just one more class.
 */
public interface ChargingSimulator {

    /** Whether this model applies to the given charging run (AC vs DC). */
    boolean supports(ChargingContext context);

    /** Battery state after {@code simulatedElapsed} of charging. */
    ChargingProgress simulate(ChargingContext context, Duration simulatedElapsed);

    String name();
}
