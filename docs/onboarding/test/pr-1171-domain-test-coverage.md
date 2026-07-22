# PR #1171 전체 Production 테스트 검증 목록

> 대상 PR: `feature/#1167-domain-test-coverage` → `develop`  
> 비교 기준: 최신 `origin/develop...HEAD`  
> 대상: `src/main/java/com/umc/product` 아래 직접 작성한 모든 production class  
> 합격 기준: JaCoCo Line 100% / Class 100%, 실패·오류 0건

## 1. 최종 결과

`./gradlew clean test jacocoTestCoverageVerification`을 실행한 최종 결과다.

- 전체 테스트: 4,430건
- 성공: 4,390건
- ignored: 40건
- 실패: 0건
- 오류: 0건
- clean build 실행시간: 7분 18초
- Line: 34,923 / 34,923 (100%)
- Class: 2,306 / 2,306 (100%)
- Branch: 9,228 / 10,295 (89.64%, 비강제 지표)
- 새 `@Disabled` 또는 ignored 테스트: 추가하지 않음

QueryDSL 제외 대상은 `build/generated/querydsl`에서 실제 생성된 source와 일치하는 class뿐이다. DTO,
configuration, exception, application root, 공통 package와 production seed package는 제외하지 않았다.

## 2. Production package별 coverage

| Production package | 테스트 건수 | ignored | Line | Class | Branch |
|---|---:|---:|---:|---:|---:|
| application root | - | - | 3/3 | 1/1 | - |
| `analytics` | 46 | 0 | 1,288/1,288 | 96/96 | 89.61% |
| `audit` | 22 | 0 | 168/168 | 13/13 | 94.74% |
| `authentication` | 309 | 0 | 1,679/1,679 | 128/128 | 84.95% |
| `authorization` | 137 | 0 | 840/840 | 44/44 | 97.67% |
| `blog` | 94 | 0 | 1,779/1,779 | 87/87 | 85.24% |
| `certificate` | 99 | 0 | 767/767 | 40/40 | 95.67% |
| `challenger` | 185 | 0 | 1,320/1,320 | 77/77 | 89.58% |
| `chat` | 109 | 0 | 344/344 | 32/32 | 92.65% |
| `common` | 13 | 0 | 78/78 | 10/10 | 97.37% |
| `community` | 120 | 0 | 994/994 | 61/61 | 90.60% |
| `curriculum` | 132 | 0 | 870/870 | 132/132 | 97.75% |
| `documentation` | 5 | 0 | 22/22 | 7/7 | 75.00% |
| `feedback` | 20 | 0 | 120/120 | 19/19 | 100.00% |
| `form` | 222 | 0 | 1,703/1,703 | 97/97 | 93.91% |
| `global` | 365 | 0 | 2,344/2,344 | 130/130 | 92.54% |
| `llm` | 32 | 0 | 337/337 | 18/18 | 82.81% |
| `maintenance` | 62 | 0 | 308/308 | 22/22 | 91.25% |
| `member` | 238 | 29 | 1,447/1,447 | 89/89 | 90.45% |
| `notice` | 88 | 0 | 1,245/1,245 | 96/96 | 88.32% |
| `notification` | 104 | 0 | 986/986 | 71/71 | 84.50% |
| `organization` | 379 | 11 | 3,083/3,083 | 241/241 | 86.47% |
| `project` | 765 | 0 | 5,310/5,310 | 272/272 | 92.74% |
| `recruiting` | 491 | 0 | 4,676/4,676 | 314/314 | 83.59% |
| `schedule` | 102 | 0 | 1,107/1,107 | 56/56 | 93.22% |
| `storage` | 88 | 0 | 538/538 | 27/27 | 90.23% |
| `term` | 32 | 0 | 234/234 | 28/28 | 96.15% |
| production `test` seed | 165 | 0 | 1,333/1,333 | 98/98 | 95.24% |
| **전체** | **4,430** | **40** | **34,923/34,923** | **2,306/2,306** | **89.64%** |

