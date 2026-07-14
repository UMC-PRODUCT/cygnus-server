# Project Policy 검토 산출물

Project target policy의 검토 기준은
`src/main/resources/policies/project/generated/project-policy-artifacts.md` 한 파일이다. 이 파일은 다음 실제 입력을
canonical UTF-8/LF 형식으로 결합한다.

- classpath에 패키징되는 `bundle.json`과 7개 policy module의 raw SHA-256
- raw policy를 `project-1.0` schema로 compile한 statement matrix
- compile 결과의 schema/context/policy version과 policy fingerprint
- runtime annotation discovery로 확인한 REST 37개, GraphQL 8개, scheduler 1개 호출면과 catalog의 action/module/gate 연결

Rollout mode와 ENFORCE 승인은 generated matrix와 분리한다. packaged 기본값은 `SHADOW`이고, 승인은 고정 classpath
`policies/project/rollout/enforcement-receipts.json`만 읽는다. 현재 shipped receipt는 비어 있으므로 ENFORCE 설정은
startup에서 fail-closed한다. wave, receipt, 관찰 및 rollback 절차는
[Project Authorization Rollout Runbook](./project-authorization-rollout-runbook.md)을 따른다.

## 갱신과 검증

정책 JSON 또는 호출면 catalog를 의도적으로 바꾼 경우에만 아래 opt-in task로 tracked artifact를 갱신한다.

```bash
./gradlew generateProjectPolicyArtifacts
```

일반 검증은 tracked 파일을 수정하지 않고 source에서 다시 생성한 byte와 비교한다. stale이면 실패하며 자동 덮어쓰지
않는다.

```bash
./gradlew verifyProjectPolicyArtifacts
```

배포 artifact 검증은 `bootJar`에서 최상위 raw policy JSON 8개를 추출한다. 각 byte가 source와 같은지 확인하고, 추출한
byte만 다시 compile한 뒤 runtime 호출면을 재발견해 생성한 산출물을 tracked 파일과 비교한다. 또한 generated markdown,
empty enforcement receipt, packaged `application.yml`의 SHADOW fallback byte/parse 계약을 함께 검증한다.

```bash
./gradlew verifyPackagedProjectPolicyArtifacts
```

`check`는 source gate와 packaged gate를 모두 실행한다. 정책 원문의 공백처럼 compiled fingerprint에 반영되지 않는
변경도 raw SHA-256 manifest가 감지한다. catalog action 변경과 tracked 파일 직접 수정도 동일하게 stale gate에서
거부된다.
