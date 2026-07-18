# Form 테스트 (기존 문서 파일명: `survey.md`)

실제 테스트 package는 `com.umc.product.form`이다. `src/test/java/com/umc/product/survey` 경로는 현재 구현에 없으며, 이 문서의 링크와 실행 명령은 모두 Form 경로를 가리킨다. 도메인 규칙은 [Form 도메인](../domain/survey.md), ownership migration/enable 운영은 [runbook](../database-backfill-with-replicas.md)을 함께 읽는다.

## 실행 명령

```bash
./gradlew test --tests 'com.umc.product.form.domain.FormOwnerReferenceTest' \
  --tests 'com.umc.product.form.adapter.out.persistence.FormOwnershipPersistenceAdapterTest' \
  --tests 'com.umc.product.form.application.service.FormOwnershipAccessServiceTest' \
  --tests 'com.umc.product.form.application.service.StandaloneFormOwnerPolicyTest' \
  --tests 'com.umc.product.form.application.service.FormPortInOwnershipContractTest' \
  --tests 'com.umc.product.form.application.service.command.FormAnswerUsageTransactionIntegrationTest' \
  --tests 'com.umc.product.form.application.service.command.FormAnswerUsageCascadeIntegrationTest'
```

Project/Notice/Feedback consumer policy와 Flyway ownership 이관은 다음 targeted test로 확인한다.

```bash
./gradlew test --tests 'com.umc.product.project.application.form.ProjectApplicationFormOwnerReferenceFactoryTest' \
  --tests 'com.umc.product.project.application.form.ProjectApplicationFormOwnerPolicyTest' \
  --tests 'com.umc.product.project.application.form.ProjectApplicationFormOwnershipContractTest' \
  --tests 'com.umc.product.registry.RegistryFlywayDataMigrationIntegrationTest'
```

## Ownership schema와 정책

| 테스트 | 검증하는 observable |
| --- | --- |
| [`FormOwnerReferenceTest`](../../../src/test/java/com/umc/product/form/domain/FormOwnerReferenceTest.java) | namespace/resource key/slot grammar, immutable tuple, 동일 Form owner transfer 거부를 확인한다. |
| [`FormOwnershipPersistenceAdapterTest`](../../../src/test/java/com/umc/product/form/adapter/out/persistence/FormOwnershipPersistenceAdapterTest.java) | binding 저장/lock 조회, 동일 binding idempotency, 동일 Form transfer 거부, 동일 owner tuple 중복 거부, form 삭제 시 cascade, DB CHECK를 확인한다. |
| [`FormOwnershipAccessServiceTest`](../../../src/test/java/com/umc/product/form/application/service/FormOwnershipAccessServiceTest.java) | mutation은 binding lock 후 expected owner와 policy를 순서대로 검사하고, query는 non-lock 조회를 사용한다. missing/mismatch/미등록·중복 evaluator는 policy 전에 fail-closed한다. |
| [`StandaloneFormOwnerPolicyTest`](../../../src/test/java/com/umc/product/form/application/service/StandaloneFormOwnerPolicyTest.java) | `form.standalone/{formId}/default`에서 creator의 관리·게시·삭제·응답 결과 조회, published 공개 READ/RESPOND, draft anonymous 거부를 확인한다. |
| [`FormPortInOwnershipContractTest`](../../../src/test/java/com/umc/product/form/application/service/FormPortInOwnershipContractTest.java) | 모든 public operation이 expected owner와 actor context를 명시적으로 요구하는지 확인한다. |

## Consumer mapping과 exact-one coverage

