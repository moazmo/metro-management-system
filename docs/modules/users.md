# Users — explain-guide

Owner: member 2. Staff accounts, roles and permissions, logging in and out,
the profile, staff management and the audit log.

## 1. What this module does

Specification module 5:

- 5a — *"Login and Logout: Staff users can log in and log out of the system."*
- 5b — *"Profile Management: Users can update their personal information except for their ID."*
- 5c — *"Role-Based Access: The system should restrict features according to each user's role and permissions."*

Rubric: *"User Authorization — The user can only view and use the permissions assigned to them."*

## 2. Classes

| Class | Layer | What it is for |
|---|---|---|
| `User` | model | Abstract staff account: name, contact, password hash, active flag |
| `Admin` | model | May do everything |
| `OperationsController` | model | Plans and controls the timetable |
| `TicketAgent` | model | Sells and validates tickets |
| `StationSupervisor` | model | A ticket agent who may also refund |
| `Role` | model | The four roles, with display names |
| `Permission` | model | The fourteen things a role may be allowed to do |
| `UserFactory` | model | Creates the right `User` subclass for a role |
| `AuditEntry` | model | One line of the audit log |
| `PasswordHasher` | service | Hashes passwords with a random salt (PBKDF2) |
| `Session` | service | Who is logged in; `can(...)` and `require(...)` |
| `AuthService` | service | Login and logout |
| `UserService` | service | Staff-user rules and own-profile changes |
| `UserRepository` | repository | `users.txt` |
| `AuditLog` | repository | `audit.log`, append-only |
| `LoginScreen` | ui | The login card |
| `StaffUsersScreen` | ui | Admin list of accounts with role, password and activation actions |
| `UserFormDialog` | ui | Add or edit an account |
| `ProfileScreen` | ui | My profile: details and password; ID, username, role read-only |
| `AuditLogScreen` | ui | Searchable audit log, newest first |

## 3. OOP concepts in this module

**Encapsulation** — [User.java](../../src/main/java/metro/model/User.java).
All fields are private. The id (from `Entity`) and the username are final with
no setter: that is spec 5b, "except for their ID", enforced by the class itself.
`setFullName` refuses a blank name; only a hash is ever stored for the password.

**Inheritance** — [StationSupervisor.java](../../src/main/java/metro/model/StationSupervisor.java)
extends `TicketAgent`, which extends `User`, which extends `Entity`. A supervisor
*is a* ticket agent: it calls `super.getPermissions()` and adds `REFUND_TICKETS`.

**Polymorphism** — `user.getPermissions()` is abstract in `User` and different
in each subclass. `Session.can(p)` calls `user.can(p)` without knowing the
subclass; the right permissions come back. The rest of the program never asks
"is this an admin?".

**Abstraction** — `User` is abstract: an account is always a concrete role.
`getRole()` and `getPermissions()` are abstract methods every role must answer.

**Relationships** — `Session` *has a* `User` (association, changes at login and
logout). Every service holds the same `Session`. A `Ticket` refers to its seller
by user id (a loose association, because a role change replaces the object).

## 4. The data file

`data/users.txt`:

```
#next=5
# id|username|passwordHash|salt|role|fullName|phone|email|active
USR-001|admin|/spyMK8WNlrCo2R/Qn09w46D88Q8MrJvBrgNWiwd82E=|xfHRW7I4eFysFL0KWn736A==|ADMIN|System Admin|||true
```

| Field | Meaning |
|---|---|
| `USR-001` | id, never changes |
| `admin` | username, stored in small letters, never changes |
| `/spyMK…` | PBKDF2 hash of the password; the password itself is never stored |
| `xfHRW…` | the random salt mixed into the hash |
| `ADMIN` | the role; decides which class is created when loading |
| `System Admin` | full name |
| (empty) (empty) | phone and email, optional |
| `true` | active; `false` means the account cannot log in |

`data/audit.log` lines look like `2026-11-16T10:02:11|agent|TICKET_SOLD|TKT-000431…TKT-000432 Helwan → Sadat x2`.

## 5. Rules and where they live

