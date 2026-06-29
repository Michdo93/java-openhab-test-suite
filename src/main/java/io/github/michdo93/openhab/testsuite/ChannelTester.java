package io.github.michdo93.openhab.testsuite;

import io.github.michdo93.openhab.Links;
import io.github.michdo93.openhab.OpenHABClient;
import io.github.michdo93.openhab.OpenHABException;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Tests openHAB item-channel link relationships.
 *
 * <p>Mirrors the Python {@code ChannelTester} class from {@code openhab-test-suite}.
 */
public class ChannelTester {

    private final Links linksAPI;

    /**
     * Creates a {@code ChannelTester} backed by the given client.
     *
     * @param client an authenticated {@link OpenHABClient}
     */
    public ChannelTester(OpenHABClient client) {
        this.linksAPI = new Links(client);
    }

    /**
     * Checks whether an item is linked to a specific channel.
     *
     * @param itemName   item name
     * @param channelUID channel UID
     * @return {@code true} if the link exists and the {@code itemName} field matches
     */
    public boolean isItemLinkedToChannel(String itemName, String channelUID) {
        try {
            Object raw = linksAPI.getLink(itemName, channelUID);
            JSONObject link = parseObject(raw);
            return link != null && itemName.equals(link.optString("itemName"));
        } catch (OpenHABException e) {
            System.err.println("Error checking link '" + itemName + "' → '" + channelUID
                + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns all channel links for a given item.
     *
     * @param itemName item name
     * @return {@link JSONArray} of link objects (empty array on error)
     */
    public JSONArray getLinksForItem(String itemName) {
        try {
            Object raw = linksAPI.getLinks(itemName, null);
            return parseArray(raw);
        } catch (OpenHABException e) {
            System.err.println("Error reading links for '" + itemName + "': " + e.getMessage());
            return new JSONArray();
        }
    }

    /**
     * Checks whether an item is linked to at least one channel.
     *
     * @param itemName item name
     * @return {@code true} if at least one link exists
     */
    public boolean isItemLinkedToAnyChannel(String itemName) {
        return getLinksForItem(itemName).length() > 0;
    }

    /**
     * Checks whether there are any orphaned links
     * (links pointing to non-existent channels).
     *
     * @return {@code true} if orphaned links exist
     */
    public boolean hasOrphanedLinks() {
        try {
            Object raw = linksAPI.getOrphanLinks();
            return parseArray(raw).length() > 0;
        } catch (OpenHABException e) {
            System.err.println("Error reading orphan links: " + e.getMessage());
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
