package metro.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FieldsTest {

    @Test
    void joinsWithSeparator() {
        assertEquals("a|b", Fields.join("a", "b"));
    }

    @Test
    void escapesSpecialCharacters() {
        assertEquals("a\\|b|c\\\\d|x\\ny", Fields.join("a|b", "c\\d", "x\ny"));
    }

    @Test
    void nullBecomesEmpty() {
        assertEquals("a||c", Fields.join("a", null, "c"));
    }

    @Test
    void keepsTrailingEmptyFields() {
        assertEquals(3, Fields.split("a||").length);
    }

    @Test
    void roundTripsAwkwardText() {
        String[] values = {"", "محطة حلوان", "A|B", "back\\slash", "two\nlines", " spaced "};
        assertArrayEquals(values, Fields.split(Fields.join(values)));
    }
}
