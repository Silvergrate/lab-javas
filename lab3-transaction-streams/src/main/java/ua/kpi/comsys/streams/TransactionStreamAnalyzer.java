package ua.kpi.comsys.streams;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Gatherer;
import java.util.stream.Gatherers;
import java.util.stream.Stream;

public class TransactionStreamAnalyzer {

    public Optional<Transaction> findFirstHighValueTransaction(
            Stream<Transaction> stream, BigDecimal threshold
    ) {
        requireArgument(stream, "stream");
        requireArgument(threshold, "threshold");

        return stream.sequential()
                .filter(transaction -> transaction.amount().compareTo(threshold) > 0)
                .findFirst();
    }

    public List<Transaction> takeTransactionsUntilBudgetExceeded(
            Stream<Transaction> stream, BigDecimal maxBudget
    ) {
        requireArgument(stream, "stream");
        requireArgument(maxBudget, "maxBudget");

        return stream.sequential()
                .gather(Gatherers.scan(
                        () -> new RunningTotal(null, BigDecimal.ZERO),
                        (previous, transaction) -> new RunningTotal(
                                transaction, previous.total().add(transaction.amount())
                        )
                ))
                .takeWhile(entry -> entry.total().compareTo(maxBudget) <= 0)
                .map(RunningTotal::transaction)
                .toList();
    }

    public List<List<Transaction>> processBatches(
            Stream<Transaction> stream, int batchSize, int maxBatches
    ) {
        requireArgument(stream, "stream");
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        if (maxBatches <= 0) {
            throw new IllegalArgumentException("maxBatches must be positive");
        }

        return stream.sequential()
                .gather(fixedBatches(batchSize, maxBatches))
                .toList();
    }

    private static Gatherer<Transaction, BatchState, List<Transaction>> fixedBatches(
            int batchSize, int maxBatches
    ) {
        return Gatherer.ofSequential(BatchState::new, (state, transaction, downstream) -> {
            state.pending.add(transaction);
            if (state.pending.size() < batchSize) {
                return true;
            }

            List<Transaction> batch = List.copyOf(state.pending);
            state.pending.clear();
            state.emitted++;
            return downstream.push(batch) && state.emitted < maxBatches;
        });
    }

    private static void requireArgument(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }

    private record RunningTotal(Transaction transaction, BigDecimal total) {}

    private static final class BatchState {
        private final List<Transaction> pending = new ArrayList<>();
        private int emitted;
    }
}
