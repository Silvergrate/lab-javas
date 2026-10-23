package ua.kpi.comsys.dop;

import java.time.LocalDate;

/** Дані про відправлення замовлення. */
public record Shipped(String trackingCode, LocalDate dispatchDate)
        implements OrderStatus {
    public Shipped {
        if (trackingCode == null || trackingCode.isBlank()) {
            throw new IllegalArgumentException("Tracking code cannot be null or blank");
        }
        if (dispatchDate == null) {
            throw new IllegalArgumentException("Dispatch date cannot be null");
        }
    }
}
