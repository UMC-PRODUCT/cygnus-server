# Recruiting GraphQL 구현 검증 Evidence

작성일: 2026-07-03

## 범위

이번 검증은 `origin/develop`의 GraphQL 기반 설정을 `feature/recruiting-domain`에 병합한 뒤, recruiting 도메인 GraphQL 표면과 track 변경이 의도대로 동작하는지 확인한 결과를 요약한다.

검증 대상은 다음과 같다.

- recruiting Query/Mutation schema와 resolver가 REST와 동일한 application UseCase를 호출한다.
- CSV 다운로드는 GraphQL에 추가하지 않고 REST 전용으로 유지한다.
- `ChallengerTrack`에서 `INFRA_CORE`를 제거하고 `WEB_PRODUCT_ENGINEER`, `MOBILE_PRODUCT_ENGINEER`, `INFRA_PLUS`를 유지한다.
- GraphQL adapter가 REST adapter DTO, outbound adapter, JPA repository, recruiting domain entity를 직접 참조하지 않는다.
- 비로그인 지원서 생성에서 입력 `applicantMemberId`를 신뢰하지 않는다.
- admin/interview resolver는 application, form, round, assignment가 요청 season에 속하는지 permission/usecase 실행 전에 확인한다.
- 잘못된 interview datetime 입력은 GraphQL `BAD_REQUEST`로 반환한다.

## 실행 검증

집중 테스트 명령:

```bash
./gradlew test \
  --tests com.umc.product.common.domain.enums.ChallengerTrackTest \
  --tests com.umc.product.global.config.GraphQlRuntimeWiringConfigTest \
  --tests com.umc.product.recruiting.adapter.in.graphql.RecruitingGraphQlControllerTest \
  --tests com.umc.product.recruiting.adapter.in.graphql.RecruitingGraphQlSecurityTest \
  --tests com.umc.product.recruiting.adapter.in.graphql.RecruitingGraphQlExceptionAdviceTest \
  --tests com.umc.product.recruiting.adapter.in.graphql.RecruitingGraphQlArchitectureTest \
  --tests "*RecruitingPersistenceAdapterTest" \
  --tests com.umc.product.recruiting.adapter.in.graphql.RecruitingGraphQlSurfaceTest
```

결과:

- Gradle 결과: `BUILD SUCCESSFUL in 37s`
- durable JUnit XML 8개 확인: 모두 `failures=0`, `errors=0`
- `RecruitingGraphQlExceptionAdviceTest`: invalid `startsAt` 입력이 `COMMON-400` 계열 `BAD_REQUEST`로 반환되고 command usecase가 호출되지 않음을 확인
- `RecruitingGraphQlSecurityTest`: anonymous memberId spoofing, cross-season application, cross-season assignment 방어 확인
- `RecruitingGraphQlSurfaceTest`: recruiting Query/Mutation introspection과 CSV REST-only 정책 확인

## 경계 검증

금지 검색 명령:

```bash
rg -n "INFRA_CORE|adapter\.in\.web|adapter\.out|JpaRepository" \
  src/main/java/com/umc/product/recruiting/adapter/in/graphql \
  src/main/resources/graphql \
  src/main/resources/db/migration/V2026.07.02.10.00__add_challenger_track.sql \
  src/main/resources/db/migration/V2026.07.02.10.10__create_recruiting_core.sql
```

결과: 금지 문자열 매치 없음.

## Review / QA 판정

| 항목 | 판정 | 근거 |
|---|---|---|
| Code Review | `APPROVE`, `CLEAR` | blocker 없음, 이전 P1/P2 보안 및 경계 이슈 닫힘 유지 |
| QA Refresh | `PASS` | invalid datetime, durable XML, GraphQL surface, forbidden boundary, security regression 확인 |
| Final Gate | `APPROVE` | C001-C003 충족, quality gate와 evidence 일관성 확인 |

## 남은 주의사항

- GraphQL 표면 검증은 live HTTP `/graphql` 호출이 아니라 `GraphQlTester`와 schema introspection 기반이다. 현재 테스트 환경에서 resolver와 schema wiring을 직접 실행했기 때문에 코드 레벨 회귀 방어에는 충분하지만, 배포 환경 smoke test에서는 실제 HTTP endpoint도 별도로 확인해야 한다.
- `.omo/evidence` 원본 실행 로그와 JUnit XML은 로컬 검증 산출물이며, 이 문서에는 커밋 리뷰에 필요한 요약만 남긴다.
