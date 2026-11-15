package metro.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void roundsHalfUp() {
        assertEquals(10.5, Money.round(10.495 + 0.005));
        assertEquals(7.0, Money.round(10 * 0.7));
        assertEquals(4.13, Money.round(8.25 * 0.5));
    }

    @Test
    void formats() {
        assertEquals("EGP 8.00", Money.format(8));
        assertEquals("EGP 1,250.50", Money.format(1250.5));
    }
}
