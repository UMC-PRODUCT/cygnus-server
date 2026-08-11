# INHOUSE KNOWLEDGE

## OVERVIEW

`inhouse` models the UMC PRODUCT team org chart: 인원(`UmcProductMember`)과 로그인 계정 연동, Chapter(기능 조직), Department(목적 조직, 구 Squad), activity period, Leadership.

## STRUCTURE

```text
inhouse/
├── domain/                         # UmcProduct* aggregates, enums, vo
├── exception/                      # InhouseDomainException / InhouseErrorCode
├── application/port/in             # command/query UseCases and DTOs
├── application/port/out            # save/load Ports
├── application/service             # command/query services + UmcProductAccessPolicy
├── adapter/in/web                  # public/admin controllers and DTOs
└── adapter/out/persistence         # persistence adapters/repositories
```

## CONVENTIONS

- 도메인 예외는 `InhouseDomainException` + `InhouseErrorCode`를 사용한다.
- UMC PRODUCT Chapter는 전역 `Chapter`/`Gisu`와 별도 모델이며 UMC PRODUCT 기수를 두지 않는다.
- UMC PRODUCT 활동 기간은 `LocalDate`/DB `DATE`로 저장하고 종료일을 포함한다. 감사 시각은 기존 `Instant`를 유지한다.
- 인원의 기능 조직 소속은 `UmcProductChapterMembership`으로 저장하고 Chapter를 직접 참조한다.
- UMC PRODUCT Chapter 소속에는 별도 역할을 두지 않으며 Part와 Part Lead 모델을 추가하지 않는다.
- `UmcProductLeadership`은 Chapter 소속과 독립적으로 관리한다.
- member/challenger/storage/organization 도메인은 ID 참조 + 공개 Query UseCase 주입으로만 읽는다.
- 관리 권한 판정은 `UmcProductAccessPolicy`(중앙 운영진 또는 유효한 `LEAD`/`VICE_LEAD`)를 서비스 안에서 수행한다.

## ANTI-PATTERNS

- 컨트롤러가 persistence repository를 직접 호출하지 않는다.
- 엔티티에 `@OneToMany` 컬렉션을 추가하지 않는다.
- `Member` aggregate를 직접 참조하지 않는다(ID만).
