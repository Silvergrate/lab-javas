package ua.kpi.comsys.dop;

/** Причина скасування є обов'язковою. */
public record Cancelled(String reason) implements OrderStatus {
    public Cancelled {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason cannot be null or blank");
        }
    }
}
