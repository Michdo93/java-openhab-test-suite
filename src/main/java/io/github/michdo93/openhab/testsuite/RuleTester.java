package io.github.michdo93.openhab.testsuite;

import io.github.michdo93.openhab.Items;
import io.github.michdo93.openhab.OpenHABClient;
import io.github.michdo93.openhab.OpenHABException;
import io.github.michdo93.openhab.Rules;

import org.json.JSONObject;

/**
 * Tests openHAB rule execution, enable/disable, and status checks.
 *
 * <p>Mirrors the Python {@code RuleTester} class from {@code openhab-test-suite}.
 */
public class RuleTester {

    private final Rules rulesAPI;
    private final Items itemsAPI;

    /**
     * Creates a {@code RuleTester} backed by the given client.
     *
     * @param client an authenticated {@link OpenHABClient}
     */
    public RuleTester(OpenHABClient client) {
        this.rulesAPI = new Rules(client);
        this.itemsAPI = new Items(client);
    }

    /**
     * Retrieves the full status object of a rule.
     *
     * @param ruleUID rule UID
     * @return a {@link JSONObject} with {@code status}, {@code statusDetail},
     *         {@code name}, {@code uid}, and {@code editable}; empty object on error
     */
    public JSONObject getRuleStatus(String ruleUID) {
        try {
            JSONObject rule = parseObject(rulesAPI.getRule(ruleUID));
            if (rule != null && rule.has("status")) {
                JSONObject s = rule.optJSONObject("status");
                JSONObject info = new JSONObject();
                info.put("status",       s != null ? s.optString("status",       "UNKNOWN") : "UNKNOWN");
                info.put("statusDetail", s != null ? s.optString("statusDetail", "UNKNOWN") : "UNKNOWN");
                info.put("editable",     rule.optBoolean("editable", false));
                info.put("name",         rule.optString("name", ""));
                info.put("uid",          rule.optString("uid", ""));
                System.out.println("Rule status: " + info);
                return info;
            }
        } catch (OpenHABException e) {
            System.err.println("Error reading status of rule '" + ruleUID + "': " + e.getMessage());
        }
        return new JSONObject();
    }

    /**
     * Checks whether a rule is active (status != {@code UNINITIALIZED}).
     *
     * @param ruleUID rule UID
     * @return {@code true} if the rule is active
     */
    public boolean isRuleActive(String ruleUID) {
        JSONObject info = getRuleStatus(ruleUID);
        return !"UNINITIALIZED".equals(info.optString("status", "UNINITIALIZED"));
    }

    /**
     * Checks whether a rule is disabled
     * (status = {@code UNINITIALIZED} and statusDetail = {@code DISABLED}).
     *
     * @param ruleUID rule UID
     * @return {@code true} if the rule is disabled
     */
    public boolean isRuleDisabled(String ruleUID) {
        JSONObject info = getRuleStatus(ruleUID);
        return "UNINITIALIZED".equals(info.optString("status"))
            && "DISABLED".equals(info.optString("statusDetail"));
    }

    /**
     * Checks whether a rule is currently {@code RUNNING}.
     *
     * @param ruleUID rule UID
     * @return {@code true} if the rule is running
     */
    public boolean isRuleRunning(String ruleUID) {
        return "RUNNING".equals(getRuleStatus(ruleUID).optString("status"));
    }

    /**
     * Checks whether a rule is {@code IDLE}.
     *
     * @param ruleUID rule UID
     * @return {@code true} if the rule is idle
     */
    public boolean isRuleIdle(String ruleUID) {
        return "IDLE".equals(getRuleStatus(ruleUID).optString("status"));
    }

    /**
     * Enables a rule.
     *
     * @param ruleUID rule UID
     * @return {@code true} if the rule was successfully enabled
     */
    public boolean enableRule(String ruleUID) {
        try {
            rulesAPI.enable(ruleUID);
            System.out.println("Rule '" + ruleUID + "' enabled successfully.");
            return true;
        } catch (OpenHABException e) {
            System.err.println("Error enabling rule '" + ruleUID + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Disables a rule.
     *
     * @param ruleUID rule UID
     * @return {@code true} if the rule was successfully disabled
     */
    public boolean disableRule(String ruleUID) {
        try {
            rulesAPI.disable(ruleUID);
            System.out.println("Rule '" + ruleUID + "' disabled successfully.");
            return true;
        } catch (OpenHABException e) {
            System.err.println("Error disabling rule '" + ruleUID + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Executes a rule immediately.
     *
     * @param ruleUID     rule UID
     * @param contextJson optional JSON context string (may be {@code null})
     * @return {@code true} if the rule was executed without error
     */
    public boolean runRule(String ruleUID, String contextJson) {
        if (isRuleDisabled(ruleUID)) {
            System.err.println("Error: Rule '" + ruleUID + "' is disabled and cannot be executed.");
            return false;
        }
        try {
            rulesAPI.runNow(ruleUID, contextJson);
            System.out.println("Rule '" + ruleUID + "' executed successfully.");
            return true;
        } catch (OpenHABException e) {
            System.err.println("Error executing rule '" + ruleUID + "': " + e.getMessage());
            return false;
        }
    }

    /** @see #runRule(String, String) */
    public boolean runRule(String ruleUID) {
        return runRule(ruleUID, null);
    }

    /**
     * Executes a rule and verifies that an item reaches the expected state afterwards.
     *
     * @param ruleUID       rule UID
     * @param expectedItem  name of the item to check
     * @param expectedValue expected state of the item after rule execution
     * @return {@code true} if the item reaches the expected state
     */
    public boolean testRuleExecution(String ruleUID, String expectedItem, String expectedValue) {
        try {
            if (!runRule(ruleUID)) {
                System.err.println("Error: Rule '" + ruleUID + "' could not be executed.");
                return false;
            }
            // Brief pause to allow the rule to complete
            Thread.sleep(2000);

            Object state = itemsAPI.getItemState(expectedItem);
            if (state == null || !state.toString().equals(expectedValue)) {
                System.err.println("Error: Item '" + expectedItem
                    + "' state mismatch. Expected: '" + expectedValue
                    + "', found: '" + state + "'.");
                return false;
            }
            System.out.println("OK: item '" + expectedItem + "' = '" + state + "'.");
            return true;
        } catch (OpenHABException e) {
            System.err.println("Error during rule test: " + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
