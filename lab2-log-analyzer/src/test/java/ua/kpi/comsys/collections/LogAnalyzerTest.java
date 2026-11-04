package ua.kpi.comsys.collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LogAnalyzerTest {
    private final LogAnalyzer analyzer = new LogAnalyzer();

    @Test
    void returnsOnlyUniqueAddresses() {
        List<String> logs = List.of(line("192.0.2.2"), line("192.0.2.1"), line("192.0.2.2"));

        assertEquals(Set.of("192.0.2.1", "192.0.2.2"), analyzer.getUniqueIps(logs));
    }

    @Test
    void ranksByDescendingFrequencyThenAscendingAddress() {
        List<String> logs = List.of(
                line("192.0.2.2"),
                line("192.0.2.10"),
                line("192.0.2.100"),
                line("192.0.2.2"),
                line("192.0.2.1"),
                line("192.0.2.100"),
                line("192.0.2.10"),
                line("192.0.2.100"));

        assertEquals(List.of("192.0.2.100", "192.0.2.10", "192.0.2.2", "192.0.2.1"),
                analyzer.getTopIps(logs, 4));
    }

    @Test
    void limitsTopListAfterSortingTies() {
        List<String> logs = List.of(
                line("192.0.2.2"),
                line("192.0.2.10"),
                line("192.0.2.100"),
                line("192.0.2.100"));

        assertEquals(List.of("192.0.2.100", "192.0.2.10"), analyzer.getTopIps(logs, 2));
    }

    @Test
    void returnsAllAddressesWhenTopNExceedsCount() {
        List<String> logs = List.of(line("192.0.2.2"), line("192.0.2.10"), line("192.0.2.2"));

        assertEquals(List.of("192.0.2.2", "192.0.2.10"), analyzer.getTopIps(logs, 10));
    }

    @Test
    void returnsEmptyCollectionsForEmptyInput() {
        assertEquals(Set.of(), analyzer.getUniqueIps(List.of()));
        assertEquals(List.of(), analyzer.getTopIps(List.of(), 1));
    }

    @Test
    void ignoresMalformedAndNullEntriesWithoutCountingThem() {
        List<String> logs = new ArrayList<>();
        logs.add(null);
        logs.add("192.0.2.2 - -");
        logs.add(line("192.0.2.10"));
        logs.add(line("192.0.2.2") + " extra-field");
        logs.add(line("192.0.2.2"));
        logs.add("192.0.2.2");

        assertEquals(Set.of("192.0.2.2", "192.0.2.10"), analyzer.getUniqueIps(logs));
        assertEquals(List.of("192.0.2.10", "192.0.2.2"), analyzer.getTopIps(logs, 2));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "192.0.2.1",
            "192.0.2.1 - - [01/May/2025:07:20:10 +0000] GET /index.html HTTP/1.1 200 9481",
            "192.0.2.1 - - [not-a-timestamp] \"GET /index.html HTTP/1.1\" 200 9481",
            "192.0.2.1 - - [31/Feb/2025:07:20:10 +0000] \"GET /index.html HTTP/1.1\" 200 9481",
            "192.0.2.1 - - [01/May/2025:07:20:10 +9960] \"GET /index.html HTTP/1.1\" 200 9481",
            "192.0.2.1 - - [01/May/2025:07:20:10 +0000] \"GET /index.html HTTP/1.1\" XX 9481",
            "192.0.2.1 - - [01/May/2025:07:20:10 +0000] \"GET /index.html HTTP/1.1\" 200 -1",
            "999.0.2.1 - - [01/May/2025:07:20:10 +0000] \"GET /index.html HTTP/1.1\" 200 9481",
            "192.0.2 - - [01/May/2025:07:20:10 +0000] \"GET /index.html HTTP/1.1\" 200 9481",
            "2001:::1 - - [01/May/2025:07:20:10 +0000] \"GET /index.html HTTP/1.1\" 200 9481",
            "example.org - - [01/May/2025:07:20:10 +0000] \"GET /index.html HTTP/1.1\" 200 9481",
            "192.0.2.1 - - [01/May/2025:07:20:10 +0000] \"GET /index.html HTTP/1.1\" 200 9481 extra"
    })
    void ignoresMalformedRecords(String log) {
        assertEquals(Set.of(), analyzer.getUniqueIps(List.of(log)));
        assertEquals(List.of(), analyzer.getTopIps(List.of(log), 1));
    }

    @Test
    void supportsIpv6AndDashForMissingByteCount() {
        List<String> logs = List.of(
                line("127.0.0.1"),
                line("2001:db8:0:0:0:0:0:1"),
                line("::ffff:192.0.2.1"),
                "2001:db8::2 - alice [01/May/2025:07:20:10 +0300] \"HEAD / HTTP/1.1\" 304 -",
                line("2001:db8::2"));

        assertEquals(Set.of("127.0.0.1", "2001:db8:0:0:0:0:0:1", "2001:db8::2", "::ffff:192.0.2.1"),
                analyzer.getUniqueIps(logs));
        assertEquals(List.of("2001:db8::2", "127.0.0.1", "2001:db8:0:0:0:0:0:1", "::ffff:192.0.2.1"),
                analyzer.getTopIps(logs, 4));
    }

    @Test
    void acceptsMissingRequestAndEscapedQuoteInRequestField() {
        List<String> logs = List.of(
                "192.0.2.1 - - [01/May/2025:07:20:10 +0000] \"-\" 408 -",
                "192.0.2.2 - - [01/May/2025:07:20:10 +0000] \"GET /\\\"q\\\" HTTP/1.1\" 400 0");

        assertEquals(Set.of("192.0.2.1", "192.0.2.2"), analyzer.getUniqueIps(logs));
        assertEquals(List.of("192.0.2.1", "192.0.2.2"), analyzer.getTopIps(logs, 2));
    }

    @Test
    void leavesInputInItsOriginalOrder() {
        List<String> original = List.of(line("192.0.2.2"), "invalid", line("192.0.2.1"));
        List<String> logs = new ArrayList<>(original);

        analyzer.getUniqueIps(logs);
        assertEquals(original, logs);

        analyzer.getTopIps(logs, 1);
        assertEquals(original, logs);
    }

    @Test
    void uniqueAddressesRejectNullList() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> analyzer.getUniqueIps(null));

        assertEquals("logLines cannot be null", error.getMessage());
    }

    @Test
    void topAddressesRejectNullList() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> analyzer.getTopIps(null, 1));

        assertEquals("logLines cannot be null", error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void topAddressesRejectNonPositiveLimit(int topN) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> analyzer.getTopIps(List.of(), topN));

        assertEquals("topN must be greater than 0", error.getMessage());
    }

    private static String line(String ip) {
        return ip + " ident alice [01/May/2025:07:20:10 +0000] \"GET /index.html HTTP/1.1\" 200 9481";
    }
}
