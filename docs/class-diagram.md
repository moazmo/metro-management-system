# Class diagrams

Drawn from the finished code. Every arrow below exists in `src/main/java`.
Fields and methods are left out except where they explain an arrow; open the
class to see the rest.

Arrow key: `<|--` inherits (extends), `<|..` implements, `*--` composition
(the part belongs to the whole), `o--` aggregation (shared parts), `-->` uses /
refers to.

## 1. Model — the records and how they relate

```mermaid
classDiagram
    direction TB
    class Identifiable {
        <<interface>>
        +getId() String
    }
    class Entity {
        <<abstract>>
        -String id
        +getId() String
    }
    Identifiable <|.. Entity

    class User {
        <<abstract>>
        -String username
        +getRole()* Role
        +getPermissions()* Set~Permission~
        +can(Permission) boolean
    }
    Entity <|-- User
    User <|-- Admin
    User <|-- OperationsController
    User <|-- TicketAgent
    TicketAgent <|-- StationSupervisor

    class Ticket {
        <<abstract>>
        -double baseFare
        +discountRate()* double
        +getFare() double
        +markUsed(LocalDateTime)
        +refund(String, LocalDateTime)
    }
    Entity <|-- Ticket
    Ticket <|-- StandardTicket
    Ticket <|-- StudentTicket
    Ticket <|-- SeniorTicket

    class Line {
        -List~Station~ stations
        +areAdjacent(Station, Station) boolean
    }
    class Trip {
        +overlaps(LocalDateTime, LocalDateTime, int) boolean
        +changeStatus(TripStatus)
    }
    Entity <|-- Station
    Entity <|-- Line
    Entity <|-- Train
    Entity <|-- Trip
    Entity <|-- FareTier

    Line o-- "2..*" Station : ordered stations
    Trip --> Train : runs on
    Trip --> Line : along
    Ticket --> Station : from
    Ticket --> Station : to

    class Journey
    class Leg
    Journey *-- "1..*" Leg : legs
    Journey --> Station : stations passed
    Leg --> Line

    Station --> StationStatus
    Train --> TrainStatus
    Trip --> TripStatus
    Trip --> Direction
    Ticket --> TicketStatus
    User --> Role
    User --> Permission
```

`UserFactory` and `TicketFactory` create the right subclass for a `Role` or a
`TicketType`. `TicketCheck` and `AuditEntry` are small result objects.

## 2. Storage — repositories and data files

```mermaid
classDiagram
    direction TB
    class Repository~T~ {
        <<interface>>
        +findAll() List~T~
        +findById(String) T
        +require(String) T
        +save(T)
        +delete(String)
        +nextId() String
    }
    class FileRepository~T~ {
        <<abstract>>
        +load()
        +saveAll(List~T~)
        #parse(String[])* T
        #format(T)* String[]
    }
    Repository <|.. FileRepository
    FileRepository <|-- UserRepository
    FileRepository <|-- StationRepository
    FileRepository <|-- LineRepository
    FileRepository <|-- TrainRepository
    FileRepository <|-- TripRepository
    FileRepository <|-- FareTierRepository
    FileRepository <|-- TicketRepository

    LineRepository --> StationRepository : turns ids into stations
    TripRepository --> TrainRepository
    TripRepository --> LineRepository
    TicketRepository --> StationRepository

    class AuditLog {
        +record(String, String, String)
        +readAll() List~AuditEntry~
    }
    class Settings {
        +get(String, String) String
        +set(String, String)
    }
```

| Class | File |
|---|---|
| `UserRepository` | `users.txt` |
| `StationRepository` | `stations.txt` |
| `LineRepository` | `lines.txt` |
| `TrainRepository` | `trains.txt` |
| `TripRepository` | `trips.txt` |
| `FareTierRepository` | `fares.txt` |
| `TicketRepository` | `tickets.txt` |
| `AuditLog` | `audit.log` (append-only) |
| `Settings` | `settings.txt` |

## 3. Services — the rules

