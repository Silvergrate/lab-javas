package ua.kpi.comsys.dop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OrderProcessorTest {
    private final OrderProcessor processor = new OrderProcessor();

    @Test
    void processesPendingWithExactOutput() {
        assertEquals("Order Status: PENDING\n"
                + "Details: Order is currently pending processing.\n",
                processor.process(new Pending()));
    }

    @Test
    void processesPaidWithExactOutput() {
        assertEquals("Order Status: PAID\n"
                + "Details: Payment ID: PAY-4104, Amount: $1250.50\n",
                processor.process(new Paid("PAY-4104", new BigDecimal("1250.50"))));
    }

    @Test
    void processesShippedWithExactOutput() {
        assertEquals("Order Status: SHIPPED\n"
                + "Details: Tracking Code: UA41040001, Dispatch Date: 2026-10-23\n",
                processor.process(new Shipped("UA41040001", LocalDate.of(2026, 10, 23))));
    }

    @Test
    void processesCancelledWithExactOutput() {
        assertEquals("Order Status: CANCELLED\nDetails: Reason: Customer request\n",
                processor.process(new Cancelled("Customer request")));
    }

    @Test
    void rejectsNullStatusWithExactMessage() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class,
                        () -> processor.process(null));

        assertEquals("Status cannot be null", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("amountsAndExpectedRepresentations")
    void printsAmountWithoutRoundingOrScientificNotation(String amount, String expected) {
        assertEquals("Order Status: PAID\nDetails: Payment ID: PAY-4104, Amount: $"
                + expected + "\n", processor.process(new Paid("PAY-4104", new BigDecimal(amount))));
    }

    static Stream<Arguments> amountsAndExpectedRepresentations() {
        return Stream.of(
                Arguments.of("12345678901234567890.12345678901234567890",
                        "12345678901234567890.12345678901234567890"),
                Arguments.of("0.00000000000000000001", "0.00000000000000000001"),
                Arguments.of("1E+3", "1000"),
                Arguments.of("1.2345E-7", "0.00000012345"),
                Arguments.of("12.3400", "12.3400")
        );
    }

    @Test
    void preservesWhitespaceInNonBlankPaymentId() {
        assertEquals("Order Status: PAID\nDetails: Payment ID:  PAY-4104 , Amount: $1\n",
                processor.process(new Paid(" PAY-4104 ", BigDecimal.ONE)));
    }

    @Test
    void preservesWhitespaceInNonBlankTrackingCode() {
        assertEquals("Order Status: SHIPPED\n"
                + "Details: Tracking Code:  UA41040001 , Dispatch Date: 2026-10-23\n",
                processor.process(new Shipped(" UA41040001 ", LocalDate.of(2026, 10, 23))));
    }

    @Test
    void preservesWhitespaceInNonBlankCancellationReason() {
        assertEquals("Order Status: CANCELLED\nDetails: Reason:  Customer request \n",
                processor.process(new Cancelled(" Customer request ")));
    }
}
