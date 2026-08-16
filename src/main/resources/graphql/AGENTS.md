# GRAPHQL SCHEMA KNOWLEDGE

## OVERVIEW

This directory contains Spring GraphQL schema contracts for the pilot GraphQL API.

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Member schema | `member.graphqls` | `me`, `member`, `members` and member nested types |
| Organization schema | `organization.graphqls` | gisu, chapter, school, organization payloads |
| Project schema | `project.graphqls` | project search/detail and application/form nested types |
| Runtime wiring | `src/main/java/com/umc/product/global/config/GraphQlRuntimeWiringConfig.java` | scalars and runtime wiring |
| Resolver code | `src/main/java/com/umc/product/*/adapter/in/graphql` | controller and DTO mapping |
| GraphQL docs | `docs/onboarding/graphql.md`, `docs/graphql-schema.md` | pilot design and schema snapshot |

## CONVENTIONS

- Schema changes must be mirrored in `*GraphQlController` and `*GraphQlResponse` DTOs.
- Prefer explicit non-null markers only when the resolver can always satisfy the field.
- Keep GraphQL request DTOs in `adapter/in/graphql/dto`.
- Resolver code should delegate to Query UseCases; it must not call repositories directly.
- Batch/nested fields should avoid N+1 by using batch mappings, IN queries, or DataLoader-aware patterns.
- Keep enum names aligned with Java enum names unless a deliberate API compatibility reason exists.

## ANTI-PATTERNS

- Do not add schema fields without resolver/test updates.
- Do not expose internal IDs or operational fields that REST does not intentionally expose.
- Do not encode business rules only in GraphQL resolver code.
- Do not bypass domain/application validation for GraphQL-specific inputs.
