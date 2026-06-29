package io.github.michdo93.openhab.testsuite;

import io.github.michdo93.openhab.OpenHABClient;
import io.github.michdo93.openhab.OpenHABException;
import io.github.michdo93.openhab.Things;

import org.json.JSONObject;

/**
 * Tests openHAB Thing status and enable/disable operations.
 *
 * <p>Mirrors the Python {@code ThingTester} class from {@code openhab-test-suite}.
 */
public class ThingTester {

    private final Things thingsAPI;

    /**
     * Creates a {@code ThingTester} backed by the given client.
     *
     * @param client an authenticated {@link OpenHABClient}
     */
    public ThingTester(OpenHABClient client) {
        this.thingsAPI = new Things(client);
    }

    /**
     * Retrieves the status string of a Thing (e.g. {@code "ONLINE"}, {@code "OFFLINE"}).
     *
     * @param thingUID thing UID
     * @return status string, or {@code "UNKNOWN"} if not determinable
     */
    public String getThingStatus(String thingUID) {
        try {
            Object raw = thingsAPI.getThing(thingUID);
            if (raw == null) return "UNKNOWN";
            JSONObject thing      = parseObject(raw);
            JSONObject statusInfo = thing != null ? thing.optJSONObject("statusInfo") : null;
            return statusInfo != null ? statusInfo.optString("status", "UNKNOWN") : "UNKNOWN";
        } catch (OpenHABException e) {
            System.err.println("Error reading status of '" + thingUID + "': " + e.getMessage());
            return "UNKNOWN";
        }
    }

    /**
     * Checks whether a Thing has the specified status.
     *
     * @param thingUID      thing UID
     * @param statusToCheck expected status (case-sensitive)
     * @return {@code true} if the Thing's status matches {@code statusToCheck}
     */
    public boolean isThingStatus(String thingUID, String statusToCheck) {
        return getThingStatus(thingUID).equals(statusToCheck);
    }

    /** @return {@code true} if the Thing is {@code ONLINE}. */
    public boolean isThingOnline(String thingUID) {
        return isThingStatus(thingUID, "ONLINE");
    }

    /** @return {@code true} if the Thing is {@code OFFLINE}. */
    public boolean isThingOffline(String thingUID) {
        return isThingStatus(thingUID, "OFFLINE");
    }

    /** @return {@code true} if the Thing is {@code PENDING}. */
    public boolean isThingPending(String thingUID) {
        return isThingStatus(thingUID, "PENDING");
    }

    /** @return {@code true} if the Thing is {@code UNKNOWN}. */
    public boolean isThingUnknown(String thingUID) {
        return isThingStatus(thingUID, "UNKNOWN");
    }

    /** @return {@code true} if the Thing is {@code UNINITIALIZED}. */
    public boolean isThingUninitialized(String thingUID) {
        return isThingStatus(thingUID, "UNINITIALIZED");
    }

    /** @return {@code true} if the Thing is in {@code ERROR} state. */
    public boolean isThingError(String thingUID) {
        return isThingStatus(thingUID, "ERROR");
    }

    /**
     * Enables a Thing.
     *
     * @param thingUID thing UID
     * @return {@code true} if the Thing was successfully enabled
     */
    public boolean enableThing(String thingUID) {
        try {
            thingsAPI.setThingStatus(thingUID, true);
            System.out.println("Thing '" + thingUID + "' enabled successfully.");
            return true;
        } catch (OpenHABException e) {
            System.err.println("Error enabling '" + thingUID + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Disables a Thing.
     *
     * @param thingUID thing UID
     * @return {@code true} if the Thing was successfully disabled
     */
    public boolean disableThing(String thingUID) {
        try {
            thingsAPI.setThingStatus(thingUID, false);
            System.out.println("Thing '" + thingUID + "' disabled successfully.");
            return true;
        } catch (OpenHABException e) {
            System.err.println("Error disabling '" + thingUID + "': " + e.getMessage());
            return false;
        }
    }

    private static JSONObject parseObject(Object raw) {
        if (raw == null) return null;
        if (raw instanceof JSONObject) return (JSONObject) raw;
        try { return new JSONObject(raw.toString()); }
        catch (Exception e) { return null; }
    }
}
