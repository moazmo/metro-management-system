# Phases 0–1 — Skeleton and Foundation (Tasks 1–7)

Part of the [build plan](../plan.md). Global constraints and task cycles are defined there.

Result of these phases: the project builds with one command, a themed
window opens, demo accounts exist, staff can log in and out, the menu
depends on the role, admins manage staff users, everyone edits their
own profile, and every change lands in a readable text file.

---

### Task 1: Project skeleton

Branch: `phase-0-skeleton`

**Files**
- Create: `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`, `.gitattributes`, `.github/workflows/build.yml`
- Create: `src/main/java/metro/Main.java`, `src/main/java/metro/ui/MetroApp.java`, `src/main/java/metro/ui/Theme.java`, `src/main/resources/metro/ui/app.css`
- Create: `src/test/java/metro/SmokeTest.java`
- Modify: `.gitignore` (add `/data/`), `README.md` (add a "Run it" section)

**Produces**
```java
// metro.Main — a plain class so the app can later be packaged as one runnable file
public static void main(String[] args)            // Application.launch(MetroApp.class, args)

// metro.ui.MetroApp extends javafx.application.Application
public void start(Stage stage)                    // title "Metro Management System", 1280x800, min 1100x700

// metro.ui.Theme — the only class that imports atlantafx
public static void apply(Scene scene, boolean dark)   // PrimerLight / PrimerDark, then adds /metro/ui/app.css
public static boolean isDark()
public static void toggle(Scene scene)
```

- [ ] **Step 1: Add the wrapper.** Take `mvnw` and `mvnw.cmd` from `maven-wrapper-distribution-3.3.4-only-script.zip` on Maven Central (`org/apache/maven/wrapper/maven-wrapper-distribution/3.3.4/`). Write `.mvn/wrapper/maven-wrapper.properties`:

```properties
wrapperVersion=3.3.4
distributionType=only-script
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip
```

Then `git add mvnw && git update-index --chmod=+x mvnw`.

- [ ] **Step 2: Write `pom.xml`.**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>metro</groupId>
  <artifactId>metro-management-system</artifactId>
  <version>1.0.0</version>

  <properties>
    <maven.compiler.release>25</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <javafx.version>25.0.5</javafx.version>
    <metro.mainClass>metro.Main</metro.mainClass>
  </properties>

  <dependencies>
    <dependency><groupId>org.openjfx</groupId><artifactId>javafx-controls</artifactId><version>${javafx.version}</version></dependency>
    <dependency><groupId>io.github.mkpaz</groupId><artifactId>atlantafx-base</artifactId><version>3.0.0</version></dependency>
    <dependency><groupId>org.kordamp.ikonli</groupId><artifactId>ikonli-javafx</artifactId><version>12.4.0</version></dependency>
    <dependency><groupId>org.kordamp.ikonli</groupId><artifactId>ikonli-material2-pack</artifactId><version>12.4.0</version></dependency>
    <dependency><groupId>org.junit.jupiter</groupId><artifactId>junit-jupiter</artifactId><version>6.1.3</version><scope>test</scope></dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-compiler-plugin</artifactId><version>3.16.0</version></plugin>
      <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-surefire-plugin</artifactId><version>3.6.1</version></plugin>
      <plugin>
        <groupId>org.openjfx</groupId><artifactId>javafx-maven-plugin</artifactId><version>0.0.8</version>
        <configuration>
          <mainClass>${metro.mainClass}</mainClass>
          <options><option>--enable-native-access=javafx.graphics</option></options>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

The `--enable-native-access` option is what keeps the console free of JDK warnings at start-up. `metro.mainClass` is a property so another entry point can be run with `-Dmetro.mainClass=…` (used by the screenshot tool in Task 6).

- [ ] **Step 3: Write `SmokeTest`** with one test, `buildRunsTests: assertEquals(4, 2 + 2)`. Run `./mvnw -q test`. Expected: exit code 0 (the first run downloads Maven and the libraries).

- [ ] **Step 4: Write `Main`, `MetroApp`, `Theme`, `app.css`.** `MetroApp.start` shows a centred label "Metro Management System" on a light themed scene. `app.css` defines the accent colour as a looked-up colour `-metro-accent: #0B6E99;` on `.root` and nothing else yet.

- [ ] **Step 5: Run it.** `./mvnw -q javafx:run`. Expected: the window opens with the title above, themed (not the default grey JavaFX look), and the console prints no `WARNING` lines. Close the window; the command exits with code 0.

- [ ] **Step 6: Add `.gitattributes`**

