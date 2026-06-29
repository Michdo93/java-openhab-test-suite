package io.github.michdo93.openhab.testsuite;

import io.github.michdo93.openhab.OpenHABClient;
import io.github.michdo93.openhab.OpenHABException;
import io.github.michdo93.openhab.Sitemaps;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Tests openHAB sitemaps: existence and item references.
 *
 * <p>Mirrors the Python {@code SitemapTester} class from {@code openhab-test-suite}.
 */
public class SitemapTester {

    private final Sitemaps sitemapsAPI;

    /**
     * Creates a {@code SitemapTester} backed by the given client.
     *
     * @param client an authenticated {@link OpenHABClient}
     */
    public SitemapTester(OpenHABClient client) {
        this.sitemapsAPI = new Sitemaps(client);
    }

    /**
     * Checks whether a sitemap with the given name exists.
     *
     * @param sitemapName sitemap name
     * @return {@code true} if the sitemap exists
     */
    public boolean doesSitemapExist(String sitemapName) {
        try {
            Object raw = sitemapsAPI.getSitemaps();
            JSONArray sitemaps = parseArray(raw);
            for (int i = 0; i < sitemaps.length(); i++) {
                JSONObject s = sitemaps.optJSONObject(i);
                if (s != null && sitemapName.equals(s.optString("name"))) return true;
            }
        } catch (OpenHABException e) {
            System.err.println("Error reading sitemaps: " + e.getMessage());
        }
        return false;
    }

    /**
     * Checks whether an item is referenced anywhere inside a sitemap.
     *
     * @param sitemapName sitemap name
     * @param itemName    item name to search for
     * @return {@code true} if the item appears in the sitemap widget tree
     */
    public boolean doesSitemapContainItem(String sitemapName, String itemName) {
        try {
            Object raw = sitemapsAPI.getSitemap(sitemapName);
            JSONObject sitemap = parseObject(raw);
            return sitemap != null && searchForItem(sitemap, itemName);
        } catch (OpenHABException e) {
            System.err.println("Error reading sitemap '" + sitemapName + "': " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Recursively searches the sitemap widget tree for a given item name.
     */
    private boolean searchForItem(Object node, String itemName) {
        if (node instanceof JSONObject) {
            JSONObject obj = (JSONObject) node;
            // Check the "item" sub-object directly on this widget
            JSONObject item = obj.optJSONObject("item");
            if (item != null && itemName.equals(item.optString("name"))) return true;
            // Recurse into all values
            for (String key : obj.keySet()) {
                if (searchForItem(obj.get(key), itemName)) return true;
            }
        } else if (node instanceof JSONArray) {
            JSONArray arr = (JSONArray) node;
            for (int i = 0; i < arr.length(); i++) {
                if (searchForItem(arr.get(i), itemName)) return true;
            }
        }
        return false;
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
