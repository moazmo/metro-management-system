package metro.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import metro.exception.ValidationException;
import org.junit.jupiter.api.Test;

class CheckTest {

    @Test
    void trimsAndReturns() {
        assertEquals("Helwan", Check.notBlank("  Helwan ", "Name"));
    }

    @Test
    void blankIsRejected() {
        String[] blanks = {"", "   ", null};
        for (String blank : blanks) {
            ValidationException e = assertThrows(ValidationException.class, () -> Check.notBlank(blank, "Name"));
            assertEquals("Name is required", e.getMessage());
        }
    }

    @Test
    void rangeMessage() {
        ValidationException e = assertThrows(ValidationException.class, () -> Check.range(0, 1, 20, "Quantity"));
        assertEquals("Quantity must be between 1 and 20", e.getMessage());
        assertEquals(20, Check.range(20, 1, 20, "Quantity"));
    }

    @Test
    void positiveMessage() {
        ValidationException e = assertThrows(ValidationException.class, () -> Check.positive(0, "Price"));
        assertEquals("Price must be greater than zero", e.getMessage());
        assertEquals(2.5, Check.positive(2.5, "Price"));
    }
}
