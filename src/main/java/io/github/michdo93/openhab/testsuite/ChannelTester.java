package io.github.michdo93.openhab.testsuite;

import static io.github.michdo93.openhab.testsuite.Support.asList;
import static io.github.michdo93.openhab.testsuite.Support.asObject;

import io.github.michdo93.openhab.Links;
import io.github.michdo93.openhab.OpenHABClient;
import java.util.List;

/** Tests for links between items and channels – the counterpart of {@code ChannelTester} (Python). */
public class ChannelTester {

    private final OpenHABClient client;
    private final Links links;

    public ChannelTester(OpenHABClient client) {
        this.client = client;
        this.links = new Links(client);
    }

    public boolean isItemLinkedToChannel(String itemName, String channelUID) {
        return itemName != null && itemName.equals(asObject(links.getLink(itemName, channelUID)).get("itemName"));
    }

    /** All links of an item (each one a map with itemName, channelUID, configuration). */
    public List<Object> getLinksForItem(String itemName) {
        return asList(links.getLinks(null, itemName));
    }

    public boolean hasOrphanedLinks() {
        return !asList(links.getOrphanLinks()).isEmpty();
    }

    public boolean isItemLinkedToAnyChannel(String itemName) {
        return !getLinksForItem(itemName).isEmpty();
    }

    public OpenHABClient client() {
        return client;
    }
}