package별 테스트 건수는 test classname의 최상위 package를 기준으로 집계했다. application root 테스트는
전체 건수에는 포함되지만 위 표의 도메인별 건수에는 별도 배분하지 않았다.

## 3. 어떤 상황을 검증했는가

### 공통 계층별 검증

| 계층 | 검증 상황 |
|---|---|
| Domain | 필수값과 null/blank, 길이·숫자·날짜 경계, 기간 중첩, 중복, 잘못된 상태 전이, 멱등성, aggregate 불변식 |
| Application service | 성공·not-found, 빈 batch 단축, 중복 제거, 입력 순서, 일부 참조 누락, port 실패, event/cache 후처리 |
| Authorization | 비인증, 미존재 회원, 다른 기수·학교 scope, 미지원 permission fail-closed, `SUPER_ADMIN` 전역 호환 |
| Persistence | Optional/get 계약, 빈 `IN`, filter 조합, cursor/page 경계, 정렬, lock timeout/deadlock, DB 제약 |
| REST/GraphQL | Bean Validation, command/query 변환, optional body 기본값, null nested data, 민감 정보 마스킹, legacy 호환 |
| External/Scheduler | disabled/no-op, 통신·파싱 실패, 잘못된 응답, 부분 실패, metric, 오류 정규화와 예외 재전파 |
| Production seed | production 환경 차단, 빈·중복 seed, 결정적 결과, 연관 데이터 생성·삭제 순서, 부분 단계 실패 |

### 도메인 묶음별 핵심 검증

| 묶음 | 대상 | 핵심 검증 |
|---|---|---|
| 기반·공통 | application root, `common`, `global`, `audit`, `documentation`, `maintenance`, `storage`, `llm`, `term` | boot entry, enum 변환, security/filter/resolver, 예외·응답 wrapping, observability sanitizing, cache/event/outbox, S3·LLM 외부 실패, maintenance no-op |
| 콘텐츠·운영 | `blog`, `certificate`, `chat`, `community`, `feedback`, `notice` | content validation, PDF/template 실패, 채팅 접근·첨부 정책, 게시물/댓글 권한과 정렬, vote·조회·민감정보 조립 |
| 분석·알림·일정 | `analytics`, `notification`, `schedule` | scope 조합과 빈 집계, FCM/SES/webhook 성공·부분 실패, outbox/scheduler 멱등성, 일정 기간·참여자·권한·cursor |
| 교육·폼 | `curriculum`, `form` | workbook/mission lifecycle, 기간 경계, Form 구조 변경, 조건부 이동, 질문/선택지, anonymous/member 응답 소유권 |
| 프로젝트 | `project` | 지원서·멤버·매칭 라운드 상태 전이, quota, 권한 scope, 통계 집계, GraphQL null nested data와 REST 호환 |
| 모집 | `recruiting` | 시즌/차수/지원서/Form/평가/면접/등록 전체 lifecycle, 발표 시각 경계, 익명 credential, 동시성 lock, 공개 결과 마스킹 |
| 핵심 회원·조직 | `member`, `challenger`, `authentication`, `authorization`, `organization` | OAuth/OIDC 실패, 역할 snapshot/cache, 기수·학교 권한, batch 누락·순서, 조직 기간 중첩, 공개 프로필 마스킹 |
| Production seed | `test` | 회원·챌린저·역할·커리큘럼·공지·프로젝트 seed의 환경 guard, empty/no-op, 결정적 random seam, 단계별 실패 |

## 4. Recruiting 추가 검증 상세

최신 `origin/develop`에서 추가된 `recruiting`은 다음 회귀 위험을 별도로 고정했다.

- 시즌/차수: 정규·추가 모집 번호, 제목 중복, 등록일·모집일 정렬, 공개 phase 경계, OPEN/CLOSED 전이.
- Form 구조: section/question/option create·update·delete·reorder, 중복/foreign ID, 누락 정책,
  TRACK→COMMON 허용과 다른 TRACK·미존재 section 이동 거부.
