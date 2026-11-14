package io.github.michdo93.openhab.testsuite;

import io.github.michdo93.openhab.OpenHABClient;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link PersistenceTester}.
 *
 * <p>Set {@code OPENHAB_PERSISTENCE_SERVICE} and {@code OPENHAB_PERSISTENCE_ITEM}
 * to match your openHAB instance.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PersistenceTesterTest {

    private static PersistenceTester tester;
    private static String            serviceId;
    private static String            itemName;

    @BeforeAll
    static void setUp() {
        String url  = System.getenv().getOrDefault("OPENHAB_URL",                "http://127.0.0.1:8080");
        String user = System.getenv().getOrDefault("OPENHAB_USER",               "openhab");
        String pass = System.getenv().getOrDefault("OPENHAB_PASS",               "habopen");
        serviceId   = System.getenv().getOrDefault("OPENHAB_PERSISTENCE_SERVICE","rrd4j");
        itemName    = System.getenv().getOrDefault("OPENHAB_PERSISTENCE_ITEM",   "testSwitch");
        tester = new PersistenceTester(new OpenHABClient(url, user, pass));
    }

    @Test @Order(1)
    void isItemPersisted() {
        boolean persisted = tester.isItemPersisted(serviceId, itemName);
        System.out.println("isItemPersisted: " + persisted);
    }

    @Test @Order(2)
    void hasDataInRange() {
        boolean has = tester.hasDataInRange(serviceId, itemName,
            "2025-01-01T00:00:00.000Z", "2025-12-31T23:59:59.999Z");
        System.out.println("hasDataInRange: " + has);
    }

    @Test @Order(3)
    void checkLastPersistedStateRunsWithoutException() {
        assertDoesNotThrow(() ->
            tester.checkLastPersistedState(serviceId, itemName, "ON"));
    }
}
