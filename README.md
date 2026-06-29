# java-openhab-test-suite

A Java testing library for validating openHAB installations. It mirrors the Python
[openhab-test-suite](https://github.com/Michdo93/openhab-test-suite) with identical
class and method names, powered by the
[java-openhab-rest-client](https://github.com/Michdo93/java-openhab-rest-client).

## Classes

| Class | Description |
|---|---|
| `ItemTester` | Validates item types, sends commands/updates, verifies state via SSE, auto-resets |
| `ThingTester` | Checks Thing status (ONLINE/OFFLINE/…), enables and disables Things |
| `RuleTester` | Runs rules, enables/disables rules, checks status |
| `ChannelTester` | Verifies item-channel links and orphaned links |
| `PersistenceTester` | Checks item registration in persistence services and historical data |
| `SitemapTester` | Verifies sitemap existence and item references |

## Requirements

- Java 11+
- Maven 3.6+
- Eclipse (recommended) with **m2e** plugin

## Installation

### Via JitPack (Maven)

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.Michdo93</groupId>
    <artifactId>java-openhab-test-suite</artifactId>
    <version>1.0.0</version>
</dependency>
```

### In Eclipse

1. **File → Import → Maven → Existing Maven Projects**
2. Select the `java-openhab-test-suite` root folder
3. Click **Finish** — Eclipse resolves all dependencies via m2e

## Configuration

All integration tests read connection settings from environment variables:

| Variable | Default | Description |
|---|---|---|
| `OPENHAB_URL` | `http://127.0.0.1:8080` | openHAB base URL |
| `OPENHAB_USER` | `openhab` | Username (Basic Auth) |
| `OPENHAB_PASS` | `habopen` | Password |
| `OPENHAB_THING_UID` | `astro:sun:local` | Thing UID for ThingTester tests |
| `OPENHAB_RULE_UID` | `test_color-1` | Rule UID for RuleTester tests |
| `OPENHAB_CHANNEL_ITEM` | `testSwitch` | Item name for ChannelTester tests |
| `OPENHAB_CHANNEL_UID` | `astro:moon:local:phase#name` | Channel UID for ChannelTester tests |
| `OPENHAB_PERSISTENCE_SERVICE` | `rrd4j` | Service ID for PersistenceTester tests |
| `OPENHAB_PERSISTENCE_ITEM` | `testSwitch` | Item name for PersistenceTester tests |
| `OPENHAB_SITEMAP` | `default` | Sitemap name for SitemapTester tests |
| `OPENHAB_SITEMAP_ITEM` | `testSwitch` | Item expected in the sitemap |

**In Eclipse** set these under **Run → Run Configurations → Environment**.

## Usage

```java
import io.github.michdo93.openhab.OpenHABClient;
import io.github.michdo93.openhab.testsuite.*;

OpenHABClient client = new OpenHABClient("http://127.0.0.1:8080", "openhab", "habopen");

// Item tests
ItemTester items = new ItemTester(client);
items.doesItemExist("testSwitch");                           // true/false
items.checkItemIsType("testSwitch", "Switch");               // true/false
items.testSwitch("testSwitch", "ON", "ON", 10);             // send + verify via SSE
items.testDimmer("testDimmer", "50", "50", 10);
items.testColor("testColor", "240,100,100", "240,100,100", 10);
items.testNumber("testNumber", "42", "42", 10);
items.testString("testString", "Hello", "Hello", 10);
items.isGroupItem("testGroup");
items.doesGroupContainMember("testGroup", "testSwitch");

// Thing tests
ThingTester things = new ThingTester(client);
things.isThingOnline("astro:sun:local");
things.enableThing("astro:sun:local");
things.disableThing("astro:sun:local");

// Rule tests
RuleTester rules = new RuleTester(client);
rules.isRuleActive("my-rule-uid");
rules.enableRule("my-rule-uid");
rules.runRule("my-rule-uid");
rules.testRuleExecution("my-rule-uid", "testColor", "240,100,100");

// Channel tests
ChannelTester channels = new ChannelTester(client);
channels.isItemLinkedToChannel("testSwitch", "astro:moon:local:phase#name");
channels.getLinksForItem("testSwitch");
channels.hasOrphanedLinks();

// Persistence tests
PersistenceTester persistence = new PersistenceTester(client);
persistence.isItemPersisted("rrd4j", "testSwitch");
persistence.hasDataInRange("rrd4j", "testSwitch",
    "2025-01-01T00:00:00.000Z", "2025-12-31T23:59:59.999Z");
persistence.checkLastPersistedState("rrd4j", "testSwitch", "ON");

// Sitemap tests
SitemapTester sitemaps = new SitemapTester(client);
sitemaps.doesSitemapExist("default");
sitemaps.doesSitemapContainItem("default", "testSwitch");
```

## Running the Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=ItemTesterTest

# With custom connection
OPENHAB_URL=http://192.168.1.100:8080 OPENHAB_USER=admin OPENHAB_PASS=secret mvn test
```

## License

MIT License — see [LICENSE](LICENSE).
