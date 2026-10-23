package ua.kpi.comsys.dop;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Демонстрація варіанта 0: 4104 % 3 = 0. */
public class Main {
    public static void main(String[] args) {
        OrderProcessor processor = new OrderProcessor();

        System.out.println("Лабораторна робота 1 | Гасюк Д.О. | "
                + "ІО-41 | 4104 | Варіант 0");
        System.out.println();

        // Сталі приклади дають однаковий результат під час кожного запуску.
        List<OrderStatus> statuses = List.of(
                new Pending(),
                new Paid("PAY-4104", new BigDecimal("1250.50")),
                new Shipped("UA41040001", LocalDate.of(2026, 10, 23)),
                new Cancelled("Customer request")
        );

        for (OrderStatus status : statuses) {
            System.out.print(processor.process(status));
            System.out.println();
        }

        System.out.println("EXPECTED VALIDATION ERRORS");
        expectValidationError("Null status", () -> processor.process(null));
        expectValidationError("Blank payment ID",
                () -> new Paid("  ", BigDecimal.ONE));
        expectValidationError("Zero amount",
                () -> new Paid("PAY-4104", BigDecimal.ZERO));
        expectValidationError("Null amount", () -> new Paid("PAY-4104", null));
        expectValidationError("Blank tracking code",
                () -> new Shipped("\t", LocalDate.of(2026, 10, 23)));
        expectValidationError("Null dispatch date",
                () -> new Shipped("UA41040001", null));
        expectValidationError("Blank reason", () -> new Cancelled(" "));
    }

    // Очікувані винятки не переривають демонстрацію наступних прикладів.
    private static void expectValidationError(String example, Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException exception) {
            System.out.println(example + " -> " + exception.getMessage());
            return;
        }
        throw new IllegalStateException(
                "Expected IllegalArgumentException: " + example);
    }
}
