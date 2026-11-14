# reloading - digital reloading logbook

[![Release](https://github.com/thomashoell/reloading/actions/workflows/release.yml/badge.svg?branch=main)](https://github.com/thomashoell/reloading/actions/workflows/release.yml)

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

Changes go through pull requests against `develop`, which are squash-merged. `develop` is the default branch until the application is usable; it is then merged to `main`, which publishes the first release. The PR title must be a [Conventional Commit](https://www.conventionalcommits.org/) (e.g. `feat: add primer entity`).

## Releases

Nothing is released from `develop`. Every merge to `main` runs the [release workflow](.github/workflows/release.yml): after the full CI pipeline, [semantic-release](https://github.com/semantic-release/semantic-release) derives the version from the Conventional Commits since the last tag, updates [CHANGELOG.md](CHANGELOG.md), pushes the release commit and tag (no `v` prefix) and creates the GitHub release with the jar attached.

- `fix:`, `perf:`, `build(deps):` → patch; `feat:` → minor; `feat!:` / `BREAKING CHANGE:` footer in the PR description → major; `docs`, `ci`, `chore`, `refactor`, `test` → no release.
- Don't edit CHANGELOG.md or create tags by hand.
- The release commit is pushed by the GitHub App `thomashoell-release-bot`, the only actor allowed to bypass the `main` ruleset.