| Rule | Method | Test |
|---|---|---|
| Each role has exactly its permissions | `Admin/…/StationSupervisor.getPermissions` | `UserPermissionsTest.controllerPermissions`, `agentPermissions` |
| A supervisor is an agent plus refunds | `StationSupervisor.getPermissions` | `UserPermissionsTest.supervisorIsAgentPlusRefund` |
| Unknown user and wrong password look the same | `AuthService.login` | `AuthServiceTest.wrongPasswordAndUnknownUserLookTheSame` |
| A deactivated account cannot log in | `AuthService.login` | `AuthServiceTest.deactivatedAccountIsRefused` |
| Every operation checks the permission | `Session.require` | `UserServiceTest.agentCannotListUsers`, `nobodyLoggedInIsDenied` |
| Passwords are stored only as salted hashes | `PasswordHasher.hash`, `UserService.create` | `UserServiceTest.createStoresHashedPassword` |
| Usernames are unique ignoring case | `UserService.create` | `UserServiceTest.duplicateUsernameIgnoringCase` |
| Profile changes keep the ID and username | `UserService.updateOwnProfile` | `UserServiceTest.profileUpdateKeepsIdAndUsername` |
| The last active admin cannot be deactivated or demoted | `UserService.setActive`, `changeRole` | `UserServiceTest.lastAdminCannotBeDeactivated`, `lastAdminCannotChangeRole` |
| You cannot deactivate yourself | `UserService.setActive` | `UserServiceTest.cannotDeactivateYourself` |
| Changing a role changes the object's class | `UserService.changeRole` | `UserServiceTest.changeRoleSwapsTheClass` |
| Changing your own role takes effect at once | `UserService.changeRole` | `UserServiceTest.changingOwnRoleUpdatesSession` |
| Passwords never reach the audit log | `UserService.record` | `UserServiceTest.passwordsNeverReachTheAuditLog` |

## 6. One action from click to file: updating my phone number

1. `ProfileScreen`: the user types a phone number and clicks **Save**.
2. The button runs `run(this::saveDetails)` (from `Screen`), so any refusal becomes a red banner.
3. `saveDetails` calls `userService.updateOwnProfile(name, phone, email)`.
4. `UserService` calls `session.require(Permission.EDIT_OWN_PROFILE)`; every role has it.
5. `applyDetails` checks the name (not blank), the phone (7–15 digits, spaces or +) and the email (text on both sides of @). A wrong value throws a `ValidationException`.
6. It calls the setters on the logged-in `User` object, then `users.save(user)`.
7. `FileRepository.save` replaces the user in its list and rewrites `users.txt` through a temporary file.
8. `UserService` adds `PROFILE_UPDATED` to `audit.log`; the screen shows *Profile saved.*

## 7. Questions you may be asked

1. **How do you stop a user changing their ID?** There is no `setId` anywhere, and the id field in `Entity` is `final`. The profile screen shows the ID in a read-only field, but even code could not change it.
2. **Why is `User` abstract?** Every account must be a real role. An abstract class lets us force each role to answer `getRole()` and `getPermissions()`.
3. **Why does `StationSupervisor` extend `TicketAgent` and not `User`?** A supervisor can do everything an agent can, plus refunds. Inheriting avoids copying the agent's list and keeps them in step.
4. **Where is authorization checked?** Twice: the sidebar hides what the role may not use, and every service method starts with `session.require(...)`. The second check is the one that matters.
5. **Why does a wrong username give the same message as a wrong password?** So nobody can find out which usernames exist by trying.
6. **How are passwords stored?** As a PBKDF2 hash with a random salt, 120,000 rounds. To check a login we hash the typed password with the stored salt and compare.
7. **What is a salt for?** Two users with the same password get different hashes, and pre-computed tables of hashes become useless.
8. **What happens when an admin changes someone's role?** A new object of the new role's class is created with the same id and details and saved in place of the old one. If that user is logged in, the session gets the new object, so the new permissions apply immediately.
9. **Why can't the last admin be deactivated?** Nobody could then manage users or the network; the system would be locked.
10. **Why are users deactivated rather than deleted?** Tickets and log entries refer to them; deleting would leave history pointing at nobody.
11. **What is in the audit log, and what is never in it?** Who did what and when: logins, failed logins, changes, sales, refunds. Never a password.
12. *Change it:* **let ticket agents refund too.** Add `Permission.REFUND_TICKETS` to `TicketAgent.getPermissions()`. The Refunds menu item appears for agents and the service allows it; `agentCannotRefund` would then fail, as it should.

## 8. Try it yourself

1. Add a new role `Auditor` that may only view reports and the audit log: a subclass, a `Role` value, a case in `UserFactory`. Log in as one.
2. Make the minimum password length 8 and fix the test that pins the old message.
3. Find in `audit.log` the line written when you logged in, and explain each field.
