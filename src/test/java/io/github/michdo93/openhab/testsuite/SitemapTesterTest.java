package io.github.michdo93.openhab.testsuite;

import io.github.michdo93.openhab.OpenHABClient;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link SitemapTester}.
 *
 * <p>Set {@code OPENHAB_SITEMAP} and {@code OPENHAB_SITEMAP_ITEM} to match
 * your openHAB instance.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SitemapTesterTest {

    private static SitemapTester tester;
    private static String        sitemapName;
    private static String        existingItem;

    @BeforeAll
    static void setUp() {
        String url  = System.getenv().getOrDefault("OPENHAB_URL",         "http://127.0.0.1:8080");
        String user = System.getenv().getOrDefault("OPENHAB_USER",        "openhab");
        String pass = System.getenv().getOrDefault("OPENHAB_PASS",        "habopen");
        sitemapName  = System.getenv().getOrDefault("OPENHAB_SITEMAP",     "default");
        existingItem = System.getenv().getOrDefault("OPENHAB_SITEMAP_ITEM","testSwitch");
        tester = new SitemapTester(new OpenHABClient(url, user, pass));
    }

    @Test @Order(1)
    void doesSitemapExist() {
        assertTrue(tester.doesSitemapExist(sitemapName),
            "Sitemap '" + sitemapName + "' should exist");
        assertFalse(tester.doesSitemapExist("sitemapThatDefinitelyDoesNotExist"));
    }

    @Test @Order(2)
    void doesSitemapContainItem() {
        boolean found = tester.doesSitemapContainItem(sitemapName, existingItem);
        System.out.println("doesSitemapContainItem('" + existingItem + "'): " + found);
        assertFalse(tester.doesSitemapContainItem(sitemapName, "itemThatDefinitelyDoesNotExist99"));
    }
}
