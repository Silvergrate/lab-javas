package ua.kpi.comsys.collections;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogAnalyzer {
    private static final Pattern LOG_LINE = Pattern.compile(
            "(?<ip>\\S+)[ \\t]+\\S+[ \\t]+\\S+[ \\t]+"
                    + "\\[(?<time>[0-9]{2}/[A-Za-z]{3}/[0-9]{4}:[0-9]{2}:[0-9]{2}:[0-9]{2} [+-][0-9]{4})\\][ \\t]+"
                    + "\"(?:[^\"\\\\\\p{Cntrl}]++|\\\\[^\\p{Cntrl}])*+\"[ \\t]+"
                    + "[0-9]{3}[ \\t]+(?:[0-9]+|-)"
    );
    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter
            .ofPattern("dd/MMM/uuuu:HH:mm:ss xx", Locale.ENGLISH)
            .withResolverStyle(ResolverStyle.STRICT);
    private static final String IPV4_OCTET = "(?:25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])";
    private static final Pattern IPV4 = Pattern.compile(IPV4_OCTET + "(?:\\." + IPV4_OCTET + "){3}");
    private static final Pattern IPV6_CHARACTERS = Pattern.compile("[0-9a-fA-F:.]+");

    public Set<String> getUniqueIps(List<String> logLines) {
        requireLogLines(logLines);
        return new HashSet<>(countIps(logLines).keySet());
    }

    public List<String> getTopIps(List<String> logLines, int topN) {
        requireLogLines(logLines);
        if (topN <= 0) {
            throw new IllegalArgumentException("topN must be greater than 0");
        }

        return countIps(logLines).entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(topN)
                .map(Map.Entry::getKey)
                .toList();
    }

    private static void requireLogLines(List<String> logLines) {
        if (logLines == null) {
            throw new IllegalArgumentException("logLines cannot be null");
        }
    }

    private static Map<String, Integer> countIps(List<String> logLines) {
        Map<String, Integer> frequencies = new HashMap<>();
        for (String line : logLines) {
            String ip = extractIp(line);
            if (ip != null) {
                frequencies.merge(ip, 1, Integer::sum);
            }
        }
        return frequencies;
    }

    private static String extractIp(String line) {
        if (line == null) {
            return null;
        }

        Matcher match = LOG_LINE.matcher(line);
        if (!match.matches()) {
            return null;
        }

        String ip = match.group("ip");
        if (!isIpLiteral(ip)) {
            return null;
        }

        try {
            OffsetDateTime.parse(match.group("time"), LOG_TIME);
            return ip;
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static boolean isIpLiteral(String ip) {
        if (ip.indexOf(':') < 0) {
            return IPV4.matcher(ip).matches();
        }
        if (!IPV6_CHARACTERS.matcher(ip).matches()) {
            return false;
        }

        try {
            // Square brackets force IPv6 literal parsing without a hostname lookup.
            InetAddress.getByName("[" + ip + "]");
            return true;
        } catch (UnknownHostException ignored) {
            return false;
        }
    }
}
