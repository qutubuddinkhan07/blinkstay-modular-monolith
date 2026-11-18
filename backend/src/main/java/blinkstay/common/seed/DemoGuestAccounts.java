package blinkstay.common.seed;

import java.util.List;

/**
 * Implemented by the auth module. Creates missing guests, returns existing ones as they are.
 */
public interface DemoGuestAccounts {
    List<DemoGuest> ensureGuests(List<NewDemoGuest> guests);
}