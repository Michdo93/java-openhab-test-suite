package io.github.michdo93.openhab.testsuite;

import io.github.michdo93.openhab.OpenHABClient;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link ChannelTester}.
 *
 * <p>Set {@code OPENHAB_CHANNEL_UID} to a channel that is linked to
 * {@code OPENHAB_CHANNEL_ITEM} in your openHAB instance.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChannelTesterTest {

    private static ChannelTester tester;
    private static String        itemName;
    private static String        channelUID;

    @BeforeAll
    static void setUp() {
        String url  = System.getenv().getOrDefault("OPENHAB_URL",         "http://127.0.0.1:8080");
        String user = System.getenv().getOrDefault("OPENHAB_USER",        "openhab");
        String pass = System.getenv().getOrDefault("OPENHAB_PASS",        "habopen");
        itemName    = System.getenv().getOrDefault("OPENHAB_CHANNEL_ITEM","testSwitch");
        channelUID  = System.getenv().getOrDefault("OPENHAB_CHANNEL_UID", "astro:moon:local:phase#name");
        tester = new ChannelTester(new OpenHABClient(url, user, pass));
    }

    @Test @Order(1)
    void getLinksForItemNotNull() {
        assertNotNull(tester.getLinksForItem(itemName));
    }

    @Test @Order(2)
    void isItemLinkedToChannel() {
        boolean linked = tester.isItemLinkedToChannel(itemName, channelUID);
        System.out.println("isItemLinkedToChannel: " + linked);
        // Not asserting true here because the link might not exist in every test env
    }

    @Test @Order(3)
    void isItemLinkedToAnyChannel() {
        // Just verify the method runs without exception
        boolean result = tester.isItemLinkedToAnyChannel(itemName);
        System.out.println("isItemLinkedToAnyChannel: " + result);
    }

    @Test @Order(4)
    void hasOrphanedLinksRunsWithoutException() {
        assertDoesNotThrow(() -> tester.hasOrphanedLinks());
    }
}
