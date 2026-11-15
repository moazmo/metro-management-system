package metro;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Proves that the build can compile and run a test at all. */
class SmokeTest {

    @Test
    void buildRunsTests() {
        assertEquals(4, 2 + 2);
    }
}