```
* text=auto
mvnw text eol=lf
*.cmd text eol=crlf
*.txt text eol=lf
```

- [ ] **Step 7: Add `.github/workflows/build.yml`**

```yaml
name: build
on:
  push:
    branches: [main]
  pull_request:
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '25'
          cache: maven
      - run: ./mvnw -B -q verify
```

- [ ] **Step 8: README "Run it" section** — needs JDK 25; `./mvnw javafx:run` to start, `./mvnw test` to test; PowerShell users type `.\mvnw.cmd`. Add the build badge for the workflow above.

- [ ] **Step 9: Commit** `Add project skeleton with themed empty window`, push the branch, open a pull request, confirm the `build` check is green, merge.

---

### Task 2: Model base, exceptions, field codec

Branch for Tasks 2–7: `phase-1-foundation`

**Files**
- Create: `model/Identifiable.java`, `model/Entity.java`
- Create: `exception/MetroException.java`, `ValidationException.java`, `NotFoundException.java`, `AccessDeniedException.java`, `DataFileException.java`
- Create: `util/Fields.java`, `util/Check.java`, `util/Money.java`, `util/Dates.java`
- Test: `util/FieldsTest.java`, `util/CheckTest.java`, `util/MoneyTest.java`, `model/EntityTest.java`

**Produces**
```java
public interface Identifiable { String getId(); }

public abstract class Entity implements Identifiable {
    protected Entity(String id)          // blank id -> IllegalArgumentException
    public final String getId()
    // equals / hashCode use the id only. There is no setId.
}

public class MetroException extends RuntimeException      // (String message), (String message, Throwable cause)
public class ValidationException  extends MetroException  // a rule was broken by the input
public class NotFoundException    extends MetroException
public class AccessDeniedException extends MetroException
public class DataFileException    extends MetroException  // a data file could not be read or written

public final class Fields {
    public static String join(String... values)   // null -> ""; writes \ as \\, | as \|, line break as \n; drops \r
    public static String[] split(String line)     // exact inverse; keeps empty fields, including trailing ones
}

public final class Check {
    public static String notBlank(String value, String label)          // returns the trimmed value; else "<label> is required"
    public static int range(int value, int min, int max, String label) // else "<label> must be between <min> and <max>"
    public static double positive(double value, String label)          // else "<label> must be greater than zero"
}

public final class Money {
    public static double round(double amount)     // two decimals, half up
    public static String format(double amount)    // "EGP 8.00", "EGP 1,250.50"
}

public final class Dates {
    public static final DateTimeFormatter FILE;   // yyyy-MM-dd'T'HH:mm:ss
    public static final DateTimeFormatter DATE;   // dd MMM yyyy, Locale.ENGLISH
    public static final DateTimeFormatter TIME;   // HH:mm
    public static String file(LocalDateTime t)    // "" for null
    public static LocalDateTime parse(String s)   // null for ""
}
```

**Tests**
```
FieldsTest
  joinsWithSeparator:        assertEquals("a|b", Fields.join("a", "b"))
  escapesSpecialCharacters:  assertEquals("a\\|b|c\\\\d|x\\ny", Fields.join("a|b", "c\\d", "x\ny"))
  nullBecomesEmpty:          assertEquals("a||c", Fields.join("a", null, "c"))
  keepsTrailingEmptyFields:  assertEquals(3, Fields.split("a||").length)
  roundTripsAwkwardText:     for {"", "محطة حلوان", "A|B", "back\\slash", "two\nlines", " spaced "}:
                             assertArrayEquals(values, Fields.split(Fields.join(values)))
CheckTest
  trimsAndReturns:           assertEquals("Helwan", Check.notBlank("  Helwan ", "Name"))
  blankIsRejected:           ValidationException with message "Name is required" for "", "   ", null
  rangeMessage:              Check.range(0, 1, 20, "Quantity") -> "Quantity must be between 1 and 20"
MoneyTest
  roundsHalfUp:              assertEquals(10.5, Money.round(10.495 + 0.005)); assertEquals(7.0, Money.round(10 * 0.7))
  formats:                   assertEquals("EGP 8.00", Money.format(8)); assertEquals("EGP 1,250.50", Money.format(1250.5))
EntityTest (with a tiny private subclass)
  sameIdIsEqual, differentIdIsNotEqual, blankIdIsRejected
```

- [ ] Write the tests · [ ] run, see them fail · [ ] implement · [ ] run, see them pass · [ ] full suite
- [ ] Commit `Add model base class, exceptions and field codec`

---

### Task 3: File storage and stations

