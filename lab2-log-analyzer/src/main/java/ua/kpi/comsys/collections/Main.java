package ua.kpi.comsys.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

public class Main {
    public static void main(String[] args) {
        List<String> sourceIps = List.of(
                "127.0.0.1", "192.0.2.5", "127.0.0.1", "10.0.0.2",
                "203.0.113.7", "192.0.2.5", "10.0.0.2", "127.0.0.1"
        );
        List<String> logLines = new ArrayList<>();
        for (String ip : sourceIps) {
            logLines.add(ip + " - - [01/May/2025:07:20:10 +0000] \"GET /index.html HTTP/1.1\" 200 9481");
        }
        logLines.add("Некоректний рядок журналу");

        LogAnalyzer analyzer = new LogAnalyzer();
        System.out.println("Аналіз журналу сервера");
        System.out.printf("Рядків у журналі — %d, з них некоректних — %d%n",
                logLines.size(), logLines.size() - sourceIps.size());
        System.out.println("Унікальні IP-адреси — " + new TreeSet<>(analyzer.getUniqueIps(logLines)));

        System.out.println();
        System.out.println("Три найчастіші IP-адреси");
        List<String> topIps = analyzer.getTopIps(logLines, 3);
        for (int index = 0; index < topIps.size(); index++) {
            String ip = topIps.get(index);
            System.out.printf("%d. %s — %d%n", index + 1, ip, Collections.frequency(sourceIps, ip));
        }

        System.out.println();
        System.out.println("Перевірка некоректних аргументів");
        printExpectedError("topN = 0", () -> analyzer.getTopIps(logLines, 0));
        printExpectedError("logLines = null", () -> analyzer.getUniqueIps(null));
    }

    private static void printExpectedError(String scenario, Runnable operation) {
        try {
            operation.run();
        } catch (IllegalArgumentException exception) {
            System.out.println(scenario + " — " + exception.getMessage());
        }
    }
}