```mermaid
classDiagram
    direction LR
    class Session {
        +can(Permission) boolean
        +require(Permission)
        +requireLogin()
    }
    Session --> User : logged in

    AuthService --> UserRepository
    UserService --> UserRepository
    StationService --> StationRepository
    StationService --> LineRepository
    StationService --> TicketRepository
    LineService --> LineRepository
    LineService --> TripRepository
    TrainService --> TrainRepository
    TrainService --> TripRepository
    TripService --> TripRepository
    RoutePlanner --> LineRepository
    FareService --> FareTierRepository
    TicketService --> TicketRepository
    TicketService --> RoutePlanner
    TicketService --> FareService
    ReportService --> TripRepository
    ReportService --> TicketRepository
    DashboardService --> TripRepository
    DashboardService --> TicketRepository

    class FarePolicy {
        <<interface>>
        +baseFare(int) double
    }
    FarePolicy <|.. TieredFarePolicy
    FareService ..> TieredFarePolicy : builds

    class Report {
        <<abstract>>
        +getColumns()* List~String~
        +getRows()* List~String[]~
        +exportCsv(Path)
    }
    Report <|-- TripReport
    Report <|-- TicketReport
    Report <|-- RevenueReport
    TicketReport *-- JourneyCount
    RevenueReport *-- DayRevenue
    ReportService ..> Report : creates
```

Every service also holds the one `Session` and the `AuditLog`; those arrows are
left out to keep the picture readable. `AppContext` creates all repositories and
services once, at start-up.

## 4. Errors

```mermaid
classDiagram
    RuntimeException <|-- MetroException
    MetroException <|-- ValidationException
    MetroException <|-- NotFoundException
    MetroException <|-- AccessDeniedException
    MetroException <|-- DataFileException
    MetroException <|-- ScheduleConflictException
    ScheduleConflictException --> Trip : clashing trips
```

## 5. Screens

```mermaid
classDiagram
    direction TB
    class Screen {
        <<abstract>>
        +onShow()
        #run(Runnable)
        #showError(String)
    }
    class CrudScreen~T~ {
        <<abstract>>
        #loadItems(String)* List~T~
        #buildColumns(TableView)*
        #openForm(T)*
    }
    class ReportScreen {
        <<abstract>>
        #generate(LocalDate, LocalDate)* Report
        #buildChart(Report)* Node
    }
    class FormDialog {
        <<abstract>>
        #save()*
    }
    Screen <|-- CrudScreen
    Screen <|-- ReportScreen
    Screen <|-- DashboardScreen
    Screen <|-- NetworkMapScreen
    Screen <|-- SellTicketScreen
    Screen <|-- ValidateTicketScreen
    Screen <|-- RefundsScreen
    Screen <|-- ProfileScreen
    Screen <|-- AuditLogScreen
    CrudScreen <|-- StationsScreen
    CrudScreen <|-- LinesScreen
    CrudScreen <|-- TrainsScreen
    CrudScreen <|-- TripsScreen
    CrudScreen <|-- StaffUsersScreen
    CrudScreen <|-- FaresScreen
    ReportScreen <|-- TripReportScreen
    ReportScreen <|-- TicketReportScreen
    ReportScreen <|-- RevenueReportScreen
    FormDialog <|-- UserFormDialog
    FormDialog <|-- StationFormDialog
    FormDialog <|-- LineFormDialog
    FormDialog <|-- TrainFormDialog
    FormDialog <|-- TripFormDialog

    MainWindow *-- Sidebar
    Sidebar --> NavItem
    NavItem ..> Screen : creates
    NetworkMapView --> MapLayout
    NetworkMapScreen *-- NetworkMapView
    MetroApp --> AppContext
    MetroApp ..> LoginScreen
    MetroApp ..> MainWindow
```

Small forms that belong to one screen are nested inside it and also extend
`FormDialog`: `StaffUsersScreen.RoleDialog`, `StaffUsersScreen.PasswordDialog`,
`TripsScreen.DelayDialog` and `FaresScreen.TierDialog`.