- 지원서: 회원·익명 생성/수정/제출/철회, DRAFT/SUBMITTED 분기, invalid key, email 정규화,
  선택 트랙 밖 답변, Form 응답 소유권, 재지원·다른 학교 중복.
- 평가/면접: 서류·최종 PASS/FAIL/null 결정, 면접 배정·생략, 질문 불변성, 일정 확정·취소·재시도,
  메일 발송 멱등성 및 메시지 없는 실패 fallback.
- 등록: 중앙 운영진과 `SUPER_ADMIN`, quota lock과 소진, 익명 지원서·합격 트랙 누락 거부,
  NOT_READY→READY→REGISTERED 전이.
- Persistence/동시성: 복수 학교·차수·상태 filter, pageable 경계, 빈 `IN` 단축,
  PostgreSQL/JPA lock timeout·deadlock 정규화, applicant lock 필수 scope.
- REST/GraphQL: optional input 기본값, page/ID validation, 익명 request 중첩 답변 변환,
  공개 결과 발표 전후 마스킹, 권한 없는 면접 일정 fail-closed.

## 5. Fixture와 테스트 구조

- `src/test/java/com/umc/product/support/fixture`의 valid-default factory를 우선 사용한다.
- `AuthenticationFixture`, `AuthorizationFixture`, `ChallengerUnitFixture`, `MemberUnitFixture`,
  `OrganizationUnitFixture`를 유지하고 `BlogUnitFixture`, `FormFixture`, `NoticeUnitFixture`,
  `ScheduleUnitFixture`를 추가했다.
- 각 테스트는 유효한 기본 fixture에서 검증 대상 값만 바꾼다.
- 저장이 필요한 fixture는 SavePort 또는 기존 persistence test support를 사용한다.
- 순수 로직은 Mockito 단위 테스트로 검증하고 실제 SQL·constraint·security wiring만 기존 통합 테스트
  기반을 재사용한다.
- 새 sleep, 외부 네트워크 호출, 테스트별 container 생성, 새 disabled 테스트를 추가하지 않았다.

## 6. Coverage gate와 CI

- `gradle/testing.gradle.kts`의 `jacocoTestCoverageVerification`이 bundle 기준 `LINE=1.0`,
  `CLASS=1.0`을 강제한다.
- `jacocoTestReport`와 verification은 같은 `jacocoProductionClassDirectories`를 사용한다.
- `check`는 coverage verification에 의존한다.
- CI와 Codecov workflow는 `./gradlew test jacocoTestCoverageVerification`을 실행한다.
- `--tests`를 사용하는 선택 실행은 전역 gate를 직접 실행하지 않으므로 개발 중 package 단위 검증을
  방해하지 않는다.

## 7. 최종 검증 명령

```bash
./gradlew spotlessCheck checkstyleMain checkstyleTest
./gradlew compileJava compileTestJava
./gradlew clean test jacocoTestCoverageVerification
```

최종 JaCoCo HTML은 `build/reports/jacoco/test/html/index.html`, XML은
`build/reports/jacoco/test/jacocoTestReport.xml`에서 확인할 수 있다.

## 8. 해석 시 주의사항

- Line/Class 100%는 모든 직접 작성 production class와 line이 full suite에서 실행됐다는 의미다.
- Branch는 컴파일러 생성 분기와 방어 조건 조합을 포함하므로 100%를 합격 조건으로 강제하지 않는다.
  다만 발생 가능한 비즈니스·보안·동시성·외부 연동 edge case는 명시적인 테스트로 우선 고정했다.
- ignored 40건은 기존 테스트이며 이 PR에서 새 ignored/disabled 테스트를 추가하지 않았다.
- production API, GraphQL schema, DB migration 계약은 변경하지 않았다. 테스트가 발견한 실제 결함은
  client contract를 유지하는 최소 production 수정과 회귀 테스트로 함께 남겼다.
