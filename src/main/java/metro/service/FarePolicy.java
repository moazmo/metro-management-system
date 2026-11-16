package metro.service;

/**
 * A way of deciding the full fare of a journey from its number of stops.
 *
 * It is an interface so that the rest of the program does not depend on how
 * the fare is decided. Today there is one implementation, TieredFarePolicy;
 * a different pricing scheme could be added as another class without
 * changing the code that sells tickets.
 */
public interface FarePolicy {

    /** The full (undiscounted) fare of a journey of the given number of stops. */
    double baseFare(int stops);
}
