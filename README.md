# reloading - digital reloading logbook

[![CI](https://github.com/thomashoell/reloading/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/thomashoell/reloading/actions/workflows/ci.yml)

*reloading* is a digital logbook for handloading ammunition: components (cases, bullets, powders), rifles, case preparation and, later on, loads and their results.

The project is at an early stage: only the persistence layer exists so far.

## Development

Requirements: Java 25, Docker (the tests start PostgreSQL 18 with Testcontainers).

```
./mvnw verify
```

To run the application, a PostgreSQL 18 database `reloading` (user and password `reloading`) is expected on `localhost:5432`, see [application.yaml](src/main/resources/application.yaml). The schema is created by Liquibase on startup.

```
./mvnw spring-boot:run
```

## Contributing

Changes go through pull requests, which are squash-merged. The PR title must be a [Conventional Commit](https://www.conventionalcommits.org/) (e.g. `feat: add primer entity`).
