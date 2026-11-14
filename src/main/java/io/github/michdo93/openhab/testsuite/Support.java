package io.github.michdo93.openhab.testsuite;

import io.github.michdo93.openhab.Json;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Helpers shared by the testers (not part of the public API). */
final class Support {

    private Support() {
    }

    /** {@code true} for the {@code {"error": "..."}} objects of java-openhab-rest-client. */
    static boolean isError(Object result) {
        return Json.isError(result);
    }

    /** A result as JSON object, or an empty map (errors, text, null). */
    @SuppressWarnings("unchecked")
    static Map<String, Object> asObject(Object result) {
        if (result == null || isError(result)) {
            return Collections.emptyMap();
        }
        if (result instanceof String) {
            try {
                Object parsed = Json.parse((String) result);
                return parsed instanceof Map ? (Map<String, Object>) parsed : Collections.emptyMap();
            } catch (IllegalArgumentException e) {
                return Collections.emptyMap();
            }
        }
        return result instanceof Map ? (Map<String, Object>) result : Collections.emptyMap();
    }

    /** A result as JSON array, or an empty list. */
    @SuppressWarnings("unchecked")
    static List<Object> asList(Object result) {
        if (result instanceof List) {
            return (List<Object>) result;
        }
        if (result instanceof String) {
            try {
                Object parsed = Json.parse((String) result);
                return parsed instanceof List ? (List<Object>) parsed : Collections.emptyList();
            } catch (IllegalArgumentException e) {
                return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }

    /** {@code expected} may be a single value or a collection of accepted values. */
    static boolean matches(Object state, Object expected) {
        if (expected instanceof Collection) {
            for (Object e : (Collection<?>) expected) {
                if (String.valueOf(state).equals(String.valueOf(e))) {
                    return true;
                }
            }
            return false;
        }
        return String.valueOf(state).equals(String.valueOf(expected));
    }

    static String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
