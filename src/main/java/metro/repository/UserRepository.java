package metro.repository;

import java.nio.file.Path;
import metro.model.Role;
import metro.model.User;
import metro.model.UserFactory;

/** Stores staff accounts in users.txt. */
public class UserRepository extends FileRepository<User> {

    public UserRepository(Path file) {
        super(file, "User", "USR", 3, "id|username|passwordHash|salt|role|fullName|phone|email|active");
    }

    @Override
    protected User parse(String[] fields) {
        if (!fields[8].equals("true") && !fields[8].equals("false")) {
            throw new IllegalArgumentException("active must be true or false");
        }
        return UserFactory.create(
                Role.valueOf(fields[4]), fields[0], fields[1], fields[2], fields[3],
                fields[5], fields[6], fields[7], fields[8].equals("true"));
    }

    @Override
    protected String[] format(User user) {
        return new String[] {
            user.getId(), user.getUsername(), user.getPasswordHash(), user.getSalt(), user.getRole().name(),
            user.getFullName(), user.getPhone(), user.getEmail(), String.valueOf(user.isActive())
        };
    }

    /** Finds an account by username, ignoring capital letters and surrounding spaces. */
    public User findByUsername(String username) {
        if (username == null) {
            return null;
        }
        for (User user : findAll()) {
            if (user.getUsername().equalsIgnoreCase(username.trim())) {
                return user;
            }
        }
        return null;
    }
}
