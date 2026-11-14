package io.github.michdo93.openhab.testsuite;

import io.github.michdo93.openhab.OpenHABClient;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link ItemTester}.
 *
 * <p><b>Prerequisites:</b> A running openHAB instance with the following items:
 * <ul>
 *   <li>testSwitch      (Switch)</li>
 *   <li>testDimmer      (Dimmer)</li>
 *   <li>testColor       (Color)</li>
 *   <li>testContact     (Contact)</li>
 *   <li>testRollershutter (Rollershutter)</li>
 *   <li>testNumber      (Number)</li>
 *   <li>testPlayer      (Player)</li>
 *   <li>testString      (String)</li>
 *   <li>testDateTime    (DateTime)</li>
 *   <li>testLocation    (Location)</li>
 *   <li>testGroup       (Group) containing testSwitch</li>
 * </ul>
 *
 * <p>Configure the connection via environment variables:
 * <pre>
 *   OPENHAB_URL  – default: http://127.0.0.1:8080
 *   OPENHAB_USER – default: openhab
 *   OPENHAB_PASS – default: habopen
 * </pre>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ItemTesterTest {

    private static ItemTester tester;

    @BeforeAll
    static void setUp() {
        String url  = System.getenv().getOrDefault("OPENHAB_URL",  "http://127.0.0.1:8080");
        String user = System.getenv().getOrDefault("OPENHAB_USER", "openhab");
        String pass = System.getenv().getOrDefault("OPENHAB_PASS", "habopen");
        tester = new ItemTester(new OpenHABClient(url, user, pass));
    }

    // ── Validators (unit tests – no network) ─────────────────────────────────

    @Test @Order(1)
    void testSwitchValidator() {
        assertTrue(ItemTester.isValidSwitchValue("ON"));
        assertTrue(ItemTester.isValidSwitchValue("OFF"));
        assertTrue(ItemTester.isValidSwitchValue("on"));
        assertFalse(ItemTester.isValidSwitchValue("TOGGLE"));
        assertFalse(ItemTester.isValidSwitchValue(null));
    }

    @Test @Order(2)
    void testDimmerValidator() {
        assertTrue(ItemTester.isValidDimmerValue("50"));
        assertTrue(ItemTester.isValidDimmerValue("0"));
        assertTrue(ItemTester.isValidDimmerValue("100"));
        assertTrue(ItemTester.isValidDimmerValue("INCREASE"));
        assertFalse(ItemTester.isValidDimmerValue("101"));
        assertFalse(ItemTester.isValidDimmerValue("-1"));
    }

    @Test @Order(3)
    void testColorValidator() {
        assertTrue(ItemTester.isValidColorValue("240,100,100"));
        assertTrue(ItemTester.isValidColorValue("0,0,0"));
        assertTrue(ItemTester.isValidColorValue("360,100,100"));
        assertTrue(ItemTester.isValidColorValue("ON"));
        assertFalse(ItemTester.isValidColorValue("361,0,0"));
        assertFalse(ItemTester.isValidColorValue("red"));
    }

    @Test @Order(4)
    void testNumberValidator() {
        assertTrue(ItemTester.isValidNumberValue("42"));
        assertTrue(ItemTester.isValidNumberValue("20.5"));
        assertTrue(ItemTester.isValidNumberValue("20 °C"));
        assertTrue(ItemTester.isValidNumberValue("-5"));
        assertFalse(ItemTester.isValidNumberValue("abc"));
        assertFalse(ItemTester.isValidNumberValue(null));
    }

    @Test @Order(5)
    void testLocationValidator() {
        assertTrue(ItemTester.isValidLocationValue("48.7758,9.1829"));
        assertTrue(ItemTester.isValidLocationValue("48.7758,9.1829,300.0"));
        assertFalse(ItemTester.isValidLocationValue("91,0"));
        assertFalse(ItemTester.isValidLocationValue("0,181"));
        assertFalse(ItemTester.isValidLocationValue("notALocation"));
    }

    @Test @Order(6)
    void testDateTimeValidator() {
        assertTrue(ItemTester.isValidDateTimeValue("2024-01-15T08:30:00+0000"));
        assertTrue(ItemTester.isValidDateTimeValue("2024-01-15T08:30:00.000Z"));
        assertFalse(ItemTester.isValidDateTimeValue("2024-01-15"));
        assertFalse(ItemTester.isValidDateTimeValue("not-a-date"));
    }

    // ── Integration tests (require openHAB) ─────────────────────────────────

    @Test @Order(10)
    void doesItemExist() {
        assertTrue(tester.doesItemExist("testSwitch"), "testSwitch should exist");
        assertFalse(tester.doesItemExist("itemThatDefinitelyDoesNotExist99"));
    }

    @Test @Order(11)
    void checkItemIsType() {
        assertTrue(tester.checkItemIsType("testSwitch", "Switch"));
        assertFalse(tester.checkItemIsType("testSwitch", "Dimmer"));
    }

    @Test @Order(12)
    void testSwitchItem() {
        assertTrue(tester.testSwitch("testSwitch", "ON",  "ON",  10));
        assertTrue(tester.testSwitch("testSwitch", "OFF", "OFF", 10));
    }

    @Test @Order(13)
    void testDimmerItem() {
        assertTrue(tester.testDimmer("testDimmer", "50", "50", 10));
    }

    @Test @Order(14)
    void testColorItem() {
        assertTrue(tester.testColor("testColor", "240,100,100", "240,100,100", 10));
    }

    @Test @Order(15)
    void testContactItem() {
        assertTrue(tester.testContact("testContact", "OPEN",   "OPEN",   10));
        assertTrue(tester.testContact("testContact", "CLOSED", "CLOSED", 10));
    }

    @Test @Order(16)
    void testNumberItem() {
        assertTrue(tester.testNumber("testNumber", "42", "42", 10));
    }

    @Test @Order(17)
    void testStringItem() {
        assertTrue(tester.testString("testString", "Hello openHAB", "Hello openHAB", 10));
    }

    @Test @Order(18)
    void testRollershutterItem() {
        assertTrue(tester.testRollershutter("testRollershutter", "DOWN", "100", 10));
    }

    @Test @Order(19)
    void testGroupMembers() {
        assertTrue(tester.isGroupItem("testGroup"));
        assertTrue(tester.doesGroupContainMember("testGroup", "testSwitch"));
        assertFalse(tester.doesGroupContainMember("testGroup", "nonExistentMember"));
    }
}
