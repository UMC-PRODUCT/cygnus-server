# Recruiting 테스트 케이스

- 테스트 파일: 15개
- 테스트 케이스: 74개
- 분류 기준: `Controller`, `UseCase`, `Repository`, `Domain`, `Permission`

| 카테고리 | 케이스 수 | 주요 파일 |
|---|---:|---|
| Controller / Inbound Adapter | 14 | `RecruitingPublicControllerTest`, `RecruitingApplicationControllerTest`, `RecruitingAdminControllerTest` |
| UseCase / Application Service | 37 | command/query service tests |
| Repository / Outbound Persistence | 7 | `RecruitingPersistenceAdapterTest` |
| Domain | 8 | `RecruitingCoreDomainTest`, `RecruitingEvaluationDomainTest` |
| Permission | 8 | `RecruitingPermissionEvaluatorTest` |

## Controller / Inbound Adapter

| 테스트 파일 | 검증 범위 |
|---|---|
| `RecruitingPublicControllerTest` | 학교별 public form 목록, 지원서 번호와 applicant identity key 기반 익명 결과 조회, raw email 미노출 |
| `RecruitingApplicationControllerTest` | draft 생성, draft 답변 수정, 제출 IP 전달, 철회 요청 매핑 |
| `RecruitingAdminControllerTest` | 시즌 생성, form-track 연결, 서류/최종 결정자 memberId 전달, 면접 일정 후보 반환, 평가 visibility 결과 반환, CSV attachment와 개인정보 제외, 상태 요약 |

## UseCase / Application Service

| 테스트 파일 | 검증 범위 |
|---|---|
| `RecruitingSeasonCommandServiceTest` | 시즌 생성/중복, 시즌 상태 전이, 본모집/추가모집 차수 생성과 중복 차수 차단 |
| `RecruitingApplicationFormCommandServiceTest` | survey form 연결, publish/close 전이, survey publish 위임 |
| `RecruitingApplicationCommandServiceTest` | draft/update/submit/cancel, 같은 차수 중복 차단, 다른 학교 지원 차단, 이전 실패/철회 후 재지원 허용, 이전 합격/진행 중 재지원 차단 |
| `RecruitingDecisionCommandServiceTest` | 서류/최종 합불 전이, 최종 합격 한 건 제한, 중앙 총괄단 이상 등록 확정, challenger track 전달 |
| `RecruitingInterviewCommandServiceTest` | 면접 배정, 면접 스킵, survey 일정 겹침 port 호출, 안내 이메일 port 호출, 평가 저장/제출 |
| `RecruitingInterviewQueryServiceTest` | 본인 평가 제출 전/후 타 면접관 평가 visibility |
| `RecruitingQueryServiceTest` | 익명 결과 조회 identity 검증, 상태 요약 count |
| `RecruitingCsvExportServiceTest` | CSV header/body, comma/quote escaping, raw email과 지원서 본문 제외 |

## Repository / Outbound Persistence

`RecruitingPersistenceAdapterTest`는 recruiting aggregate의 save/load, 중복 체크 query, 상태 필터, evaluation lookup, CSV summary row 조회를 검증한다. public form list는 round와 season을 fetch join하여 form 응답 DTO 조립 시 lazy loading 의존을 줄인다.

## Domain

| 테스트 파일 | 검증 범위 |
|---|---|
| `RecruitingCoreDomainTest` | 시즌/차수/폼/지원서 생성, 상태 전이, invalid transition, reapplication block status |
| `RecruitingEvaluationDomainTest` | 평가 템플릿 기본 기준, custom criteria 교체, 평가 draft/submit, 중복 제출 차단, visibility rule |

## Permission

`RecruitingPermissionEvaluatorTest`는 `ResourceType.RECRUITMENT`에 대해 학교 회장단 자기 학교 허용, 다른 학교 거부, 중앙운영사무국 총괄단 허용, 교내 파트장 거부, 미구현 permission 예외를 검증한다.

## 실행 명령

```bash
./gradlew test --tests 'com.umc.product.recruiting.*'
./gradlew test --tests 'com.umc.product.recruiting.adapter.in.web.*Test'
./gradlew test --tests 'com.umc.product.recruiting.application.service.evaluator.RecruitingPermissionEvaluatorTest'
```

GraphQL adapter 테스트는 GraphQL 기반 PR이 병합된 뒤 추가한다. 현재 worktree에는 GraphQL schema/resolver 인프라가 없으므로 resolver convention을 추측하지 않는다.
