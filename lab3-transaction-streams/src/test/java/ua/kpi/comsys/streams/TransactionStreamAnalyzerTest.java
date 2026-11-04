package ua.kpi.comsys.streams;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionStreamAnalyzerTest {
    private static final LocalDateTime START = LocalDateTime.of(2026, 1, 1, 12, 0);
    private final TransactionStreamAnalyzer analyzer = new TransactionStreamAnalyzer();

    @Test
    void findsFirstTransactionStrictlyAboveThreshold() {
        Transaction below = transaction(1, "19.99");
        Transaction equal = transaction(2, "20.00");
        Transaction firstMatch = transaction(3, "20.01");
        Transaction largerMatch = transaction(4, "100.00");

        Optional<Transaction> result = analyzer.findFirstHighValueTransaction(
                Stream.of(below, equal, firstMatch, largerMatch), new BigDecimal("20.0"));

        assertEquals(Optional.of(firstMatch), result);
    }

    @Test
    void returnsEmptyOptionalWhenFiniteStreamHasNoMatch() {
        Optional<Transaction> result = analyzer.findFirstHighValueTransaction(
                Stream.of(transaction(1, "5.00"), transaction(2, "10.0")), BigDecimal.TEN);

        assertEquals(Optional.empty(), result);
    }

    @Test
    void returnsEmptyOptionalForEmptyStream() {
        assertEquals(Optional.empty(), analyzer.findFirstHighValueTransaction(Stream.empty(), BigDecimal.ZERO));
    }

    @Test
    @Timeout(value = 2, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void stopsReadingInfiniteStreamAtFirstMatch() {
        AtomicInteger readCount = new AtomicInteger();
        Stream<Transaction> source = Stream.iterate(1, number -> number + 1)
                .map(number -> transaction(readCount.incrementAndGet(), number + ".00"));

        Optional<Transaction> result = analyzer.findFirstHighValueTransaction(source, new BigDecimal("3"));

        assertEquals(Optional.of(transaction(4, "4.00")), result);
        assertEquals(4, readCount.get());
    }

    @Test
    void includesExactBudgetAndStopsBeforeFirstOvershootEvenWhenARefundFollows() {
        Transaction first = transaction(1, "0.10");
        Transaction second = transaction(2, "0.20");
        AtomicInteger readCount = new AtomicInteger();
        Stream<Transaction> source = Stream.of(first, second, transaction(3, "0.01"), transaction(4, "-0.20"))
                .peek(ignored -> readCount.incrementAndGet());

        List<Transaction> result = analyzer.takeTransactionsUntilBudgetExceeded(source, new BigDecimal("0.3"));

        assertEquals(List.of(first, second), result);
        assertEquals(3, readCount.get());
    }

    @Test
    void returnsEmptyListWhenFirstTransactionExceedsBudget() {
        List<Transaction> result = analyzer.takeTransactionsUntilBudgetExceeded(
                Stream.of(transaction(1, "11.00"), transaction(2, "1.00")), BigDecimal.TEN);

        assertEquals(List.of(), result);
    }

    @Test
    void returnsAllTransactionsWhenFiniteStreamStaysWithinBudget() {
        List<Transaction> transactions = List.of(transaction(1, "4.00"), transaction(2, "1.00"));

        assertEquals(transactions,
                analyzer.takeTransactionsUntilBudgetExceeded(transactions.stream(), BigDecimal.TEN));
    }

    @Test
    void returnsEmptyBudgetPrefixForEmptyStream() {
        assertEquals(List.of(), analyzer.takeTransactionsUntilBudgetExceeded(Stream.empty(), BigDecimal.TEN));
    }

    @Test
    @Timeout(value = 2, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void stopsReadingInfiniteStreamAtFirstBudgetOvershoot() {
        AtomicInteger readCount = new AtomicInteger();
        Stream<Transaction> source = Stream.generate(() -> transaction(readCount.incrementAndGet(), "4.00"));

        List<Transaction> result = analyzer.takeTransactionsUntilBudgetExceeded(source, new BigDecimal("12.00"));

        assertEquals(List.of(transaction(1, "4.00"), transaction(2, "4.00"), transaction(3, "4.00")), result);
        assertEquals(4, readCount.get());
    }

    @Test
    void discardsIncompleteFinalBatch() {
        Transaction first = transaction(1, "1.00");
        Transaction second = transaction(2, "2.00");
        Transaction third = transaction(3, "3.00");
        Transaction fourth = transaction(4, "4.00");
        Transaction remainder = transaction(5, "5.00");

        List<List<Transaction>> result = analyzer.processBatches(Stream.of(first, second, third, fourth, remainder),
                2, 3);

        assertEquals(List.of(List.of(first, second), List.of(third, fourth)), result);
    }

    @Test
    void returnsEmptyListWhenNoFullBatchCanBeFormed() {
        assertEquals(List.of(), analyzer.processBatches(Stream.empty(), 2, 3));
        assertEquals(List.of(), analyzer.processBatches(Stream.of(transaction(1, "1.00")), 2, 3));
    }

    @Test
    @Timeout(value = 2, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void readsExactlyEnoughTransactionsForTheRequestedBatchesFromInfiniteStream() {
        AtomicInteger readCount = new AtomicInteger();
        Stream<Transaction> source = Stream.iterate(1, number -> number + 1)
                .map(number -> transaction(readCount.incrementAndGet(), "1.00"));

        List<List<Transaction>> result = analyzer.processBatches(source, 2, 3);

        assertEquals(List.of(
                List.of(transaction(1, "1.00"), transaction(2, "1.00")),
                List.of(transaction(3, "1.00"), transaction(4, "1.00")),
                List.of(transaction(5, "1.00"), transaction(6, "1.00"))), result);
        assertEquals(6, readCount.get());
    }

    @Test
    void supportsBatchesOfOneTransaction() {
        Transaction first = transaction(1, "5.00");
        Transaction second = transaction(2, "6.00");

        assertEquals(List.of(List.of(first)), analyzer.processBatches(Stream.of(first, second), 1, 1));
    }

    @Test
    void rejectsNullStreams() {
        assertThrows(IllegalArgumentException.class,
                () -> analyzer.findFirstHighValueTransaction(null, BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class,
                () -> analyzer.takeTransactionsUntilBudgetExceeded(null, BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> analyzer.processBatches(null, 1, 1));
    }

    @Test
    void rejectsNullThresholdAndBudget() {
        assertThrows(IllegalArgumentException.class,
                () -> analyzer.findFirstHighValueTransaction(Stream.empty(), null));
        assertThrows(IllegalArgumentException.class,
                () -> analyzer.takeTransactionsUntilBudgetExceeded(Stream.empty(), null));
    }

    @ParameterizedTest
    @CsvSource({"0, 1", "-1, 1", "1, 0", "1, -1"})
    void rejectsNonPositiveBatchArguments(int batchSize, int maxBatches) {
        assertThrows(IllegalArgumentException.class,
                () -> analyzer.processBatches(Stream.empty(), batchSize, maxBatches));
    }

    private static Transaction transaction(int number, String amount) {
        return new Transaction("TX-" + number, "books", new BigDecimal(amount), START.plusSeconds(number));
    }
}
