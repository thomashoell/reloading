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

Without login, for local development only (every request runs as the user `developer`, the application only listens on `localhost`):

```
./mvnw spring-boot:run -Dspring-boot.run.profiles=noauth
```

By default the application requires an OAuth2/OIDC login (e.g. Keycloak; redirect URI `<base URL>/login/oauth2/code/keycloak`) and doesn't start without these environment variables:

```
SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_ISSUER_URI=https://<keycloak>/realms/<realm>
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KEYCLOAK_CLIENT_ID=<client id>
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KEYCLOAK_CLIENT_SECRET=<client secret>
```

## Data model

Components are split into a product and its purchased lots (`casing`/`casing_lot`, `bullet`/`bullet_lot`, `powder`/`powder_lot`, `primer`/`primer_lot`). A `processed_case` is a batch of cases of one lot that were prepared the same way, a `load` a batch of cartridges made from such a batch and one lot of each component, and a `shooting_result` what a load did in a rifle.

All measures are stored in SI base units: lengths in m, masses in kg, velocities in m/s, temperatures in K. Converting to and from grains, inches etc. is a concern of the user interface.

## Contributing

Changes go through pull requests against `develop`, which are squash-merged. `develop` is the default branch until the application is usable; it is then merged to `main`, which publishes the first release. The PR title must be a [Conventional Commit](https://www.conventionalcommits.org/) (e.g. `feat: add primer entity`).

## Releases

Nothing is released from `develop`. Every merge to `main` runs the [release workflow](.github/workflows/release.yml): after the full CI pipeline, [semantic-release](https://github.com/semantic-release/semantic-release) derives the version from the Conventional Commits since the last tag, updates [CHANGELOG.md](CHANGELOG.md), pushes the release commit and tag (no `v` prefix) and creates the GitHub release with the jar attached.

- `fix:`, `perf:`, `build(deps):` → patch; `feat:` → minor; `feat!:` / `BREAKING CHANGE:` footer in the PR description → major; `docs`, `ci`, `chore`, `refactor`, `test` → no release.
- Don't edit CHANGELOG.md or create tags by hand.
- The release commit is pushed by the GitHub App `thomashoell-release-bot`, the only actor allowed to bypass the `main` ruleset.
