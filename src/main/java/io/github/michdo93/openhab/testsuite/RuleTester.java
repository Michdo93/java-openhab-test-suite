package io.github.michdo93.openhab.testsuite;

import static io.github.michdo93.openhab.testsuite.Support.asObject;
import static io.github.michdo93.openhab.testsuite.Support.isError;

import io.github.michdo93.openhab.Items;
import io.github.michdo93.openhab.Json;
import io.github.michdo93.openhab.OpenHABClient;
import io.github.michdo93.openhab.Rules;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Tests for openHAB rules – the counterpart of {@code RuleTester} in openhab-test-suite (Python). */
public class RuleTester {

    private final OpenHABClient client;
    private final Rules rulesAPI;
    private final Items itemsAPI;

    public RuleTester(OpenHABClient client) {
        this.client = client;
        this.rulesAPI = new Rules(client);
        this.itemsAPI = new Items(client);
    }

    private Map<String, Object> rule(String ruleUID) {
        Map<String, Object> rule = asObject(rulesAPI.getRule(ruleUID));
        if (!(rule.get("status") instanceof Map)) {
            System.err.println("Error retrieving the status of rule " + ruleUID + ".");
            return null;
        }
        return rule;
    }

    private static String status(Map<String, Object> rule, String key) {
        Object v = Json.get(rule, "status", key);
        return v == null ? null : String.valueOf(v);
    }

    /** Runs a rule (if it is not disabled); {@code contextData} may be {@code null}. */
    public boolean runRule(String ruleUID, Map<String, Object> contextData) {
        if (isRuleDisabled(ruleUID)) {
            System.err.println("Error: Rule " + ruleUID + " could not be executed because it is disabled.");
            return false;
        }
        Object result = rulesAPI.runNow(ruleUID, contextData);
        if (isError(result)) {
            System.err.println("Error executing rule " + ruleUID + ": " + Json.error(result));
            return false;
        }
        System.out.println("Rule " + ruleUID + " executed successfully.");
        return true;
    }

    public boolean runRule(String ruleUID) {
        return runRule(ruleUID, null);
    }

    public boolean enableRule(String ruleUID) {
        Object result = rulesAPI.enable(ruleUID);
        if (isError(result)) {
            System.err.println("Error enabling rule " + ruleUID + ": " + Json.error(result));
            return false;
        }
        System.out.println("Rule " + ruleUID + " enabled successfully.");
        return true;
    }

    public boolean disableRule(String ruleUID) {
        Object result = rulesAPI.disable(ruleUID);
        if (isError(result)) {
            System.err.println("Error disabling rule " + ruleUID + ": " + Json.error(result));
            return false;
        }
        System.out.println("Rule " + ruleUID + " disabled successfully.");
        return true;
    }

    /**
     * Runs a rule and checks afterwards whether an item has the expected value.
     *
     * @param waitSeconds time the rule gets before the check (default 2, as in Python)
     */
    public boolean testRuleExecution(String ruleUID, String expectedItem, Object expectedValue, double waitSeconds) {
        if (!runRule(ruleUID)) {
            System.err.println("Error: Rule " + ruleUID + " could not be executed.");
            return false;
        }
        try {
            Thread.sleep((long) (waitSeconds * 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        Object state = itemsAPI.getItemState(expectedItem);
        if (isError(state) || !String.valueOf(state).equals(String.valueOf(expectedValue))) {
            System.err.println("Error: State of item " + expectedItem + " after rule execution does not match. "
                    + "Expected: " + expectedValue + ", Found: " + (isError(state) ? Json.error(state) : state));
            return false;
        }
        System.out.println(expectedItem + " state after rule execution: " + state);
        return true;
    }

    public boolean testRuleExecution(String ruleUID, String expectedItem, Object expectedValue) {
        return testRuleExecution(ruleUID, expectedItem, expectedValue, 2);
    }

    public boolean isRuleActive(String ruleUID) {
        Map<String, Object> r = rule(ruleUID);
        if (r == null) {
            return false;
        }
        System.out.println("Rule status: " + status(r, "status"));
        return !"UNINITIALIZED".equals(status(r, "status"));
    }

    public boolean isRuleDisabled(String ruleUID) {
        Map<String, Object> r = rule(ruleUID);
        if (r == null) {
            return false;
        }
        System.out.println("Rule status: " + status(r, "status") + ", Detail: " + status(r, "statusDetail"));
        return "UNINITIALIZED".equals(status(r, "status")) && "DISABLED".equals(status(r, "statusDetail"));
    }

    public boolean isRuleRunning(String ruleUID) {
        Map<String, Object> r = rule(ruleUID);
        if (r == null) {
            return false;
        }
        System.out.println("Rule status: " + status(r, "status"));
        return "RUNNING".equals(status(r, "status"));
    }

    public boolean isRuleIdle(String ruleUID) {
        Map<String, Object> r = rule(ruleUID);
        if (r == null) {
            return false;
        }
        System.out.println("Rule status: " + status(r, "status"));
        return "IDLE".equals(status(r, "status"));
    }

    /** {@code {status, statusDetail, editable, name, uid}} or an empty map. */
    public Map<String, Object> getRuleStatus(String ruleUID) {
        Map<String, Object> r = rule(ruleUID);
        if (r == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        String st = status(r, "status");
        String detail = status(r, "statusDetail");
        out.put("status", st == null || st.isEmpty() ? "UNKNOWN" : st);
        out.put("statusDetail", detail == null || detail.isEmpty() ? "UNKNOWN" : detail);
        out.put("editable", r.get("editable") == null ? false : r.get("editable"));
        out.put("name", r.get("name") == null ? "" : r.get("name"));
        out.put("uid", r.get("uid") == null ? "" : r.get("uid"));
        return out;
    }

    public OpenHABClient client() {
        return client;
    }
}
