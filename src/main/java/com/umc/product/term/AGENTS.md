# TERM KNOWLEDGE

## OVERVIEW

`term` owns terms, required consent status, agreement commands, and consent history.

## STRUCTURE

```text
term/
├── domain/                         # term, consent, log entities and enums
├── application/port/in             # term command/query UseCases
├── application/port/out            # term persistence ports
├── application/service/command     # term and agreement writes
├── application/service/query       # term/agreement/required status reads
├── application/service/evaluator   # consent requirement helpers
├── adapter/in/web                  # term controller
└── adapter/out/persistence         # term, consent, log persistence
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Term API | `adapter/in/web/TermController.java` | public/member term surface |
| Term commands | `TermCommandService.java` | create/update term flows |
| Agreements | `TermAgreementCommandService.java`, `TermAgreementQueryService.java` | consent write/read |
| Required status | `RequiredTermConsentStatusQueryService.java` | required consent checks |
| Consent logs | `TermConsentLogPersistenceAdapter.java` | audit/history storage |
| QueryDSL | `TermQueryRepository.java` | term list/status reads |

## CONVENTIONS

- Agreement writes should record consent state and log history together.
- Required-term status must be derived server-side from active required terms.
- Consent history should be append-friendly; avoid rewriting past logs.
- Controllers should use current member context for member agreements.
- Query services stay read-only and should not mark terms as agreed.
- Term type/version changes need tests for existing consent behavior.

## ANTI-PATTERNS

- Do not treat optional and required terms the same in required status checks.
- Do not delete or rewrite consent logs as a shortcut for correction.
- Do not accept member IDs from clients for agreement writes when authenticated context exists.
- Do not expose inactive terms unless the API explicitly asks for history.
