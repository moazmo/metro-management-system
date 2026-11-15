# Metro Management System

[![build](https://github.com/moazmo/metro-management-system/actions/workflows/build.yml/badge.svg)](https://github.com/moazmo/metro-management-system/actions/workflows/build.yml)

Team project for the Programming Language 2 (Java) course.

A management system for a metro network: stations, lines and trains,
trip scheduling, ticket sales with fare calculation, reports, and
role-based staff access.

**Status:** in development.

- [Design](docs/design.md) — what we are building and how it fits together
- [Build plan](docs/plan.md) — 27 tasks in 8 phases, from empty project to discussion day

## Run it

You need **JDK 25**. Nothing else has to be installed: the build tool is
downloaded by the wrapper script the first time you use it.

```bash
./mvnw javafx:run     # start the application
./mvnw test           # run the tests
```

On Windows PowerShell type `.\mvnw.cmd` in place of `./mvnw`.