| 테스트 | 검증하는 observable |
| --- | --- |
| [`ProjectApplicationFormOwnerReferenceFactoryTest`](../../../src/test/java/com/umc/product/project/application/form/ProjectApplicationFormOwnerReferenceFactoryTest.java) | 서버가 `project.application-form/{projectId}/default`를 생성하고 요청 namespace를 신뢰하지 않는다. |
| [`ProjectApplicationFormOwnerPolicyTest`](../../../src/test/java/com/umc/product/project/application/form/ProjectApplicationFormOwnerPolicyTest.java) | Project permission capability와 Form operation의 일대일 매핑, malformed key/unauthenticated actor fail-closed를 확인한다. |
| [`ProjectApplicationFormOwnershipContractTest`](../../../src/test/java/com/umc/product/project/application/form/ProjectApplicationFormOwnershipContractTest.java) | 다른 Project owner key 또는 child에서 resolve한 Form ID mismatch가 policy 전에 거부된다. |
| [`FeedbackTemplateOwnerPolicyTest`](../../../src/test/java/com/umc/product/feedback/application/policy/FeedbackTemplateOwnerPolicyTest.java) | `feedback.template` owner lookup, actor/business permission, missing ownership와 policy 예외의 false 수렴을 확인한다. |
| [`RegistryFlywayDataMigrationIntegrationTest`](../../../src/test/java/com/umc/product/registry/RegistryFlywayDataMigrationIntegrationTest.java) | Project/Notice/Feedback와 standalone Form mapping을 실제 Flyway SQL로 이관하고 Storage/Chat migration 순서까지 검증한다. |

Form readiness는 persisted namespace와 declared namespace의 합집합에 대해 evaluator가 정확히 하나인지 확인한다. `form.standalone`, `project.application-form`, `notice.vote`, `feedback.template` 중 하나라도 누락/중복/오염되면 `app.engine-ownership.enforcement-enabled=true`여도 STRICT가 되지 않는다.

## 응답·첨부 경계

| 테스트 | 검증하는 observable |
| --- | --- |
| [`FormResponseCommandServiceTest`](../../../src/test/java/com/umc/product/form/application/service/command/FormResponseCommandServiceTest.java) | draft/submit 중복 정책, parent chain, required question scope, answer format, named respondent ownership, anonymous raw key hash 경계를 확인한다. |
| [`FormResponseAnonymousAttachmentUsageTest`](../../../src/test/java/com/umc/product/form/application/service/command/FormResponseAnonymousAttachmentUsageTest.java) | 익명 attachment null은 snapshot 유지, empty는 clear, subset은 부분 제거만 허용한다. |
| [`FormAnswerAttachmentUsageServiceTest`](../../../src/test/java/com/umc/product/form/application/service/FormAnswerAttachmentUsageServiceTest.java) | FILE Answer ID exact snapshot 등록과 response 삭제 전 batch detach를 확인한다. |
| [`FormAnswerUsageCascadeIntegrationTest`](../../../src/test/java/com/umc/product/form/application/service/command/FormAnswerUsageCascadeIntegrationTest.java) | 공유 file은 첫 response 삭제에서 유지하고 마지막 Form 삭제에서 detach한다. |
| [`FormAnswerUsageTransactionIntegrationTest`](../../../src/test/java/com/umc/product/form/application/service/command/FormAnswerUsageTransactionIntegrationTest.java) | usage 실패 시 Answer 저장을 rollback한다. |
| [`FormChildOptionalQueryOwnershipTest`](../../../src/test/java/com/umc/product/form/application/service/query/FormChildOptionalQueryOwnershipTest.java) | missing/existing section·question·option도 실제 root parent chain ownership을 먼저 검증하고 foreign child는 fail-closed한다. |
| [`FormCollectionQueryOwnershipTest`](../../../src/test/java/com/umc/product/form/application/service/query/FormCollectionQueryOwnershipTest.java) | collection/batch adapter가 expected owner scope 밖 root·child·response·answer를 반환하면 거부한다. |

새 API/FE 필드나 raw engine ID를 추가하지 않는다. anonymous 응답 계약과 attachment legacy snapshot 보존 규칙은 기존 테스트가 회귀 보호한다.
