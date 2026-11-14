package io.github.michdo93.openhab.testsuite;

import static io.github.michdo93.openhab.testsuite.Support.asObject;
import static io.github.michdo93.openhab.testsuite.Support.isError;

import io.github.michdo93.openhab.Json;
import io.github.michdo93.openhab.OpenHABClient;
import io.github.michdo93.openhab.Things;

/** Tests for openHAB things – the counterpart of {@code ThingTester} in openhab-test-suite (Python). */
public class ThingTester {

    private final OpenHABClient client;
    private final Things thingsAPI;

    public ThingTester(OpenHABClient client) {
        this.client = client;
        this.thingsAPI = new Things(client);
    }

    /** Status of a thing ("ONLINE", "OFFLINE", …) or "UNKNOWN" if it cannot be determined. */
    public String getThingStatus(String thingUID) {
        Object status = Json.get(asObject(thingsAPI.getThing(thingUID)), "statusInfo", "status");
        return status == null ? "UNKNOWN" : String.valueOf(status);
    }

    public boolean isThingStatus(String thingUID, String statusToCheck) {
        return getThingStatus(thingUID).equals(statusToCheck);
    }

    public boolean isThingOnline(String thingUID) {
        return isThingStatus(thingUID, "ONLINE");
    }

    public boolean isThingOffline(String thingUID) {
        return isThingStatus(thingUID, "OFFLINE");
    }

    public boolean isThingPending(String thingUID) {
        return isThingStatus(thingUID, "PENDING");
    }

    public boolean isThingUnknown(String thingUID) {
        return isThingStatus(thingUID, "UNKNOWN");
    }

    public boolean isThingUninitialized(String thingUID) {
        return isThingStatus(thingUID, "UNINITIALIZED");
    }

    public boolean isThingError(String thingUID) {
        return isThingStatus(thingUID, "ERROR");
    }

    private boolean setEnabled(String thingUID, boolean enabled) {
        Object result = thingsAPI.setThingStatus(thingUID, enabled);
        if (isError(result)) {
            System.err.println("Error " + (enabled ? "enabling" : "disabling") + " thing " + thingUID + ": "
                    + Json.error(result));
            return false;
        }
        System.out.println("Thing " + thingUID + " was successfully " + (enabled ? "enabled" : "disabled") + ".");
        return true;
    }

    public boolean enableThing(String thingUID) {
        return setEnabled(thingUID, true);
    }

    public boolean disableThing(String thingUID) {
        return setEnabled(thingUID, false);
    }

    public OpenHABClient client() {
        return client;
    }
}
