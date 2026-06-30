package io.github.michdo93.openhab.testsuite;

import io.github.michdo93.openhab.OpenHABClient;
import io.github.michdo93.openhab.OpenHABException;
import io.github.michdo93.openhab.Persistence;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Tests openHAB persistence services: item registration, data existence,
 * and state verification.
 *
 * <p>Mirrors the Python {@code PersistenceTester} class from {@code openhab-test-suite}.
 */
public class PersistenceTester {

    private final Persistence persistenceAPI;

    /**
     * Creates a {@code PersistenceTester} backed by the given client.
     *
     * @param client an authenticated {@link OpenHABClient}
     */
    public PersistenceTester(OpenHABClient client) {
        this.persistenceAPI = new Persistence(client);
    }

    /**
     * Checks whether an item is registered in the given persistence service.
     *
     * @param serviceId persistence service ID (e.g. {@code "rrd4j"})
     * @param itemName  item name
     * @return {@code true} if the item appears in the service's item list
     */
    public boolean isItemPersisted(String serviceId, String itemName) {
        try {
            Object raw = persistenceAPI.getItems(serviceId);
            JSONArray items = parseArray(raw);
            for (int i = 0; i < items.length(); i++) {
                Object entry = items.get(i);
                if (entry instanceof JSONObject) {
                    if (itemName.equals(((JSONObject) entry).optString("name"))) return true;
                } else if (itemName.equals(entry.toString())) {
                    return true;
                }
            }
        } catch (OpenHABException e) {
            System.err.println("Error checking persistence for '" + itemName
                + "': " + e.getMessage());
        }
        return false;
    }

    /**
     * Checks whether historical data exists for an item within a time range.
     *
     * @param serviceId persistence service ID
     * @param itemName  item name
     * @param startTime start timestamp (ISO-8601)
     * @param endTime   end timestamp (ISO-8601)
     * @return {@code true} if at least one data point exists in the range
     */
    public boolean hasDataInRange(String serviceId, String itemName,
                                  String startTime, String endTime) {
        try {
            Object raw = persistenceAPI.getItemPersistenceData(
                itemName, serviceId, startTime, endTime, null, null, null);
            JSONObject data = parseObject(raw);
            JSONArray  entries = data != null ? data.optJSONArray("data") : null;
            return entries != null && entries.length() > 0;
        } catch (OpenHABException e) {
            System.err.println("Error reading persistence data for '" + itemName
                + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks whether the most recently persisted value matches the expected state.
     *
     * @param serviceId     persistence service ID
     * @param itemName      item name
     * @param expectedState expected state string
     * @return {@code true} if the last persisted entry matches {@code expectedState}
     */
    public boolean checkLastPersistedState(String serviceId, String itemName,
                                           String expectedState) {
        try {
            Object raw = persistenceAPI.getItemPersistenceData(
                itemName, serviceId, null, null, null, null, null);
            JSONObject data = parseObject(raw);
            JSONArray  entries = data != null ? data.optJSONArray("data") : null;
            if (entries == null || entries.length() == 0) return false;
            JSONObject last = entries.optJSONObject(entries.length() - 1);
            return last != null && expectedState.equals(last.optString("state"));
        } catch (OpenHABException e) {
            System.err.println("Error reading last persisted state for '" + itemName
                + "': " + e.getMessage());
            return false;
        }
    }

    private static JSONObject parseObject(Object raw) {
        if (raw == null) return null;
        if (raw instanceof JSONObject) return (JSONObject) raw;
        try { return new JSONObject(raw.toString()); }
        catch (Exception e) { return null; }
    }

    private static JSONArray parseArray(Object raw) {
        if (raw == null) return new JSONArray();
        if (raw instanceof JSONArray) return (JSONArray) raw;
        try { return new JSONArray(raw.toString()); }
        catch (Exception e) { return new JSONArray(); }
    }
}