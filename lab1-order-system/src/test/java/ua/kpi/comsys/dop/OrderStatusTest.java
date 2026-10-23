package ua.kpi.comsys.dop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class OrderStatusTest {
    private static final LocalDate DISPATCH_DATE = LocalDate.of(2026, 10, 23);

    @Test
    void pendingCanBeCreatedWithoutAdditionalData() {
        assertNotNull(new Pending());
    }

    @Test
    void paidPreservesPaymentIdAndExactAmount() {
        BigDecimal amount = new BigDecimal("1250.5000");
        Paid status = new Paid(" PAY-4104 ", amount);

        assertEquals(" PAY-4104 ", status.paymentId());
        assertEquals(amount, status.amount());
        assertEquals(4, status.amount().scale());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "   ", "\t", "\n", " \t\r\n "})
    void paidRejectsMissingOrBlankPaymentId(String paymentId) {
        assertThrows(IllegalArgumentException.class,
                () -> new Paid(paymentId, new BigDecimal("1.00")));
    }

    @ParameterizedTest
    @MethodSource("invalidAmounts")
    void paidRejectsNullOrNonPositiveAmount(BigDecimal amount) {
        assertThrows(IllegalArgumentException.class, () -> new Paid("PAY-4104", amount));
    }

    static Stream<BigDecimal> invalidAmounts() {
        return Stream.of(null, BigDecimal.ZERO, new BigDecimal("0.0000"),
                new BigDecimal("-1"), new BigDecimal("-0.0001"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00000000000000000001", "1", "1250.50", "1E+3"})
    void paidAcceptsEveryStrictlyPositiveAmount(String value) {
        BigDecimal amount = new BigDecimal(value);

        assertEquals(amount, new Paid("PAY-4104", amount).amount());
    }

    @Test
    void shippedPreservesTrackingCodeAndDispatchDate() {
        Shipped status = new Shipped(" UA41040001 ", DISPATCH_DATE);

        assertEquals(" UA41040001 ", status.trackingCode());
        assertEquals(DISPATCH_DATE, status.dispatchDate());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "   ", "\t", "\n", " \t\r\n "})
    void shippedRejectsMissingOrBlankTrackingCode(String trackingCode) {
        assertThrows(IllegalArgumentException.class,
                () -> new Shipped(trackingCode, DISPATCH_DATE));
    }

    @Test
    void shippedRejectsMissingDispatchDate() {
        assertThrows(IllegalArgumentException.class, () -> new Shipped("UA41040001", null));
    }

    @Test
    void cancelledPreservesReason() {
        assertEquals(" Customer request ", new Cancelled(" Customer request ").reason());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "   ", "\t", "\n", " \t\r\n "})
    void cancelledRejectsMissingOrBlankReason(String reason) {
        assertThrows(IllegalArgumentException.class, () -> new Cancelled(reason));
    }
}
