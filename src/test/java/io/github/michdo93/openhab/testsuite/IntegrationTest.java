package io.github.michdo93.openhab.testsuite;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.michdo93.openhab.OpenHABClient;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Against a real openHAB with the example configuration of the book series
 * (Wohnzimmer_Licht, Wohnzimmer_Dimmer, Werkstatt_Temperatur, gWohnzimmer, astro:sun:zuhause,
 * rule restbuch-2, sitemap restbuch). Skipped without OPENHAB_URL/OPENHAB_TOKEN.
 */
class IntegrationTest {

    static OpenHABClient client;

    @BeforeAll
    static void connect() {
        String url = System.getenv("OPENHAB_URL");
        String token = System.getenv("OPENHAB_TOKEN");
        assumeTrue(url != null && token != null, "OPENHAB_URL/OPENHAB_TOKEN not set");
        client = new OpenHABClient(url, null, null, token);
        assumeTrue(client.isLoggedIn, "openHAB not reachable");
    }

    @Test
    void itemChecks() {
        ItemTester t = new ItemTester(client);
        assertTrue(t.doesItemExist("Wohnzimmer_Licht"));
        assertFalse(t.doesItemExist("Gibtsnicht"));
        assertTrue(t.checkItemIsType("Werkstatt_Temperatur", "Number"));
        assertTrue(t.checkItemIsType("Werkstatt_Temperatur", "Number:Temperature"));
        assertFalse(t.checkItemIsType("Wohnzimmer_Licht", "Dimmer"));
        assertTrue(t.isGroupItem("gWohnzimmer"));
        assertTrue(t.doesGroupContainMember("gWohnzimmer", "Wohnzimmer_Licht"));
    }

    @Test
    void commandTests() {
        ItemTester t = new ItemTester(client);
        assertTrue(t.testSwitch("Wohnzimmer_Licht", "ON", "ON", 5));
        assertTrue(t.testDimmer("Wohnzimmer_Dimmer", 0, "0", 5));          // value 0
        assertTrue(t.testDimmer("Wohnzimmer_Dimmer", 40, List.of("40", "40.0"), 5));
        assertFalse(t.testSwitch("Wohnzimmer_Licht", "AN", "ON", 2));      // invalid value
        assertFalse(t.testSwitch("Gibtsnicht", "ON", "ON", 2));
    }

    @Test
    void things() {
        ThingTester t = new ThingTester(client);
        assertTrue(t.isThingOnline("astro:sun:zuhause"));
        assertFalse(t.isThingOnline("astro:sun:gibtsnicht"));             // unknown UID -> false
        assertEquals("UNKNOWN", t.getThingStatus("astro:sun:gibtsnicht"));
    }

    @Test
    void rulesLinksSitemaps() {
        RuleTester r = new RuleTester(client);
        assertFalse(r.isRuleDisabled("restbuch-2"));
        assertTrue(r.runRule("restbuch-2"));
        assertTrue(r.getRuleStatus("gibtsnicht").isEmpty());
        ChannelTester c = new ChannelTester(client);
        assertFalse(c.isItemLinkedToChannel("Wohnzimmer_Licht", "astro:sun:zuhause:rise#start"));
        assertFalse(c.isItemLinkedToAnyChannel("Wohnzimmer_Licht"));
        SitemapTester s = new SitemapTester(client);
        assertTrue(s.doesSitemapExist("restbuch"));
        assertTrue(s.doesSitemapContainItem("restbuch", "Wohnzimmer_Licht"));
    }

    @Test
    void persistence() {
        PersistenceTester p = new PersistenceTester(client);
        new io.github.michdo93.openhab.Items(client).postUpdate("Werkstatt_Temperatur", "18.5 °C");
        assertTrue(p.isItemPersisted("inmemory", "Werkstatt_Temperatur"));
        Instant now = Instant.now();
        assertTrue(p.hasDataInRange("inmemory", "Werkstatt_Temperatur", now.minus(1, ChronoUnit.HOURS),
                now.plus(1, ChronoUnit.MINUTES)));
        assertFalse(p.hasDataInRange("inmemory", "Werkstatt_Temperatur", "2001-01-01T00:00:00.000Z",
                "2001-01-02T00:00:00.000Z"));
    }
}