**Files**
- Create: `repository/Repository.java`, `repository/FileRepository.java`
- Create: `model/Station.java`, `model/StationStatus.java`, `repository/StationRepository.java`
- Test: `repository/StationRepositoryTest.java` (uses JUnit's `@TempDir`)

**Produces**
```java
public interface Repository<T extends Identifiable> {
    List<T> findAll();          // a new list, in file order
    T findById(String id);      // null when there is none
    T require(String id);       // NotFoundException "<Label> <id> was not found"
    void save(T item);          // adds when the id is new, otherwise keeps its position; rewrites the file
    void delete(String id);     // NotFoundException when there is none; rewrites the file
    String nextId();            // "STN-004"; never gives out a number twice, even after deletes
}

public abstract class FileRepository<T extends Identifiable> implements Repository<T> {
    protected FileRepository(Path file, String label, String idPrefix, int idDigits, String header)
    public void load()                           // read the file into memory; a missing file means no records
    protected abstract T parse(String[] fields); // one line -> one object
    protected abstract String[] format(T item);  // one object -> one line's fields
}

public enum StationStatus { OPEN, CLOSED }       // getDisplayName(): "Open", "Closed"

public class Station extends Entity {
    public Station(String id, String name, String nameAr, StationStatus status)
    public String getName();   public void setName(String name)        // Check.notBlank(name, "Station name")
    public String getNameAr(); public void setNameAr(String nameAr)    // null -> ""
    public StationStatus getStatus(); public void setStatus(StationStatus status)
    public boolean isOpen()
    public String toString()                     // the name
}

public class StationRepository extends FileRepository<Station> {
    public StationRepository(Path file)          // label "Station", prefix "STN", 3 digits, header "id|name|nameAr|status"
    public Station findByName(String name)       // trimmed, ignoring case; null when there is none
}
```

**File layout** (the same for every repository)
```
#next=3
# id|name|nameAr|status
STN-001|Helwan|حلوان|OPEN
STN-002|Sadat|السادات|OPEN
```
- Line 1 holds the next id number. Lines that are blank or start with `#` are skipped when loading.
- Saving writes `<file>.tmp` in UTF-8 and then moves it over the real file (`REPLACE_EXISTING`, `ATOMIC_MOVE`).
- Any problem with a line (wrong number of fields, unknown enum value, anything `parse` throws) becomes `DataFileException` with the message `<file name> line <n>: <reason>`, for example `stations.txt line 3: expected 4 fields but found 2`.
- An I/O failure becomes `DataFileException` `Could not read <file name>` / `Could not save <file name>`.

**Tests**
```
savedStationsSurviveReload:   save Helwan (حلوان) and Sadat; a new repository on the same file after load()
                              has 2 stations, same names, same Arabic names, same order
fileIsReadableText:           after saving STN-001 Helwan: Files.readAllLines(file) equals
                              ["#next=2", "# id|name|nameAr|status", "STN-001|Helwan|حلوان|OPEN"]
updateKeepsPosition:          save A, B; A.setName("A2"); save(A); findAll() names are ["A2", "B"]; file line 3 starts "STN-001|A2|"
deleteRemovesTheLine:         after delete("STN-001") the file has no line starting "STN-001"
deleteUnknownIsNotFound:      assertThrows(NotFoundException.class, () -> repo.delete("STN-999"))
idsAreNeverReused:            save STN-001, STN-002; delete STN-002; reload from file; assertEquals("STN-003", repo.nextId())
missingFileMeansEmpty:        load() on a path that does not exist -> findAll().isEmpty(), nextId() is "STN-001"
separatorInNameSurvives:      a station named "A|B" comes back as "A|B" after reload
findByNameIgnoresCaseAndSpaces: findByName("  helwan ") returns the Helwan station
damagedLineNamesFileAndLine:  file with third line "STN-001|Helwan" -> DataFileException whose message
                              contains "stations.txt line 3"
unknownStatusNamesFileAndLine: "STN-001|Helwan||OPENED" -> DataFileException containing "stations.txt line 3"
```

- [ ] Write the tests · [ ] run, see them fail · [ ] implement · [ ] run, see them pass · [ ] full suite
- [ ] Commit `Add file-backed repository and station storage`

---

### Task 4: Users, roles and permissions

**Files**
- Create: `model/Permission.java`, `model/Role.java`, `model/User.java`, `model/Admin.java`, `model/OperationsController.java`, `model/TicketAgent.java`, `model/StationSupervisor.java`, `model/UserFactory.java`
- Create: `service/PasswordHasher.java`, `repository/UserRepository.java`
- Test: `model/UserPermissionsTest.java`, `service/PasswordHasherTest.java`, `repository/UserRepositoryTest.java`

**Produces**
```java
public enum Permission {
    MANAGE_STATIONS, MANAGE_LINES, MANAGE_TRAINS, MANAGE_USERS, MANAGE_FARES, VIEW_AUDIT_LOG,
    MANAGE_TRIPS, VIEW_TRIPS, VIEW_NETWORK,
    SELL_TICKETS, VALIDATE_TICKETS, REFUND_TICKETS,
    VIEW_REPORTS, EDIT_OWN_PROFILE
}

public enum Role {      // getDisplayName()
    ADMIN("Admin"), OPERATIONS_CONTROLLER("Operations controller"),
    TICKET_AGENT("Ticket agent"), STATION_SUPERVISOR("Station supervisor")
}

public abstract class User extends Entity {
    protected User(String id, String username, String passwordHash, String salt,
                   String fullName, String phone, String email, boolean active)
    public abstract Role getRole();
    public abstract Set<Permission> getPermissions();
    public boolean can(Permission permission)
    public String getUsername()                          // no setter
    public String getPasswordHash(); public String getSalt();
    public void setPassword(String passwordHash, String salt)
    public String getFullName(); public void setFullName(String fullName)   // Check.notBlank(fullName, "Full name")
    public String getPhone(); public void setPhone(String phone)
    public String getEmail(); public void setEmail(String email)
    public boolean isActive(); public void setActive(boolean active)
}

public class Admin extends User
public class OperationsController extends User
public class TicketAgent extends User
public class StationSupervisor extends TicketAgent      // its permissions are super.getPermissions() plus one

public final class UserFactory {
    public static User create(Role role, String id, String username, String passwordHash, String salt,
                              String fullName, String phone, String email, boolean active)
}

public final class PasswordHasher {      // PBKDF2WithHmacSHA256, 120000 iterations, 256-bit key, Base64 text
    public static String newSalt()       // 16 random bytes
    public static String hash(String password, String salt)
    public static boolean matches(String password, String salt, String expectedHash)
}

public class UserRepository extends FileRepository<User> {
    public UserRepository(Path file)     // "User", "USR", 3 digits,
                                         // header "id|username|passwordHash|salt|role|fullName|phone|email|active"
    public User findByUsername(String username)   // trimmed, ignoring case; null when there is none
}
```

**Permissions per role** (design §5 — exact)

| Role | Permissions |
|---|---|
| Admin | all fourteen |
| Operations controller | `MANAGE_TRIPS`, `VIEW_TRIPS`, `VIEW_NETWORK`, `EDIT_OWN_PROFILE` |
| Ticket agent | `VIEW_TRIPS`, `VIEW_NETWORK`, `SELL_TICKETS`, `VALIDATE_TICKETS`, `EDIT_OWN_PROFILE` |
| Station supervisor | everything a ticket agent has, plus `REFUND_TICKETS` |

**Tests**
```
UserPermissionsTest
  adminHasEverything:        assertEquals(EnumSet.allOf(Permission.class), admin.getPermissions())
  controllerPermissions:     assertEquals(EnumSet.of(MANAGE_TRIPS, VIEW_TRIPS, VIEW_NETWORK, EDIT_OWN_PROFILE), ...)
  agentPermissions:          assertEquals(EnumSet.of(VIEW_TRIPS, VIEW_NETWORK, SELL_TICKETS, VALIDATE_TICKETS, EDIT_OWN_PROFILE), ...)
  supervisorIsAgentPlusRefund: supervisor permissions == agent permissions + REFUND_TICKETS;
                             assertTrue(supervisor instanceof TicketAgent)
  agentCannotRefund:         assertFalse(agent.can(REFUND_TICKETS))
  factoryBuildsTheRightClass: for each Role, UserFactory.create(role, ...).getRole() == role
PasswordHasherTest
  sameInputSameHash, differentSaltDifferentHash,
  matchesOnlyTheRightPassword: matches("metro123", salt, hash) true; matches("Metro123", salt, hash) false
  hashDoesNotContainThePassword
UserRepositoryTest
  roleSurvivesReload:        a saved StationSupervisor is a StationSupervisor after reload
  inactiveFlagSurvivesReload
  findByUsernameIgnoresCase: findByUsername("ADMIN") finds "admin"
  unknownRoleNamesFileAndLine: role field "BOSS" -> DataFileException containing "users.txt line 3"
```

- [ ] Write the tests · [ ] run, see them fail · [ ] implement · [ ] run, see them pass · [ ] full suite
- [ ] Commit `Add user roles, permissions and password hashing`

---

### Task 5: Session, login, audit log, staff-user rules

**Files**
- Create: `service/Session.java`, `service/AuthService.java`, `service/UserService.java`
- Create: `model/AuditEntry.java`, `repository/AuditLog.java`
- Test: `service/AuthServiceTest.java`, `service/UserServiceTest.java`, `repository/AuditLogTest.java`
- Test support: `src/test/java/metro/TestData.java`, `src/test/java/metro/MutableClock.java`

**Produces**
```java
public class Session {
    public void login(User user); public void logout();
    public User getUser()                     // null when nobody is logged in
    public boolean isLoggedIn()
    public boolean can(Permission permission) // false when nobody is logged in
    public void require(Permission permission)// AccessDeniedException "You do not have permission to do this."
    public void requireLogin()                // AccessDeniedException "Please log in first."
}

public class AuditEntry { getTime(): LocalDateTime; getUsername(); getAction(); getDetail(); }

public class AuditLog {
    public AuditLog(Path file, Clock clock)
    public void record(String username, String action, String detail)   // appends one line: time|username|action|detail
    public List<AuditEntry> readAll()                                   // oldest first; missing file -> empty list
}

public class AuthService {
    public AuthService(UserRepository users, Session session, AuditLog audit)
    public User login(String username, String password)
    public void logout()
}

public class UserService {
    public UserService(UserRepository users, Session session, AuditLog audit)
    public List<User> listAll()                                   // MANAGE_USERS
    public List<User> search(String text)                         // MANAGE_USERS; username, full name, role name; blank -> all
    public User create(Role role, String username, String password,
                       String fullName, String phone, String email)   // MANAGE_USERS
    public User update(String id, String fullName, String phone, String email)   // MANAGE_USERS
    public User changeRole(String id, Role role)                  // MANAGE_USERS
    public User setActive(String id, boolean active)              // MANAGE_USERS
    public void resetPassword(String id, String newPassword)      // MANAGE_USERS
    public User updateOwnProfile(String fullName, String phone, String email)   // EDIT_OWN_PROFILE
    public void changeOwnPassword(String currentPassword, String newPassword)   // EDIT_OWN_PROFILE
    public String displayName(String userId)                      // full name, or the id when unknown; any logged-in user
}

// test support
public class MutableClock extends Clock { MutableClock(LocalDateTime start); void set(LocalDateTime t); void plusMinutes(long m); }
public class TestData {            // everything lives in a temp folder passed to the constructor
    public TestData(Path dir)      // clock fixed at 2026-11-15T10:00; one user per role, password "metro123",
                                   // created in the order admin, controller, agent, supervisor (USR-001 … USR-004)
    public final MutableClock clock; public final Session session; public final AuditLog audit;
    public final UserRepository users;
    public User admin, controller, agent, supervisor;
    public void loginAs(User user)
    // later tasks add their repositories, services and the fixture network to this class
}
```

**Rules and wording**
- Login: unknown username and wrong password give the same message, `Wrong username or password`. A deactivated account gives `This account is deactivated`. Username is trimmed and matched ignoring case.
- Username: 3–20 characters, letters, digits, `.` or `_` → `Username must be 3–20 letters, digits, dots or underscores`. Taken (ignoring case) → `Username is already taken`. Stored in lower case. It cannot be changed afterwards.
- Password: at least 6 characters → `Password must be at least 6 characters`.
- Email: blank, or contains `@` with text on both sides → `Email address is not valid`.
- Phone: blank, or 7–15 characters of digits, spaces and `+` → `Phone number is not valid`.
- Deactivating, or changing the role of, the only active admin → `The last active admin cannot be deactivated or changed`.
- Deactivating yourself → `You cannot deactivate your own account`.
- `changeOwnPassword` with a wrong current password → `Current password is wrong`.
- `changeRole` builds a new object of the new role's class with the same id and every other field copied, and saves it in place of the old one. If the changed user is the logged-in user, the session is given the new object.
- Audit actions: `LOGIN`, `LOGIN_FAILED`, `LOGOUT`, `USER_CREATED`, `USER_UPDATED`, `USER_ROLE_CHANGED`, `USER_ACTIVATED`, `USER_DEACTIVATED`, `PASSWORD_RESET`, `PROFILE_UPDATED`, `PASSWORD_CHANGED`. Detail is the affected username. Passwords are never written to the log.

**Tests**
```
AuthServiceTest
  loginPutsUserInSession:          login("admin", "metro123") returns admin; session.getUser() is admin
  loginIgnoresCaseAndSpaces:       login("  ADMIN ", "metro123") succeeds
  wrongPasswordAndUnknownUserLookTheSame: both throw ValidationException "Wrong username or password"
  deactivatedAccountIsRefused:     ValidationException "This account is deactivated"; session stays logged out
  logoutClearsSession
  loginAndFailureAreAudited:       audit.readAll() has LOGIN_FAILED then LOGIN for "admin"
UserServiceTest
  agentCannotListUsers:            as agent, listAll() throws AccessDeniedException
  nobodyLoggedInIsDenied:          with no login, listAll() throws AccessDeniedException
  createStoresHashedPassword:      created user's hash != "secret1"; PasswordHasher.matches("secret1", salt, hash)
  createWritesTheFile:             users.txt gains a line starting "USR-005|nour|"
  duplicateUsernameIgnoringCase:   create(..., "Admin", ...) -> "Username is already taken"
  badUsername / shortPassword / badEmail / badPhone: each exact message above
  profileUpdateKeepsIdAndUsername: after updateOwnProfile("New Name", "0100000000", "a@b.c") id and username are unchanged,
                                   full name changed, and the file line for that id shows the new name
  lastAdminCannotBeDeactivated:    setActive(admin.getId(), false) -> message above
  lastAdminCannotChangeRole
  cannotDeactivateYourself:        with a second admin created and logged in as the first
  changeRoleSwapsTheClass:         changeRole(agent.getId(), STATION_SUPERVISOR) -> users.require(id) instanceof StationSupervisor, same id
  changingOwnRoleUpdatesSession:   second admin changes own role to TICKET_AGENT -> session.can(MANAGE_USERS) is false
  changeOwnPasswordNeedsCurrent:   wrong current -> "Current password is wrong"; right one -> login with the new password works
  resetPasswordLetsUserLogIn
AuditLogTest
  appendsInOrder, missingFileIsEmpty,
  detailWithSeparatorSurvives:     record("admin", "X", "a|b") -> readAll().get(0).getDetail() equals "a|b"
```

- [ ] Write the tests · [ ] run, see them fail · [ ] implement · [ ] run, see them pass · [ ] full suite
- [ ] Commit `Add login, session, audit log and staff-user rules`

---

### Task 6: Application context, demo accounts, login and main window

**Files**
- Create: `metro/AppContext.java`, `service/DemoData.java`
- Create: `ui/Icons.java`, `ui/Screen.java`, `ui/NavItem.java`, `ui/Sidebar.java`, `ui/MainWindow.java`, `ui/LoginScreen.java`, `ui/DashboardScreen.java`
- Create: `dev/Screenshots.java`
- Modify: `ui/MetroApp.java`, `src/main/resources/metro/ui/app.css`
- Test: `AppContextTest.java`

**Produces**
```java
public class AppContext {
    public AppContext(Path dataDir, Clock clock)
    public Path getDataDir(); public Clock getClock(); public Session getSession(); public AuditLog getAuditLog();
    public AuthService getAuthService(); public UserService getUserService();
    // every later task that adds a repository or service adds its creation here and a getter
}

public final class DemoData {
    public static final String DEMO_PASSWORD = "metro123";
    public static void seedUsers(UserRepository users)
}

public final class Icons {                          // the only class that imports ikonli
    public static Node of(String name, int size)    // unknown name -> a plain dot, never an exception
}

public abstract class Screen extends BorderPane {
    protected Screen(AppContext context, String title)
    protected final AppContext context;
    public String getTitle()
    public void onShow()                            // called every time the screen is opened; default does nothing
    protected void showError(String message)        // red banner at the top of the screen, stays until the next action
    protected void showInfo(String message)         // green banner, disappears after 3 seconds
    protected boolean confirm(String question)      // modal OK / Cancel
    protected void run(Runnable action)             // runs it; a MetroException becomes showError(e.getMessage())
}

public class NavItem {
    public NavItem(String section, String label, String icon, Permission permission, Supplier<Screen> screen)
    // permission null = every logged-in user
}

public class Sidebar extends VBox {
    public Sidebar(List<NavItem> items, Session session, Consumer<NavItem> onSelect)
    public void select(NavItem item)
}

public class MainWindow extends BorderPane { public MainWindow(AppContext context, Runnable onLogout) }
public class LoginScreen extends StackPane  { public LoginScreen(AppContext context, Runnable onLoggedIn) }
public class DashboardScreen extends Screen { public DashboardScreen(AppContext context) }   // for now: "Welcome, <full name>" and the role
```

**Behaviour**
- `AppContext`: creates the data folder if missing; creates repositories; loads them in dependency order; when `users.txt` does not exist, seeds demo data. Start-up order for all repositories once they exist: users, stations, lines, trains, trips, fare tiers, tickets.
- File names in the data folder, fixed for the whole project: `users.txt`, `stations.txt`, `lines.txt`, `trains.txt`, `trips.txt`, `fares.txt`, `tickets.txt`, `audit.log`, `settings.txt`.
- Demo accounts — one per role, all active, all with `DEMO_PASSWORD`:

  | Username | Role | Full name |
  |---|---|---|
  | `admin` | Admin | System Admin |
  | `controller` | Operations controller | Operations Desk |
  | `agent` | Ticket agent | Ticket Window 1 |
  | `supervisor` | Station supervisor | Station Supervisor |

- `MetroApp`: builds `AppContext(Path.of("data"), Clock.systemDefaultZone())`. If that throws `DataFileException`, show one error dialog titled `A data file could not be read` with the exception message and exit; no stack trace on screen. Otherwise show `LoginScreen`; after login show `MainWindow`; Log out returns to `LoginScreen`.
- `LoginScreen`: card in the centre with the product name, username, password, a Log in button (Enter also submits), inline error text under the fields, and a small hint line `Demo accounts: admin, controller, agent, supervisor`.
- `MainWindow`: top bar (product name left; dark-mode toggle and a user menu showing `<full name> · <role>` with My profile and Log out on the right); `Sidebar` on the left; current `Screen` in the centre with its title as a heading. Opens Dashboard first.
- `Sidebar` shows an item only when `permission == null || session.can(permission)`. A section heading is shown only when at least one of its items is. The selected item is highlighted.
- Navigation table — the single source for sidebar order, labels and permissions. This task adds Dashboard only; later tasks add their rows.

  | Section | Label | Permission | Added in |
  |---|---|---|---|
  | — | Dashboard | none | Task 6 |
  | — | Network map | `VIEW_NETWORK` | Task 23 |
  | ADMINISTRATION | Stations | `MANAGE_STATIONS` | Task 11 |
  | ADMINISTRATION | Lines | `MANAGE_LINES` | Task 11 |
  | ADMINISTRATION | Trains | `MANAGE_TRAINS` | Task 11 |
  | SCHEDULE | Trips | `VIEW_TRIPS` | Task 14 |
  | TICKETS | Sell ticket | `SELL_TICKETS` | Task 19 |
  | TICKETS | Validate | `VALIDATE_TICKETS` | Task 19 |
  | TICKETS | Refunds | `REFUND_TICKETS` | Task 19 |
  | TICKETS | Fares | `MANAGE_FARES` | Task 19 |
  | REPORTS | Trip report | `VIEW_REPORTS` | Task 21 |
  | REPORTS | Ticket report | `VIEW_REPORTS` | Task 21 |
  | REPORTS | Revenue report | `VIEW_REPORTS` | Task 21 |
  | ACCOUNT | Staff users | `MANAGE_USERS` | Task 7 |
  | ACCOUNT | Audit log | `VIEW_AUDIT_LOG` | Task 25 |
  | ACCOUNT | My profile | `EDIT_OWN_PROFILE` | Task 7 |

- `dev.Screenshots`: a second entry point. It creates an `AppContext` on a fresh temporary folder, then for each demo account logs in, opens every sidebar item that account can see, and saves the window as `target/screenshots/<username>-<label in lower case with dashes>.png` (1280×800); it also saves `login.png`. It exits by itself. PNGs are written by copying the snapshot's pixels into a `BufferedImage` and calling `ImageIO.write`, so no extra JavaFX module is needed.

**Tests**
```
AppContextTest
  firstRunCreatesFolderAndDemoAccounts: new AppContext(tempDir.resolve("data"), clock) -> users.txt exists;
                                        4 users, one of each Role; login("admin", DemoData.DEMO_PASSWORD) works
  secondRunKeepsChanges:                deactivate "agent"; a new AppContext on the same folder -> agent still inactive
  damagedFileStopsStartUp:              users.txt with a 3-field line -> constructor throws DataFileException
                                        containing "users.txt line"
```

- [ ] Write the tests · [ ] run, see them fail · [ ] implement `AppContext` and `DemoData` · [ ] run, see them pass
- [ ] Build the screens and `Screenshots`
- [ ] Run `./mvnw -q javafx:run -Dmetro.mainClass=metro.dev.Screenshots`. Expected files: `login.png`, `admin-dashboard.png`, `controller-dashboard.png`, `agent-dashboard.png`, `supervisor-dashboard.png`. Look at them: login card centred; dashboard shows sidebar, top bar with the right name and role.
- [ ] Run the app. Log in as `admin` with a wrong password (inline error, exact wording), then correctly; toggle dark mode; log out; you are back at login. `data/users.txt` and `data/audit.log` exist and are readable.
- [ ] Break `data/users.txt` by deleting a field from one line and start the app: one clean error dialog naming the file and line. Restore the file.
- [ ] Full suite · [ ] Commit `Add login screen, main window and role-based sidebar`

---

### Task 7: List-screen base class, staff users, my profile

**Files**
- Create: `ui/CrudScreen.java`, `ui/FormDialog.java`, `ui/StatusBadge.java`
- Create: `ui/StaffUsersScreen.java`, `ui/UserFormDialog.java`, `ui/ProfileScreen.java`
- Modify: `ui/MainWindow.java` (two sidebar rows), `app.css`

**Produces**
```java
public abstract class CrudScreen<T extends Identifiable> extends Screen {
    protected CrudScreen(AppContext context, String title, String addButtonLabel)
    protected abstract List<T> loadItems(String searchText);     // called on open, on every search keystroke, after every change
    protected abstract void buildColumns(TableView<T> table);
    protected abstract void openForm(T existingOrNull);          // null = add
    protected void deleteItem(T item)                            // default does nothing; screens with delete override it
    protected boolean canAdd()                                   // default true; hides the add button when false
    protected List<Button> rowActions(T selected)                // default: Edit, Delete (Delete asks confirm() first)
    protected final void addColumn(TableView<T> table, String heading, Function<T, String> text)
    protected final void addBadgeColumn(TableView<T> table, String heading, Function<T, StatusBadge> badge)
    protected final void addFilter(Node control)                 // placed beside the search box; call refresh() from its listener
    protected final void refresh()
    protected final T getSelected()
}

public abstract class FormDialog extends Dialog<Boolean> {
    protected FormDialog(String title)
    protected final TextField addText(String label, String initialValue)
    protected final PasswordField addPassword(String label)
    protected final <E> ComboBox<E> addChoice(String label, List<E> options, E initialValue)
    protected final Spinner<Integer> addNumber(String label, int min, int max, int initialValue)
    protected final void addRow(String label, Node control)
    protected abstract void save();          // throw a MetroException to keep the dialog open with the message shown inline
    public boolean showAndSave()             // true when save() finished without an exception
}

public class StatusBadge extends Label {
    public enum Tone { NEUTRAL, GOOD, WARNING, BAD }
    public StatusBadge(String text, Tone tone)
}
```

**Screens**
- Layout of every `CrudScreen`: search box (prompt `Search…`) and filters on the left of one row, the add button on the right; table below; row-action buttons under the table, enabled only when a row is selected; when the table is empty it shows `Nothing to show yet.`
- **Staff users** (`CrudScreen<User>`): columns ID, Username, Full name, Role, Status (badge `Active` good / `Inactive` neutral). Add button `Add user`. Row actions: Edit (full name, phone, email), Change role, Reset password, Deactivate or Activate. There is no delete.
- `UserFormDialog` for add: role, username, password, full name, phone, email.
- **My profile** (`Screen`): ID, username and role shown as read-only, greyed fields; full name, phone, email editable with a Save button; a separate "Change password" block with current, new and repeat fields — when new and repeat differ the screen shows `The two new passwords do not match` and does not call the service.

- [ ] Build the base classes and the three screens; add the sidebar rows
- [ ] Screenshots run. Check `admin-staff-users.png` (four rows, badges), `admin-my-profile.png` (ID field visibly read-only), and that `agent-staff-users.png` does not exist.
- [ ] Click-through:
  1. As `admin`: Staff users → Add user, role Ticket agent, username `nour`. The row appears and `data/users.txt` has a new line starting `USR-005|nour|`.
  2. Try to add `NOUR` again → `Username is already taken` inside the dialog, dialog stays open.
  3. Log out, log in as `nour`: the sidebar shows Dashboard and My profile only.
  4. My profile: change the phone, Save → green notice; the `USR-005` line in the file shows the new phone; the ID could not be edited.
  5. As `admin`: deactivate `nour`; logging in as `nour` now says `This account is deactivated`.
  6. As `admin`: try to deactivate `admin` → refused with the last-admin message.
- [ ] Full suite · [ ] Commit `Add staff users and profile screens`
- [ ] Push, open the pull request for `phase-1-foundation`, wait for the green check, merge.
