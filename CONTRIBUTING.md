# Contributing

## Workflow

1. Keep `tastile-core` cloned next to this repository when working on Android builds.
2. Point `JAVA_HOME` at JDK 17 or 21.
3. Run `./gradlew verify` before pushing.
4. Use `./gradlew assembleDebug` before handing off Android-facing changes.

## Secrets And Local Configuration

- Commit only publishable client configuration.
- Store release signing credentials in `~/.gradle/gradle.properties`.
- Do not commit `local.properties`, keystores, or machine-specific JVM paths.

## Code And Repository Standards

- Prefer small, reviewable commits with clear intent.
- Add or update tests for behavior changes when practical.
- Keep generated outputs out of version control.
- Document operational or architectural decisions under `docs/`.

## Pull Request Checklist

- `./gradlew verify` passes.
- Android build assumptions are documented if they changed.
- New local setup requirements are reflected in `README.md` or `docs/development.md`.
- Secrets and machine-local paths are not introduced.
