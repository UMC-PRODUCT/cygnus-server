# SURVEY KNOWLEDGE

## OVERVIEW

`survey` owns reusable form, section, question, option, answer, response, and vote models used by other domains.

## STRUCTURE

```text
survey/
├── domain/                         # form, question, option, answer, vote entities and enums
├── application/port/in             # form/question/answer command and query UseCases
├── application/port/out            # persistence ports
├── application/service             # vote service and shared survey orchestration
├── application/service/command     # form/section/question/answer writes
├── application/service/query       # form/section/question/answer reads
└── adapter/out/persistence         # adapters and QueryDSL repositories
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Form writes | `application/service/command/FormCommandService.java` | form lifecycle |
| Sections | `FormSectionCommandService.java`, `FormSectionQueryService.java` | section ordering |
| Questions | `QuestionCommandService.java`, `QuestionQueryService.java` | question lifecycle |
| Options | `QuestionOptionCommandService.java`, `QuestionOptionQueryService.java` | choice options |
| Answers | `AnswerCommandService.java`, `AnswerQueryService.java` | answer storage/read |
| Responses | `FormResponseCommandService.java`, `FormResponseQueryService.java` | response lifecycle |
| Votes | `VoteService.java`, `VoteQueryService.java` | vote-specific behavior |
| QueryDSL | `adapter/out/persistence/*QueryRepository.java` | nested form projections |

## CONVENTIONS

- This package currently has no REST controller; other domains should access it through UseCases.
- Keep form, section, question, option, answer, and response rules separated.
- Preserve ordering semantics for sections, questions, and options.
- Validate answer shape against question type before persisting.
- Query repositories should assemble nested form/response projections without N+1 paths.
- Cross-domain consumers should store survey IDs, not survey aggregate objects.

## ANTI-PATTERNS

- Do not expose survey persistence adapters directly to project or notice flows.
- Do not accept answers without checking the current form/question definition.
- Do not reorder sections or options implicitly during query mapping.
- Do not hide vote behavior inside generic answer services.
