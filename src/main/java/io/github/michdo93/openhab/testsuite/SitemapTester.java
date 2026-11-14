package io.github.michdo93.openhab.testsuite;

import static io.github.michdo93.openhab.testsuite.Support.asList;
import static io.github.michdo93.openhab.testsuite.Support.asObject;

import io.github.michdo93.openhab.OpenHABClient;
import io.github.michdo93.openhab.Sitemaps;
import java.util.Collection;
import java.util.Map;

/** Tests for sitemaps – the counterpart of {@code SitemapTester} in openhab-test-suite (Python). */
public class SitemapTester {

    private final OpenHABClient client;
    private final Sitemaps sitemaps;

    public SitemapTester(OpenHABClient client) {
        this.client = client;
        this.sitemaps = new Sitemaps(client);
    }

    public boolean doesSitemapExist(String sitemapName) {
        for (Object s : asList(sitemaps.getSitemaps())) {
            if (s instanceof Map && sitemapName.equals(((Map<?, ?>) s).get("name"))) {
                return true;
            }
        }
        return false;
    }

    public boolean doesSitemapContainItem(String sitemapName, String itemName) {
        return searchForItem(asObject(sitemaps.getSitemap(sitemapName)), itemName);
    }

    private boolean searchForItem(Object node, String itemName) {
        if (node instanceof Collection) {
            for (Object e : (Collection<?>) node) {
                if (searchForItem(e, itemName)) {
                    return true;
                }
            }
            return false;
        }
        if (node instanceof Map) {
            Object item = ((Map<?, ?>) node).get("item");
            if (item instanceof Map && itemName.equals(((Map<?, ?>) item).get("name"))) {
                return true;
            }
            for (Object v : ((Map<?, ?>) node).values()) {
                if (searchForItem(v, itemName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public OpenHABClient client() {
        return client;
    }
}
