# Form Policy

이 폴더에는 서로 다른 두 계약이 공존한다.

## `form-1.0` 독립 bundle

- `bundle.json`
- `form-resource.policy.json`

Form 엔진이 최종적으로 소유할 lifecycle, response ownership, capability 정책이다.

- Form/section/question/option 관리: `relation.usageOwnerAuthorized`
- response 생성: consumer 도메인이 검증한 `relation.consumerAuthorized`
- 기명 response/answer 변경: `MEMBER` + `relation.isRespondent`
- 익명 response/answer 변경: `CAPABILITY` + `relation.capabilityBoundToResponse`

raw response access key는 policy attribute가 아니다. Form이 SHA-256 hash를 검증한 뒤
`CAPABILITY(FORM_RESPONSE_ACCESS, formResponseId)` principal을 생성해야 한다.

`FormUsageBinding(formId, usageType, ownerResourceId)`과 usage별 typed provider가 아직 없으므로
이 bundle은 startup compile만 수행하고 runtime rollout에는 연결하지 않았다.

## Project transitional module

`form.policy.json`은 `project-1.0` context와 `project-form:*` action을 사용하는 기존 Project
module이다. 물리적으로 Form 폴더가 소유하지만 Project bundle이 logical filename
`form.policy.json`으로 import한다.

독립 Form runtime 연결 순서는 다음과 같다.

1. immutable `FormUsageBinding` 도입
2. Project/Recruiting/Feedback typed usage provider 구현
3. access key 검증 후 CAPABILITY principal 생성
4. Form service ownership 검사를 `form-1.0` SHADOW rollout으로 교체
5. Project의 `project-form:*` module 제거
