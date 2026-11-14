package io.github.michdo93.openhab.testsuite;

import static io.github.michdo93.openhab.testsuite.Support.asObject;
import static io.github.michdo93.openhab.testsuite.Support.isError;
import static io.github.michdo93.openhab.testsuite.Support.matches;

import io.github.michdo93.openhab.ItemEvents;
import io.github.michdo93.openhab.Items;
import io.github.michdo93.openhab.Json;
import io.github.michdo93.openhab.OpenHABClient;
import io.github.michdo93.openhab.SSEConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Tests for openHAB items – the counterpart of {@code ItemTester} in openhab-test-suite (Python).
 *
 * <p>Every check and test returns {@code true} or {@code false} and explains failures on
 * {@code System.err}. The tests per item type send a command (an update for Contact and
 * Location), wait up to {@code timeout} seconds for the expected state and restore the previous
 * state afterwards.
 *
 * <pre>{@code
 * OpenHABClient client = new OpenHABClient("http://openhab:8080", null, null, token);
 * boolean ok = new ItemTester(client).testSwitch("Wohnzimmer_Licht", "ON", "ON", 10);
 * }</pre>
 */
public class ItemTester {

    private static final List<String> VALID_TYPES = List.of("Color", "Contact", "DateTime", "Dimmer",
            "Group", "Image", "Location", "Number", "Player", "Rollershutter", "String", "Switch");
    private static final Pattern NUMBER = Pattern.compile("^-?\\d+(\\.\\d+)?$");
    private static final Pattern NUMBER_WITH_UNIT = Pattern.compile("^-?\\d+(\\.\\d+)?(\\s+\\S+)?$");
    private static final Pattern DATETIME =
            Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:?\\d{2})?$");
    private static final Pattern DATA_IMAGE = Pattern.compile("^data:image/[a-zA-Z+]+;base64,.*", Pattern.DOTALL);
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "item-tester-timeout");
        t.setDaemon(true);
        return t;
    });

    private final OpenHABClient client;
    private final Items itemsAPI;
    private final ItemEvents itemEventsAPI;

    public ItemTester(OpenHABClient client) {
        this.client = client;
        this.itemsAPI = new Items(client);
        this.itemEventsAPI = new ItemEvents(client);
    }

    // ── Command validators ─────────────────────────────────────────────────────

    private static String up(Object v) {
        return String.valueOf(v).trim().toUpperCase();
    }

    private static boolean oneOf(Object v, String... values) {
        return v instanceof String && List.of(values).contains(up(v));
    }

    private static boolean isPercent(Object value) {
        String t = String.valueOf(value).trim();
        if (!NUMBER.matcher(t).matches()) {
            return false;
        }
        double d = Double.parseDouble(t);
        return d >= 0 && d <= 100;
    }

    private static Double num(String s) {
        try {
            return s.trim().isEmpty() ? null : Double.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean isValidSwitchValue(Object value) {
        return oneOf(value, "ON", "OFF");
    }

    public static boolean isValidContactValue(Object value) {
        return oneOf(value, "OPEN", "CLOSED");
    }

    public static boolean isValidDimmerValue(Object value) {
        return value != null && (List.of("ON", "OFF", "INCREASE", "DECREASE").contains(up(value))
                || isPercent(value));
    }

    public static boolean isValidRollershutterValue(Object value) {
        return value != null && (List.of("UP", "DOWN", "STOP", "MOVE").contains(up(value)) || isPercent(value));
    }

    /** ON, OFF, INCREASE, DECREASE or "H,S,B" with H 0–360, S and B 0–100. */
    public static boolean isValidColorValue(Object value) {
        if (!(value instanceof String)) {
            return false;
        }
        if (oneOf(value, "ON", "OFF", "INCREASE", "DECREASE")) {
            return true;
        }
        String[] p = ((String) value).trim().split(",", -1);
        if (p.length != 3) {
            return false;
        }
        Double h = num(p[0]), s = num(p[1]), b = num(p[2]);
        return h != null && s != null && b != null && h >= 0 && h <= 360 && s >= 0 && s <= 100
                && b >= 0 && b <= 100;
    }

    public static boolean isValidPlayerValue(Object value) {
        return oneOf(value, "PLAY", "PAUSE", "NEXT", "PREVIOUS", "REWIND", "FASTFORWARD");
    }

    /** A number, optionally followed by a unit: "20", "20.5", "20 °C", "100 %". */
    public static boolean isValidNumberValue(Object value) {
        return value != null && NUMBER_WITH_UNIT.matcher(String.valueOf(value).trim()).matches();
    }

    /** ISO 8601, e.g. "2024-01-15T08:30:00+0000" or "2024-01-15T08:30:00.000Z". */
    public static boolean isValidDateTimeValue(Object value) {
        return value instanceof String && DATETIME.matcher(((String) value).trim()).matches();
    }

    /** "lat,lon" or "lat,lon,alt". */
    public static boolean isValidLocationValue(Object value) {
        if (!(value instanceof String)) {
            return false;
        }
        String[] p = ((String) value).trim().split(",", -1);
        if (p.length < 2 || p.length > 3) {
            return false;
        }
        Double[] n = new Double[p.length];
        for (int i = 0; i < p.length; i++) {
            n[i] = num(p[i]);
            if (n[i] == null) {
                return false;
            }
        }
        return n[0] >= -90 && n[0] <= 90 && n[1] >= -180 && n[1] <= 180;
    }

    public static boolean isValidStringValue(Object value) {
        return value != null;
    }

    /** An http/https URL or a data:image/…;base64, URI. */
    public static boolean isValidImageValue(Object value) {
        if (!(value instanceof String)) {
            return false;
        }
        String v = ((String) value).trim();
        return v.startsWith("http://") || v.startsWith("https://") || DATA_IMAGE.matcher(v).matches();
    }

    // ── Checks ─────────────────────────────────────────────────────────────────

    public boolean doesItemExist(String itemName) {
        if (itemName != null && itemName.equals(asObject(itemsAPI.getItem(itemName)).get("name"))) {
            return true;
        }
        System.err.println("Error: The item '" + itemName + "' does not exist!");
        return false;
    }

    /** {@code itemType}: "Number" (base type) or "Number:Temperature" (with dimension). */
    public boolean checkItemIsType(String itemName, String itemType) {
        if (!VALID_TYPES.contains(String.valueOf(itemType).split(":")[0])) {
            System.err.println("Error: '" + itemType + "' is not a valid item type.");
            return false;
        }
        Object result = itemsAPI.getItem(itemName);
        if (isError(result)) {
            System.err.println("Error: The item '" + itemName + "' could not be found.");
            return false;
        }
        String actual = Support.str(asObject(result).get("type"));
        if (actual.split(":")[0].equals(itemType) || actual.equals(itemType)) {
            return true;
        }
        System.err.println("Error: The item '" + itemName + "' is not of type '" + itemType
                + "'! Found type: '" + actual + "'");
        return false;
    }

    public boolean checkItemHasState(String itemName, Object state) {
        Object current = itemsAPI.getItemState(itemName);
        if (current == null || isError(current)) {
            System.err.println("Error: Could not retrieve the state for item '" + itemName + "'.");
            return false;
        }
        return String.valueOf(current).equals(String.valueOf(state));
    }

    public boolean isGroupItem(String itemName) {
        return checkItemIsType(itemName, "Group");
    }

    /** The members of a group with their states, or an empty list. */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getGroupMembers(String groupName) {
        Object members = asObject(itemsAPI.getItem(groupName, Map.of("recursive", true))).get("members");
        return members instanceof List ? (List<Map<String, Object>>) members : Collections.emptyList();
    }

    public boolean doesGroupContainMember(String groupName, String memberName) {
        for (Map<String, Object> m : getGroupMembers(groupName)) {
            if (memberName.equals(m.get("name"))) {
                return true;
            }
        }
        return false;
    }

    public boolean checkGroupMemberState(String groupName, String memberName, Object expectedState) {
        for (Map<String, Object> m : getGroupMembers(groupName)) {
            if (memberName.equals(m.get("name"))) {
                return String.valueOf(m.get("state")).equals(String.valueOf(expectedState));
            }
        }
        return false;
    }

    // ── Tests per item type ────────────────────────────────────────────────────
    // expectedState: a single state, a collection of accepted states, or null (only send)

    private boolean typedTest(String itemName, String itemType, String value, boolean valid, String hint,
                              Object expectedState, int timeout) {
        if (!checkItemIsType(itemName, itemType)) {
            System.err.println("Test failed: '" + itemName + "' is not of type '" + itemType + "'.");
            return false;
        }
        if (!valid) {
            System.err.println("Test failed: '" + value + "' is not a valid " + itemType + " value. " + hint);
            return false;
        }
        return testItem(itemName, itemType, value, expectedState, timeout);
    }

    private static int t(Integer timeout) {
        return timeout == null ? 10 : timeout;
    }

    private static String s(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    public boolean testColor(String itemName, String command, Object expectedState, Integer timeout) {
        return typedTest(itemName, "Color", command, isValidColorValue(command),
                "Use ON, OFF, INCREASE, DECREASE, or 'H,S,B' (e.g. '240,100,100').", expectedState, t(timeout));
    }

    public boolean testContact(String itemName, String update, Object expectedState, Integer timeout) {
        return typedTest(itemName, "Contact", update, update == null || isValidContactValue(update),
                "Use OPEN or CLOSED.", expectedState, t(timeout));
    }

    public boolean testDateTime(String itemName, String command, Object expectedState, Integer timeout) {
        return typedTest(itemName, "DateTime", command, isValidDateTimeValue(command),
                "Use ISO 8601, e.g. '2024-01-15T08:30:00+0000'.", expectedState, t(timeout));
    }

    /** {@code command}: ON, OFF, INCREASE, DECREASE or a percentage (String or number). */
    public boolean testDimmer(String itemName, Object command, Object expectedState, Integer timeout) {
        return typedTest(itemName, "Dimmer", s(command), isValidDimmerValue(command),
                "Use ON, OFF, INCREASE, DECREASE, or a percentage between 0 and 100.", expectedState, t(timeout));
    }

    public boolean testImage(String itemName, String command, Object expectedState, Integer timeout) {
        return typedTest(itemName, "Image", command, isValidImageValue(command),
                "Use an http/https URL or a data URI (data:image/...;base64,...).", expectedState, t(timeout));
    }

    public boolean testLocation(String itemName, String update, Object expectedState, Integer timeout) {
        return typedTest(itemName, "Location", update, isValidLocationValue(update),
                "Use 'lat,lon' or 'lat,lon,alt', e.g. '48.7758,9.1829'.", expectedState, t(timeout));
    }

    /** {@code command}: a number, optionally with unit (String or number). */
    public boolean testNumber(String itemName, Object command, Object expectedState, Integer timeout) {
        return typedTest(itemName, "Number", s(command), isValidNumberValue(command),
                "Use a number, optionally with a unit (e.g. '20', '20.5', '20 °C').", expectedState, t(timeout));
    }

    public boolean testPlayer(String itemName, String command, Object expectedState, Integer timeout) {
        return typedTest(itemName, "Player", command, isValidPlayerValue(command),
                "Use PLAY, PAUSE, NEXT, PREVIOUS, REWIND, or FASTFORWARD.", expectedState, t(timeout));
    }

    /** {@code command}: UP, DOWN, STOP, MOVE or a percentage (String or number). */
    public boolean testRollershutter(String itemName, Object command, Object expectedState, Integer timeout) {
        return typedTest(itemName, "Rollershutter", s(command), isValidRollershutterValue(command),
                "Use UP, DOWN, STOP, MOVE, or a percentage between 0 and 100.", expectedState, t(timeout));
    }

    public boolean testString(String itemName, String command, Object expectedState, Integer timeout) {
        return typedTest(itemName, "String", command, isValidStringValue(command),
                "The command must not be null.", expectedState, t(timeout));
    }

    public boolean testSwitch(String itemName, String command, Object expectedState, Integer timeout) {
        return typedTest(itemName, "Switch", command, isValidSwitchValue(command), "Use ON or OFF.",
                expectedState, t(timeout));
    }

    // Shorter forms: without timeout (10 s) and without expected state (only send)

    public boolean testColor(String itemName, String command, Object expectedState) {
        return testColor(itemName, command, expectedState, null);
    }

    public boolean testColor(String itemName, String command) {
        return testColor(itemName, command, null, null);
    }

    public boolean testContact(String itemName, String update, Object expectedState) {
        return testContact(itemName, update, expectedState, null);
    }

    public boolean testContact(String itemName, String update) {
        return testContact(itemName, update, null, null);
    }

    public boolean testDateTime(String itemName, String command, Object expectedState) {
        return testDateTime(itemName, command, expectedState, null);
    }

    public boolean testDateTime(String itemName, String command) {
        return testDateTime(itemName, command, null, null);
    }

    public boolean testDimmer(String itemName, Object command, Object expectedState) {
        return testDimmer(itemName, command, expectedState, null);
    }

    public boolean testDimmer(String itemName, Object command) {
        return testDimmer(itemName, command, null, null);
    }

    public boolean testImage(String itemName, String command, Object expectedState) {
        return testImage(itemName, command, expectedState, null);
    }

    public boolean testImage(String itemName, String command) {
        return testImage(itemName, command, null, null);
    }

    public boolean testLocation(String itemName, String update, Object expectedState) {
        return testLocation(itemName, update, expectedState, null);
    }

    public boolean testLocation(String itemName, String update) {
        return testLocation(itemName, update, null, null);
    }

    public boolean testNumber(String itemName, Object command, Object expectedState) {
        return testNumber(itemName, command, expectedState, null);
    }

    public boolean testNumber(String itemName, Object command) {
        return testNumber(itemName, command, null, null);
    }

    public boolean testPlayer(String itemName, String command, Object expectedState) {
        return testPlayer(itemName, command, expectedState, null);
    }

    public boolean testPlayer(String itemName, String command) {
        return testPlayer(itemName, command, null, null);
    }

    public boolean testRollershutter(String itemName, Object command, Object expectedState) {
        return testRollershutter(itemName, command, expectedState, null);
    }

    public boolean testRollershutter(String itemName, Object command) {
        return testRollershutter(itemName, command, null, null);
    }

    public boolean testString(String itemName, String command, Object expectedState) {
        return testString(itemName, command, expectedState, null);
    }

    public boolean testString(String itemName, String command) {
        return testString(itemName, command, null, null);
    }

    public boolean testSwitch(String itemName, String command, Object expectedState) {
        return testSwitch(itemName, command, expectedState, null);
    }

    public boolean testSwitch(String itemName, String command) {
        return testSwitch(itemName, command, null, null);
    }

    // ── Core ───────────────────────────────────────────────────────────────────

    private boolean testItem(String itemName, String itemType, String commandOrUpdate, Object expectedState,
                             int timeout) {
        boolean isUpdate = itemType.equals("Contact") || itemType.equals("Location");
        String initialState = null;
        SSEConnection stream = null;
        ScheduledFuture<?> timer = null;
        try {
            if (commandOrUpdate != null) {
                Object state = itemsAPI.getItemState(itemName);
                if (isError(state)) {
                    System.err.println("Warning: Could not retrieve initial state for item '" + itemName + "'.");
                } else {
                    initialState = String.valueOf(state);
                }
            }
            // open the stream before sending, so that no event is missed
            stream = itemEventsAPI.ItemStateChangedEvent(itemName);
            if (stream.isError()) {
                System.err.println("Error: No event stream for item '" + itemName + "': "
                        + Json.error(stream.error()));
                return false;
            }
            if (commandOrUpdate != null) {
                Object sent = isUpdate ? itemsAPI.postUpdate(itemName, commandOrUpdate)
                        : itemsAPI.sendCommand(itemName, commandOrUpdate);
                if (isError(sent)) {
                    System.err.println("Error: '" + commandOrUpdate + "' was rejected for '" + itemName + "': "
                            + Json.error(sent));
                    return false;
                }
            }
            if (expectedState == null) {
                return true;
            }
            // if the state does not change at all, the check after the timeout decides
            final SSEConnection s = stream;
            timer = TIMER.schedule(s::close, timeout, TimeUnit.SECONDS);
            for (Map<String, Object> event : stream.events()) {
                if (!"ItemStateChangedEvent".equals(event.get("type"))) {
                    continue;
                }
                Object value = Json.get(event, "payload", "value");
                if (matches(value, expectedState)) {
                    System.out.println("Success: '" + itemName + "' reached state '" + value + "'.");
                    return true;
                }
            }
            return checkFinalState(itemName, expectedState);
        } catch (RuntimeException err) {
            System.err.println("Error testing '" + itemName + "': " + err.getMessage());
            return false;
        } finally {
            if (timer != null) {
                timer.cancel(false);
            }
            if (stream != null) {
                stream.close();
            }
            resetItem(itemName, itemType, initialState);
        }
    }

    private void resetItem(String itemName, String itemType, String initialState) {
        if (initialState == null) {
            return;
        }
        // NULL/UNDEF are no valid commands – restore them as state
        boolean asUpdate = itemType.equals("Contact") || itemType.equals("Location")
                || initialState.equals("NULL") || initialState.equals("UNDEF");
        Object result = asUpdate ? itemsAPI.postUpdate(itemName, initialState)
                : itemsAPI.sendCommand(itemName, initialState);
        if (isError(result)) {
            System.err.println("Warning: Could not reset item '" + itemName + "' to '" + initialState + "': "
                    + Json.error(result));
        }
    }

    private boolean checkFinalState(String itemName, Object expectedState) {
        List<Object> accepted = new ArrayList<>();
        if (expectedState instanceof Collection) {
            accepted.addAll((Collection<?>) expectedState);
        } else {
            accepted.add(expectedState);
        }
        for (Object state : accepted) {
            if (checkItemHasState(itemName, state)) {
                return true;
            }
        }
        System.err.println("Error: After fallback, state of '" + itemName + "' is not "
                + (accepted.size() > 1 ? "in " + accepted : "'" + accepted.get(0) + "'") + ".");
        return false;
    }

    /** The client this tester uses. */
    public OpenHABClient client() {
        return client;
    }
}
