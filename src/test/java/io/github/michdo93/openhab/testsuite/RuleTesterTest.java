package io.github.michdo93.openhab.testsuite;

import io.github.michdo93.openhab.OpenHABClient;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link RuleTester}.
 *
 * <p>Set {@code OPENHAB_RULE_UID} to a valid rule UID in your environment.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RuleTesterTest {

    private static RuleTester tester;
    private static String     ruleUID;

    @BeforeAll
    static void setUp() {
        String url  = System.getenv().getOrDefault("OPENHAB_URL",     "http://127.0.0.1:8080");
        String user = System.getenv().getOrDefault("OPENHAB_USER",    "openhab");
        String pass = System.getenv().getOrDefault("OPENHAB_PASS",    "habopen");
        ruleUID     = System.getenv().getOrDefault("OPENHAB_RULE_UID","test_color-1");
        tester = new RuleTester(new OpenHABClient(url, user, pass));
    }

    @Test @Order(1)
    void getRuleStatusNotEmpty() {
        assertFalse(tester.getRuleStatus(ruleUID).isEmpty());
    }

    @Test @Order(2)
    void enableAndDisableRule() {
        assertTrue(tester.enableRule(ruleUID),  "enableRule should succeed");
        assertFalse(tester.isRuleDisabled(ruleUID), "rule should not be disabled after enable");
        assertTrue(tester.disableRule(ruleUID), "disableRule should succeed");
        assertTrue(tester.isRuleDisabled(ruleUID), "rule should be disabled");
        // Restore
        tester.enableRule(ruleUID);
    }

    @Test @Order(3)
    void runRuleSucceeds() {
        tester.enableRule(ruleUID);
        assertTrue(tester.runRule(ruleUID));
    }

    @Test @Order(4)
    void isRuleIdleAfterRun() throws InterruptedException {
        tester.enableRule(ruleUID);
        tester.runRule(ruleUID);
        Thread.sleep(1500);
        assertTrue(tester.isRuleIdle(ruleUID) || tester.isRuleActive(ruleUID));
    }
}
