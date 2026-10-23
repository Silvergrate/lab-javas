package ua.kpi.comsys.dop;

/** Закрита ієрархія: нові стани потребуватимуть оновлення switch. */
public sealed interface OrderStatus permits Pending, Paid, Shipped, Cancelled {
}
