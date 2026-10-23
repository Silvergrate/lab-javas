package ua.kpi.comsys.dop;

import java.math.BigDecimal;

/** Незмінні дані про виконану оплату. */
public record Paid(String paymentId, BigDecimal amount) implements OrderStatus {
    // Компактний конструктор перевіряє дані перед створенням record.
    public Paid {
        if (paymentId == null || paymentId.isBlank()) {
            throw new IllegalArgumentException("Payment ID cannot be null or blank");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}
