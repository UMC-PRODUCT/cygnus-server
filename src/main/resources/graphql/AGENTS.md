# GRAPHQL SCHEMA KNOWLEDGE

## OVERVIEW

This directory contains Spring GraphQL schema contracts for the pilot GraphQL API.

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Contract guide | `README.md` | ownership, composition, direct reference/projection/snapshot rules |
| Technical root/shared | `schema.graphqls`, `shared/` | root types, transport scalars, platform contracts |
| Challenger output | `challenger/output.graphqls` | challenger-owned enums |
| Form output | `form/output.graphqls` | canonical form structure and enums |
| Domain contracts | `{domain}/request.graphqls`, `{domain}/response.graphqls` | root/input and output declarations |
| Domain relation guide | `{domain}/README.md` | field ownership and cross-domain relationships |
| Runtime wiring | `src/main/java/com/umc/product/global/config/GraphQlRuntimeWiringConfig.java` | scalars and runtime wiring |
| Resolver code | `src/main/java/com/umc/product/*/adapter/in/graphql` | controller and DTO mapping |
| GraphQL docs | `docs/onboarding/graphql/README.md`, `docs/graphql-schema.md` | pilot design and schema snapshot |

## CONVENTIONS

- Schema changes must be mirrored in `*GraphQlController` and `*GraphQlResponse` DTOs.
- Treat all `*.graphqls` files as one runtime schema; each directory remains the provider domain's standard IDL.
- Keep `shared` limited to ownerless transport primitives and explicitly standardized platform contracts. Business enums and object types belong to their provider domain.
- Put root operations and inputs in `request.graphqls`; put outputs and enums in `response.graphqls` or `output.graphqls`.
- Reference provider types directly only when semantics are unchanged. Filtered, enriched, or snapshot data belongs to a consumer-owned type and converter.
- Document external IDs and transformed fields with GraphQL descriptions and update the owning directory's `README.md`.
- Prefer explicit non-null markers only when the resolver can always satisfy the field.
- Keep GraphQL request DTOs in `adapter/in/graphql/dto`.
- Resolver code should delegate to application inbound UseCases; it must not call repositories directly.
- Batch/nested fields should avoid N+1 by using batch mappings, IN queries, or DataLoader-aware patterns.
- Reuse one object type for the same domain identity; do not create `Summary`, `Detail`, or parent-prefixed types only to vary field selection.
- Keep enum names aligned with Java enum names unless a deliberate API compatibility reason exists.

## ANTI-PATTERNS

- Do not add schema fields without resolver/test updates.
- Do not expose internal IDs or operational fields that REST does not intentionally expose.
- Do not encode business rules only in GraphQL resolver code.
- Do not bypass domain/application validation for GraphQL-specific inputs.
