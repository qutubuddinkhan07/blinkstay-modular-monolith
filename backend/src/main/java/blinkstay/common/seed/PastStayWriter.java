package blinkstay.common.seed;

import java.util.UUID;

/**
 * Implemented by the booking module. Returns the new booking's id.
 */
public interface PastStayWriter {
    UUID createCompletedStay(PastStay stay);
}