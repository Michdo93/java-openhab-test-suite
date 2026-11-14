package io.github.michdo93.openhab.testsuite;

import static io.github.michdo93.openhab.testsuite.Support.asList;
import static io.github.michdo93.openhab.testsuite.Support.asObject;

import io.github.michdo93.openhab.OpenHABClient;
import io.github.michdo93.openhab.Persistence;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tests for persisted data – the counterpart of {@code PersistenceTester} in openhab-test-suite (Python). */
public class PersistenceTester {

    private final OpenHABClient client;
    private final Persistence persistence;

    public PersistenceTester(OpenHABClient client) {
        this.client = client;
        this.persistence = new Persistence(client);
    }

    public boolean isItemPersisted(String serviceID, String itemName) {
        for (Object entry : asList(persistence.getItemsFromService(serviceID))) {
            Object name = entry instanceof Map ? ((Map<?, ?>) entry).get("name") : entry;
            if (itemName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static String iso(Object t) {
        if (t instanceof Instant) {
            return ((Instant) t).truncatedTo(ChronoUnit.MILLIS).toString();
        }
        return t == null ? null : String.valueOf(t);
    }

    /** {@code startTime}/{@code endTime} as ISO 8601 strings or {@link Instant}. */
    public boolean hasDataInRange(String serviceID, String itemName, Object startTime, Object endTime) {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("startTime", iso(startTime));
        options.put("endTime", iso(endTime));
        Object data = asObject(persistence.getItemPersistenceData(itemName, serviceID, options)).get("data");
        return data instanceof List && !((List<?>) data).isEmpty();
    }

    public boolean checkLastPersistedState(String serviceID, String itemName, Object expectedState) {
        Object data = asObject(persistence.getItemPersistenceData(itemName, serviceID)).get("data");
        if (!(data instanceof List) || ((List<?>) data).isEmpty()) {
            return false;
        }
        List<?> entries = (List<?>) data;
        Object last = entries.get(entries.size() - 1);
        Object state = last instanceof Map ? ((Map<?, ?>) last).get("state") : null;
        return String.valueOf(state).equals(String.valueOf(expectedState));
    }

    public OpenHABClient client() {
        return client;
    }
}
