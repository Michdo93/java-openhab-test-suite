# java-openhab-test-suite

Tests for openHAB items, things, rules, links, persistence and sitemaps in Java. It mirrors
[openhab-test-suite](https://github.com/Michdo93/openhab-test-suite) (Python) with the same classes,
methods and parameters and is built on
[java-openhab-rest-client](https://github.com/Michdo93/java-openhab-rest-client) 2.0.0.

Every check and test returns `true` or `false` and explains failures on `System.err` – so the methods
fit into JUnit assertions as well as into a quick pre-flight check before a long demo (a robot that
drives through the smart home for ten minutes should not fail after five because an item is missing).

## Classes

| Class | Description |
|---|---|
| `ItemTester` | item exists, type (also `Number:Temperature`), state, groups; command tests per item type with expected state via SSE and automatic reset |
| `ThingTester` | thing status (ONLINE/OFFLINE/…), enable and disable things |
| `RuleTester` | run, enable, disable rules, check status, rule sets an item |
| `ChannelTester` | links between items and channels, orphaned links |
| `PersistenceTester` | item persisted, data in a time range, last persisted state |
| `SitemapTester` | sitemap exists, sitemap contains an item |

## Installation

Java 11 or newer. Both libraries are served by [JitPack](https://jitpack.io):

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.Michdo93</groupId>
        <artifactId>java-openhab-test-suite</artifactId>
        <version>2.0.0</version>
    </dependency>
</dependencies>
```

java-openhab-rest-client 2.0.0 comes along as a dependency.

Gradle (Kotlin DSL):

```kotlin
repositories { mavenCentral(); maven("https://jitpack.io") }
dependencies { testImplementation("com.github.Michdo93:java-openhab-test-suite:2.0.0") }
```

## Example

```java
import io.github.michdo93.openhab.OpenHABClient;
import io.github.michdo93.openhab.testsuite.*;
import java.util.List;

OpenHABClient client = new OpenHABClient("http://openhab:8080", null, null, System.getenv("OPENHAB_TOKEN"));

ItemTester items = new ItemTester(client);
items.doesItemExist("Wohnzimmer_Licht");                            // true
items.checkItemIsType("Werkstatt_Temperatur", "Number:Temperature"); // true (also "Number")
items.testSwitch("Wohnzimmer_Licht", "ON", "ON", 10);               // send, wait up to 10 s, reset
items.testDimmer("Wohnzimmer_Dimmer", 40, List.of("40", "40.0"));   // several accepted states

new ThingTester(client).isThingOnline("astro:sun:zuhause");
new RuleTester(client).testRuleExecution("restbuch-2", "Wohnzimmer_Licht", "OFF");
new PersistenceTester(client).hasDataInRange("rrd4j", "Werkstatt_Temperatur",
        Instant.now().minus(1, ChronoUnit.DAYS), Instant.now());
new SitemapTester(client).doesSitemapContainItem("restbuch", "Wohnzimmer_Licht");
```

With JUnit 5:

```java
@Test
void lightCanBeSwitched() {
    assertTrue(new ItemTester(client).testSwitch("Wohnzimmer_Licht", "ON", "ON", 10));
}
```

## Reference

### ItemTester

| Method | Description |
|---|---|
| `doesItemExist(name)` | item exists |
| `checkItemIsType(name, type)` | type matches; `"Number"` and `"Number:Temperature"` both accepted |
| `checkItemHasState(name, state)` | current state equals `state` |
| `isGroupItem(name)`, `getGroupMembers(group)`, `doesGroupContainMember(group, member)`, `checkGroupMemberState(group, member, state)` | groups |
| `testSwitch`, `testDimmer`, `testColor`, `testNumber`, `testString`, `testRollershutter`, `testPlayer`, `testDateTime`, `testImage` `(name, command, [expectedState, [timeout]])` | send a command and wait for the expected state (single value or collection); without `expectedState` only send; previous state is restored |
| `testContact`, `testLocation` `(name, update, [expectedState, [timeout]])` | the same with a state update |
| `isValidSwitchValue(v)` … `isValidImageValue(v)` (static) | validate a command before sending |

The tests open the event stream **before** sending, so even fast devices are not missed. If no
event arrives within `timeout` (default 10 s), the current state decides. `NULL`/`UNDEF` are restored
as state updates, not as commands.

### ThingTester

`getThingStatus(uid)` (`"UNKNOWN"` for unknown UIDs), `isThingStatus(uid, status)`, `isThingOnline`,
`isThingOffline`, `isThingPending`, `isThingUnknown`, `isThingUninitialized`, `isThingError`,
`enableThing(uid)`, `disableThing(uid)`.

### RuleTester

`runRule(uid, [context])`, `enableRule(uid)`, `disableRule(uid)`,
`testRuleExecution(uid, item, expectedValue, [waitSeconds = 2])`, `isRuleActive`, `isRuleDisabled`,
`isRuleRunning`, `isRuleIdle`, `getRuleStatus(uid)` → `{status, statusDetail, editable, name, uid}`.

### ChannelTester

`isItemLinkedToChannel(item, channelUID)`, `getLinksForItem(item)`, `hasOrphanedLinks()`,
`isItemLinkedToAnyChannel(item)`.

### PersistenceTester

`isItemPersisted(serviceID, item)`, `hasDataInRange(serviceID, item, start, end)` (ISO 8601 strings
or `Instant`), `checkLastPersistedState(serviceID, item, expectedState)`.

### SitemapTester

`doesSitemapExist(name)`, `doesSitemapContainItem(sitemap, item)`.

## Changes in 2.0.0

- built on java-openhab-rest-client 2.0.0 (decoded JSON, error objects); no dependency on org.json
- behaves like openhab-test-suite 22.0.2 (Python): unknown thing UIDs give `false`/`"UNKNOWN"`, the
  event stream is opened before the command, base types are accepted for dimension items,
  numbers including `0` are valid Dimmer/Number/Rollershutter commands, `hasDataInRange` passes the
  time range on, `expectedState` may be a collection

## Development

```bash
mvn package                                                   # validator tests
OPENHAB_URL=http://127.0.0.1:8080 OPENHAB_TOKEN=oh.… mvn test  # plus integration tests
```

The integration tests expect the example configuration of the book series "openHAB REST API".

## License

MIT – see [LICENSE](LICENSE).
