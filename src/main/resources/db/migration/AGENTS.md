# FLYWAY MIGRATION KNOWLEDGE

## OVERVIEW

This directory contains PostgreSQL/PostGIS Flyway migrations. Files are ordered by timestamp-like version segments.

## CONVENTIONS

- File names use `VYYYY.MM.DD.HH.MM__snake_case.sql`.
- Use a descriptive verb phrase after `__`: `create_*`, `add_*`, `alter_*`, `drop_*`, `migrate_*`, `seed_*`, `fix_*`, `restrict_*`.
- Never reuse a version. Gradle task `checkDuplicateFlywayMigrationVersions` enforces this before tests.
- Keep data migrations explicit and idempotency-aware where possible.
- Prefer additive migrations for production data safety. Drops and constraint tightening need clear sequencing.
- Match application entity changes with a migration in the same work item.
- Baseline is `2026.02.25.01.30`; do not rewrite historical migrations after they have shipped.
- PostgreSQL-specific SQL is acceptable; this project targets PostgreSQL with PostGIS.

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Baseline schema | `V2026.02.25.01.30__init_schema.sql` | Large initial schema |
| Flyway settings | `src/main/resources/application.yml` | baseline, out-of-order, validate behavior |
| Duplicate check | `build.gradle.kts` | custom Gradle migration version check |

## ANTI-PATTERNS

- Do not edit an already-applied migration to change production schema.
- Do not add two files with the same version prefix.
- Do not use vague names like `update.sql` or `change_table.sql`.
- Do not depend on Hibernate DDL generation; JPA uses `ddl-auto: validate`.
