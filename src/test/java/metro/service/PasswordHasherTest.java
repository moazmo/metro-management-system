package metro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    @Test
    void sameInputSameHash() {
        String salt = PasswordHasher.newSalt();
        assertEquals(PasswordHasher.hash("metro123", salt), PasswordHasher.hash("metro123", salt));
    }

    @Test
    void differentSaltDifferentHash() {
        assertNotEquals(
                PasswordHasher.hash("metro123", PasswordHasher.newSalt()),
                PasswordHasher.hash("metro123", PasswordHasher.newSalt()));
    }

    @Test
    void matchesOnlyTheRightPassword() {
        String salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash("metro123", salt);
        assertTrue(PasswordHasher.matches("metro123", salt, hash));
        assertFalse(PasswordHasher.matches("Metro123", salt, hash));
    }

    @Test
    void hashDoesNotContainThePassword() {
        String hash = PasswordHasher.hash("metro123", PasswordHasher.newSalt());
        assertFalse(hash.contains("metro123"));
    }
}
