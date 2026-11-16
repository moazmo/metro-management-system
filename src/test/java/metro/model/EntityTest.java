package metro.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EntityTest {

    /** The smallest possible entity, only for this test. */
    private static class Thing extends Entity {
        Thing(String id) {
            super(id);
        }
    }

    @Test
    void sameIdIsEqual() {
        assertEquals(new Thing("X-001"), new Thing("X-001"));
        assertEquals(new Thing("X-001").hashCode(), new Thing("X-001").hashCode());
    }

    @Test
    void differentIdIsNotEqual() {
        assertNotEquals(new Thing("X-001"), new Thing("X-002"));
    }

    @Test
    void blankIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Thing(" "));
        assertThrows(IllegalArgumentException.class, () -> new Thing(null));
    }
}
