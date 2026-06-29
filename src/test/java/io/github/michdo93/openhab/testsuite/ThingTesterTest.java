package io.github.michdo93.openhab.testsuite;

import io.github.michdo93.openhab.OpenHABClient;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link ThingTester}.
 *
 * <p>Set {@code OPENHAB_THING_UID} to a valid Thing UID in your environment,
 * e.g. {@code astro:sun:local}.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ThingTesterTest {

    private static ThingTester tester;
    private static String      thingUID;

    @BeforeAll
    static void setUp() {
        String url  = System.getenv().getOrDefault("OPENHAB_URL",      "http://127.0.0.1:8080");
        String user = System.getenv().getOrDefault("OPENHAB_USER",     "openhab");
        String pass = System.getenv().getOrDefault("OPENHAB_PASS",     "habopen");
        thingUID    = System.getenv().getOrDefault("OPENHAB_THING_UID","astro:sun:local");
        tester = new ThingTester(new OpenHABClient(url, user, pass));
    }

    @Test @Order(1)
    void getThingStatusIsNotNull() {
        String status = tester.getThingStatus(thingUID);
        assertNotNull(status);
        System.out.println("Thing status: " + status);
    }

    @Test @Order(2)
    void isThingStatusMethods() {
        // At least one of ONLINE / OFFLINE / UNINITIALIZED / UNKNOWN must be true
        boolean anyMatch =
            tester.isThingOnline(thingUID)       ||
            tester.isThingOffline(thingUID)      ||
            tester.isThingUninitialized(thingUID)||
            tester.isThingUnknown(thingUID)      ||
            tester.isThingPending(thingUID)      ||
            tester.isThingError(thingUID);
        assertTrue(anyMatch, "Thing should have a known status");
    }

    @Test @Order(3)
    void enableDisableThing() {
        assertTrue(tester.disableThing(thingUID), "disableThing should succeed");
        assertTrue(tester.enableThing(thingUID),  "enableThing should succeed");
    }
}
