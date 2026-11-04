package ua.kpi.comsys.streams;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

public final class Main {
    private static final LocalDateTime START = LocalDateTime.of(2026, 1, 1, 9, 0);

    private Main() {}

    public static void main(String[] args) {
        var analyzer = new TransactionStreamAnalyzer();
        var first = analyzer.findFirstHighValueTransaction(transactions(), BigDecimal.valueOf(20));
        System.out.println("Перша транзакція понад 20: " + first
                .map(transaction -> transaction.transactionId() + " (сума " + transaction.amount() + ")")
                .orElse("не знайдено"));

        var withinBudget = analyzer.takeTransactionsUntilBudgetExceeded(
                transactions(), BigDecimal.valueOf(60)
        );
        System.out.println("Транзакції в межах бюджету 60: " + ids(withinBudget));

        var batches = analyzer.processBatches(transactions(), 2, 2);
        System.out.println("Пакети (розмір 2, ліміт 2): " + batches.stream().map(Main::ids).toList());
    }

    private static Stream<Transaction> transactions() {
        return Stream.iterate(1L, number -> number + 1)
                .map(number -> new Transaction(
                        "T-" + number,
                        "Роздрібна торгівля",
                        BigDecimal.TEN.multiply(BigDecimal.valueOf(number)),
                        START.plusSeconds(number - 1)
                ));
    }

    private static List<String> ids(List<Transaction> transactions) {
        return transactions.stream().map(Transaction::transactionId).toList();
    }
}
