package io.github.michdo93.openhab.testsuite;

import io.github.michdo93.openhab.Items;
import io.github.michdo93.openhab.ItemEvents;
import io.github.michdo93.openhab.OpenHABClient;
import io.github.michdo93.openhab.OpenHABException;
import io.github.michdo93.openhab.SSEConnection;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Tests openHAB item behaviour: type checks, command/update validation,
 * SSE-based state observation, and automatic state reset after each test.
 *
 * <p>Mirrors the Python {@code ItemTester} class from {@code openhab-test-suite}.
 */
public class ItemTester {

    private final Items     itemsAPI;
    private final ItemEvents itemEventsAPI;

    // Valid openHAB item types
    private static final java.util.Set<String> VALID_TYPES = new java.util.HashSet<>(Arrays.asList(
        "Color", "Contact", "DateTime", "Dimmer", "Group",
        "Image", "Location", "Number", "Player",
        "Rollershutter", "String", "Switch"
    ));

    private static final Pattern ISO8601 =
        Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:?\\d{2})?$");

    private static final Pattern NUMBER_WITH_UNIT =
        Pattern.compile("^-?\\d+(\\.\\d+)?(\\s+\\S+)?$");

    /**
     * Creates an {@code ItemTester} backed by the given client.
     *
     * @param client an authenticated {@link OpenHABClient}
     */
    public ItemTester(OpenHABClient client) {
        this.itemsAPI     = new Items(client);
        this.itemEventsAPI = new ItemEvents(client);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Static validators
    // ─────────────────────────────────────────────────────────────────────────

    /** @return {@code true} if {@code value} is {@code ON} or {@code OFF}. */
    public static boolean isValidSwitchValue(String value) {
        if (value == null) return false;
        String v = value.trim().toUpperCase();
        return v.equals("ON") || v.equals("OFF");
    }

    /** @return {@code true} if {@code value} is {@code OPEN} or {@code CLOSED}. */
    public static boolean isValidContactValue(String value) {
        if (value == null) return false;
        String v = value.trim().toUpperCase();
        return v.equals("OPEN") || v.equals("CLOSED");
    }

    /**
     * @return {@code true} if {@code value} is {@code ON}, {@code OFF},
     *         {@code INCREASE}, {@code DECREASE}, or a percentage 0–100.
     */
    public static boolean isValidDimmerValue(String value) {
        if (value == null) return false;
        String v = value.trim().toUpperCase();
        if (v.equals("ON") || v.equals("OFF") || v.equals("INCREASE") || v.equals("DECREASE"))
            return true;
        try {
            double d = Double.parseDouble(v);
            return d >= 0.0 && d <= 100.0;
        } catch (NumberFormatException e) { return false; }
    }

    /**
     * @return {@code true} if {@code value} is {@code UP}, {@code DOWN},
     *         {@code STOP}, {@code MOVE}, or a percentage 0–100.
     */
    public static boolean isValidRollershutterValue(String value) {
        if (value == null) return false;
        String v = value.trim().toUpperCase();
        if (v.equals("UP") || v.equals("DOWN") || v.equals("STOP") || v.equals("MOVE"))
            return true;
        try {
            double d = Double.parseDouble(v);
            return d >= 0.0 && d <= 100.0;
        } catch (NumberFormatException e) { return false; }
    }

    /**
     * @return {@code true} for {@code ON}, {@code OFF}, {@code INCREASE},
     *         {@code DECREASE}, or an HSB string {@code "H,S,B"}.
     */
    public static boolean isValidColorValue(String value) {
        if (value == null) return false;
        String v = value.trim().toUpperCase();
        if (v.equals("ON") || v.equals("OFF") || v.equals("INCREASE") || v.equals("DECREASE"))
            return true;
        String[] parts = value.trim().split(",");
        if (parts.length == 3) {
            try {
                double h = Double.parseDouble(parts[0]);
                double s = Double.parseDouble(parts[1]);
                double b = Double.parseDouble(parts[2]);
                return h >= 0 && h <= 360 && s >= 0 && s <= 100 && b >= 0 && b <= 100;
            } catch (NumberFormatException e) { return false; }
        }
        return false;
    }

    /**
     * @return {@code true} for {@code PLAY}, {@code PAUSE}, {@code NEXT},
     *         {@code PREVIOUS}, {@code REWIND}, or {@code FASTFORWARD}.
     */
    public static boolean isValidPlayerValue(String value) {
        if (value == null) return false;
        String v = value.trim().toUpperCase();
        return v.equals("PLAY") || v.equals("PAUSE") || v.equals("NEXT")
            || v.equals("PREVIOUS") || v.equals("REWIND") || v.equals("FASTFORWARD");
    }

    /**
     * @return {@code true} for any numeric value, optionally followed by a unit
     *         (e.g. {@code "20"}, {@code "20.5"}, {@code "20 °C"}).
     */
    public static boolean isValidNumberValue(String value) {
        if (value == null) return false;
        return NUMBER_WITH_UNIT.matcher(value.trim()).matches();
    }

    /**
     * @return {@code true} for an ISO-8601 datetime string,
     *         e.g. {@code "2024-01-15T08:30:00+0000"}.
     */
    public static boolean isValidDateTimeValue(String value) {
        if (value == null) return false;
        return ISO8601.matcher(value.trim()).matches();
    }

    /**
     * @return {@code true} for {@code "lat,lon"} or {@code "lat,lon,alt"}
     *         with lat ∈ [−90, 90] and lon ∈ [−180, 180].
     */
    public static boolean isValidLocationValue(String value) {
        if (value == null) return false;
        String[] parts = value.trim().split(",");
        if (parts.length < 2 || parts.length > 3) return false;
        try {
            double lat = Double.parseDouble(parts[0]);
            double lon = Double.parseDouble(parts[1]);
            if (parts.length == 3) Double.parseDouble(parts[2]);
            return lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
        } catch (NumberFormatException e) { return false; }
    }

    /** @return {@code true} for an HTTP/HTTPS URL or a Base64 data URI. */
    public static boolean isValidImageValue(String value) {
        if (value == null) return false;
        String v = value.trim();
        return v.startsWith("http://") || v.startsWith("https://")
            || v.matches("^data:image/[a-zA-Z+]+;base64,.*");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Checks whether an item with the given name exists in openHAB.
     *
     * @param itemName item name
     * @return {@code true} if the item exists
     */
    public boolean doesItemExist(String itemName) {
        try {
            Object raw = itemsAPI.getItem(itemName);
            JSONObject item = parseObject(raw);
            if (item != null && itemName.equals(item.optString("name"))) return true;
        } catch (OpenHABException e) { /* fall through */ }
        System.err.println("Error: The item '" + itemName + "' does not exist!");
        return false;
    }

    /**
     * Verifies that an item is of the expected type.
     *
     * @param itemName item name
     * @param itemType expected type (e.g. {@code "Switch"})
     * @return {@code true} if the type matches
     */
    public boolean checkItemIsType(String itemName, String itemType) {
        if (!VALID_TYPES.contains(itemType)) {
            System.err.println("Error: '" + itemType + "' is not a valid item type.");
            return false;
        }
        try {
            Object raw = itemsAPI.getItem(itemName);
            JSONObject item = parseObject(raw);
            if (item == null) {
                System.err.println("Error: Item '" + itemName + "' not found.");
                return false;
            }
            String actualType = item.optString("type", "");
            String baseType   = actualType.contains(":") ? actualType.split(":")[0] : actualType;
            if (baseType.equals(itemType)) return true;
            System.err.println("Error: '" + itemName + "' is type '" + actualType
                + "', expected '" + itemType + "'.");
        } catch (OpenHABException e) {
            System.err.println("Error checking type of '" + itemName + "': " + e.getMessage());
        }
        return false;
    }

    /**
     * Checks if the item currently holds the given state.
     *
     * @param itemName item name
     * @param state    expected state string
     * @return {@code true} if the current state equals {@code state}
     */
    public boolean checkItemHasState(String itemName, String state) {
        try {
            Object raw = itemsAPI.getItemState(itemName);
            return raw != null && raw.toString().equals(state);
        } catch (OpenHABException e) {
            System.err.println("Error reading state of '" + itemName + "': " + e.getMessage());
            return false;
        }
    }

    /** @return {@code true} if the item is of type {@code Group}. */
    public boolean isGroupItem(String itemName) {
        return checkItemIsType(itemName, "Group");
    }

    /**
     * Returns the list of member items of a Group item.
     *
     * @param groupName group item name
     * @return JSON array of member item objects (empty array on error)
     */
    public JSONArray getGroupMembers(String groupName) {
        try {
            Object raw = itemsAPI.getItem(groupName, ".*", true, null);
            JSONObject item = parseObject(raw);
            if (item != null) return item.optJSONArray("members");
        } catch (OpenHABException e) {
            System.err.println("Error reading group '" + groupName + "': " + e.getMessage());
        }
        return new JSONArray();
    }

    /**
     * Checks whether a group contains a specific member.
     *
     * @param groupName  name of the Group item
     * @param memberName name of the expected member
     * @return {@code true} if the member is found
     */
    public boolean doesGroupContainMember(String groupName, String memberName) {
        JSONArray members = getGroupMembers(groupName);
        for (int i = 0; i < members.length(); i++) {
            JSONObject m = members.optJSONObject(i);
            if (m != null && memberName.equals(m.optString("name"))) return true;
        }
        return false;
    }

    /**
     * Checks whether a group member holds the expected state.
     *
     * @param groupName     name of the Group item
     * @param memberName    name of the member item
     * @param expectedState expected state string
     * @return {@code true} if the member's state matches
     */
    public boolean checkGroupMemberState(String groupName, String memberName, String expectedState) {
        JSONArray members = getGroupMembers(groupName);
        for (int i = 0; i < members.length(); i++) {
            JSONObject m = members.optJSONObject(i);
            if (m != null && memberName.equals(m.optString("name")))
                return expectedState.equals(m.optString("state"));
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Per-type test methods
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tests a {@code Switch} item: sends {@code command} and optionally verifies
     * the resulting state via SSE.
     *
     * @param itemName      item name
     * @param command       {@code ON} or {@code OFF}
     * @param expectedState expected state (may be {@code null} to skip verification)
     * @param timeoutSec    SSE timeout in seconds
     * @return {@code true} if the test passes
     */
    public boolean testSwitch(String itemName, String command, String expectedState, int timeoutSec) {
        if (!checkItemIsType(itemName, "Switch")) return false;
        if (!isValidSwitchValue(command)) {
            System.err.println("Invalid Switch command '" + command + "'. Use ON or OFF.");
            return false;
        }
        return runTest(itemName, "Switch", command, expectedState, timeoutSec);
    }

    /** @see #testSwitch(String, String, String, int) */
    public boolean testSwitch(String itemName, String command) {
        return testSwitch(itemName, command, null, 10);
    }

    /**
     * Tests a {@code Contact} item: posts an update ({@code OPEN}/{@code CLOSED})
     * because Contact items do not accept {@code sendCommand}.
     *
     * @param itemName      item name
     * @param update        {@code OPEN} or {@code CLOSED}
     * @param expectedState expected state (may be {@code null})
     * @param timeoutSec    SSE timeout in seconds
     * @return {@code true} if the test passes
     */
    public boolean testContact(String itemName, String update, String expectedState, int timeoutSec) {
        if (!checkItemIsType(itemName, "Contact")) return false;
        if (update != null && !isValidContactValue(update)) {
            System.err.println("Invalid Contact update '" + update + "'. Use OPEN or CLOSED.");
            return false;
        }
        return runTest(itemName, "Contact", update, expectedState, timeoutSec);
    }

    /** @see #testContact(String, String, String, int) */
    public boolean testContact(String itemName, String update) {
        return testContact(itemName, update, null, 10);
    }

    /**
     * Tests a {@code Color} item with an HSB string or {@code ON}/{@code OFF}/
     * {@code INCREASE}/{@code DECREASE}.
     *
     * @param itemName      item name
     * @param command       HSB string {@code "H,S,B"} or a named command
     * @param expectedState expected state (may be {@code null})
     * @param timeoutSec    SSE timeout in seconds
     * @return {@code true} if the test passes
     */
    public boolean testColor(String itemName, String command, String expectedState, int timeoutSec) {
        if (!checkItemIsType(itemName, "Color")) return false;
        if (!isValidColorValue(command)) {
            System.err.println("Invalid Color command '" + command + "'.");
            return false;
        }
        return runTest(itemName, "Color", command, expectedState, timeoutSec);
    }

    /** @see #testColor(String, String, String, int) */
    public boolean testColor(String itemName, String command) {
        return testColor(itemName, command, null, 10);
    }

    /**
     * Tests a {@code Dimmer} item.
     *
     * @param itemName      item name
     * @param command       {@code ON}/{@code OFF}/{@code INCREASE}/{@code DECREASE}
     *                      or a percentage string {@code "0"} – {@code "100"}
     * @param expectedState expected state (may be {@code null})
     * @param timeoutSec    SSE timeout in seconds
     * @return {@code true} if the test passes
     */
    public boolean testDimmer(String itemName, String command, String expectedState, int timeoutSec) {
        if (!checkItemIsType(itemName, "Dimmer")) return false;
        if (!isValidDimmerValue(command)) {
            System.err.println("Invalid Dimmer command '" + command + "'.");
            return false;
        }
        return runTest(itemName, "Dimmer", command, expectedState, timeoutSec);
    }

    /** @see #testDimmer(String, String, String, int) */
    public boolean testDimmer(String itemName, String command) {
        return testDimmer(itemName, command, null, 10);
    }

    /**
     * Tests a {@code Rollershutter} item.
     *
     * @param itemName      item name
     * @param command       {@code UP}/{@code DOWN}/{@code STOP}/{@code MOVE}
     *                      or a percentage string
     * @param expectedState expected state (may be {@code null})
     * @param timeoutSec    SSE timeout in seconds
     * @return {@code true} if the test passes
     */
    public boolean testRollershutter(String itemName, String command, String expectedState, int timeoutSec) {
        if (!checkItemIsType(itemName, "Rollershutter")) return false;
        if (!isValidRollershutterValue(command)) {
            System.err.println("Invalid Rollershutter command '" + command + "'.");
            return false;
        }
        return runTest(itemName, "Rollershutter", command, expectedState, timeoutSec);
    }

    /** @see #testRollershutter(String, String, String, int) */
    public boolean testRollershutter(String itemName, String command) {
        return testRollershutter(itemName, command, null, 10);
    }

    /**
     * Tests a {@code Number} item.
     *
     * @param itemName      item name
     * @param command       numeric string, optionally with unit (e.g. {@code "20 °C"})
     * @param expectedState expected state (may be {@code null})
     * @param timeoutSec    SSE timeout in seconds
     * @return {@code true} if the test passes
     */
    public boolean testNumber(String itemName, String command, String expectedState, int timeoutSec) {
        if (!checkItemIsType(itemName, "Number")) return false;
        if (!isValidNumberValue(command)) {
            System.err.println("Invalid Number command '" + command + "'.");
            return false;
        }
        return runTest(itemName, "Number", command, expectedState, timeoutSec);
    }

    /** @see #testNumber(String, String, String, int) */
    public boolean testNumber(String itemName, String command) {
        return testNumber(itemName, command, null, 10);
    }

    /**
     * Tests a {@code Player} item.
     *
     * @param itemName      item name
     * @param command       {@code PLAY}/{@code PAUSE}/{@code NEXT}/{@code PREVIOUS}/
     *                      {@code REWIND}/{@code FASTFORWARD}
     * @param expectedState expected state (may be {@code null})
     * @param timeoutSec    SSE timeout in seconds
     * @return {@code true} if the test passes
     */
    public boolean testPlayer(String itemName, String command, String expectedState, int timeoutSec) {
        if (!checkItemIsType(itemName, "Player")) return false;
        if (!isValidPlayerValue(command)) {
            System.err.println("Invalid Player command '" + command + "'.");
            return false;
        }
        return runTest(itemName, "Player", command, expectedState, timeoutSec);
    }

    /** @see #testPlayer(String, String, String, int) */
    public boolean testPlayer(String itemName, String command) {
        return testPlayer(itemName, command, null, 10);
    }

    /**
     * Tests a {@code DateTime} item.
     *
     * @param itemName      item name
     * @param command       ISO-8601 datetime string
     * @param expectedState expected state (may be {@code null})
     * @param timeoutSec    SSE timeout in seconds
     * @return {@code true} if the test passes
     */
    public boolean testDateTime(String itemName, String command, String expectedState, int timeoutSec) {
        if (!checkItemIsType(itemName, "DateTime")) return false;
        if (!isValidDateTimeValue(command)) {
            System.err.println("Invalid DateTime command '" + command + "'. Use ISO-8601.");
            return false;
        }
        return runTest(itemName, "DateTime", command, expectedState, timeoutSec);
    }

    /** @see #testDateTime(String, String, String, int) */
    public boolean testDateTime(String itemName, String command) {
        return testDateTime(itemName, command, null, 10);
    }

    /**
     * Tests a {@code Location} item (postUpdate only).
     *
     * @param itemName      item name
     * @param update        {@code "lat,lon"} or {@code "lat,lon,alt"}
     * @param expectedState expected state (may be {@code null})
     * @param timeoutSec    SSE timeout in seconds
     * @return {@code true} if the test passes
     */
    public boolean testLocation(String itemName, String update, String expectedState, int timeoutSec) {
        if (!checkItemIsType(itemName, "Location")) return false;
        if (!isValidLocationValue(update)) {
            System.err.println("Invalid Location update '" + update + "'.");
            return false;
        }
        return runTest(itemName, "Location", update, expectedState, timeoutSec);
    }

    /** @see #testLocation(String, String, String, int) */
    public boolean testLocation(String itemName, String update) {
        return testLocation(itemName, update, null, 10);
    }

    /**
     * Tests an {@code Image} item.
     *
     * @param itemName      item name
     * @param command       HTTP/HTTPS URL or Base64 data URI
     * @param expectedState expected state (may be {@code null})
     * @param timeoutSec    SSE timeout in seconds
     * @return {@code true} if the test passes
     */
    public boolean testImage(String itemName, String command, String expectedState, int timeoutSec) {
        if (!checkItemIsType(itemName, "Image")) return false;
        if (!isValidImageValue(command)) {
            System.err.println("Invalid Image command '" + command + "'.");
            return false;
        }
        return runTest(itemName, "Image", command, expectedState, timeoutSec);
    }

    /** @see #testImage(String, String, String, int) */
    public boolean testImage(String itemName, String command) {
        return testImage(itemName, command, null, 10);
    }

    /**
     * Tests a {@code String} item.
     *
     * @param itemName      item name
     * @param command       any non-null string
     * @param expectedState expected state (may be {@code null})
     * @param timeoutSec    SSE timeout in seconds
     * @return {@code true} if the test passes
     */
    public boolean testString(String itemName, String command, String expectedState, int timeoutSec) {
        if (!checkItemIsType(itemName, "String")) return false;
        if (command == null) {
            System.err.println("Command for String item '" + itemName + "' must not be null.");
            return false;
        }
        return runTest(itemName, "String", command, expectedState, timeoutSec);
    }

    /** @see #testString(String, String, String, int) */
    public boolean testString(String itemName, String command) {
        return testString(itemName, command, null, 10);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Core test loop: saves current state, sends command/update, waits for SSE
     * {@code ItemStateChangedEvent}, and restores the original state afterwards.
     */
    private boolean runTest(String itemName, String itemType,
                            String commandOrUpdate, String expectedState,
                            int timeoutSec) {
        String initialState = null;
        boolean result      = false;

        try {
            // Save initial state for reset
            if (commandOrUpdate != null) {
                try {
                    Object raw = itemsAPI.getItemState(itemName);
                    initialState = raw != null ? raw.toString() : null;
                } catch (OpenHABException e) {
                    System.out.println("Warning: could not read initial state of '" + itemName + "'.");
                }
            }

            // Open SSE *before* sending the command so we don't miss the event
            try (SSEConnection sse = itemEventsAPI.ItemStateChangedEvent(itemName)) {

                // Send command or post update
                if (commandOrUpdate != null) {
                    if ("Contact".equals(itemType) || "Location".equals(itemType)) {
                        itemsAPI.postUpdate(itemName, commandOrUpdate);
                    } else {
                        itemsAPI.sendCommand(itemName, commandOrUpdate);
                    }
                }

                if (expectedState == null) {
                    // No verification requested – command accepted → success
                    result = true;
                } else {
                    // Walk SSE events until expected state or timeout
                    long deadline = System.currentTimeMillis() + timeoutSec * 1000L;
                    while (System.currentTimeMillis() < deadline) {
                        String event = sse.nextEvent();
                        if (event == null) break;
                        try {
                            JSONObject data    = new JSONObject(event);
                            String     evType  = data.optString("type");
                            String     payload = data.optString("payload");
                            if ("ItemStateChangedEvent".equals(evType) && !payload.isEmpty()) {
                                String state = new JSONObject(payload).optString("value");
                                if (expectedState.equals(state)) {
                                    System.out.println("OK: '" + itemName + "' reached state '" + state + "'.");
                                    result = true;
                                    break;
                                }
                            }
                        } catch (Exception ignored) { /* malformed event – skip */ }
                    }

                    // Fallback: direct state read after timeout
                    if (!result) {
                        result = checkItemHasState(itemName, expectedState);
                        if (!result)
                            System.err.println("Error: state of '" + itemName
                                + "' is not '" + expectedState + "' after " + timeoutSec + "s.");
                    }
                }
            }

        } catch (OpenHABException e) {
            System.err.println("Error testing '" + itemName + "': " + e.getMessage());
        } finally {
            // Always restore the initial state
            resetItem(itemName, itemType, initialState);
        }
        return result;
    }

    private void resetItem(String itemName, String itemType, String initialState) {
        if (initialState == null) return;
        try {
            if ("Contact".equals(itemType) || "Location".equals(itemType)) {
                itemsAPI.postUpdate(itemName, initialState);
            } else {
                itemsAPI.sendCommand(itemName, initialState);
            }
        } catch (OpenHABException e) {
            System.out.println("Warning: could not reset '" + itemName + "' to '" + initialState + "': " + e.getMessage());
        }
    }

    private static JSONObject parseObject(Object raw) {
        if (raw == null) return null;
        if (raw instanceof JSONObject) return (JSONObject) raw;
        try { return new JSONObject(raw.toString()); }
        catch (Exception e) { return null; }
    }
}
