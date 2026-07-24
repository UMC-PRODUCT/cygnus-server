# Storage policy

Storage의 파일 삭제 actor 권한을 `storage-1.0` typed context와 JSON policy로 관리한다.
파일 조회, 업로드 상태 검증, S3 object 삭제와 metadata 삭제 순서는 domain flow에 남고,
“누가 삭제할 수 있는가”만 policy decision으로 이동한다.

## Action과 attribute

| Action | Attribute | Type | 의미 |
|---|---|---|---|
| `storage-file:delete` | `relation.isUploader` | `BOOLEAN` | 서버가 조회한 `FileMetadata.uploadedMemberId`와 인증된 requester가 같은지 |
| `storage-file:delete` | `relation.superAdmin` | `BOOLEAN` | 공용 subject snapshot의 member system role에 `SUPER_ADMIN`이 있는지 |

`relation.isUploader`는 모든 평가에 필요한 required attribute다. 업로더이면 subject role 조회를
생략할 수 있다. 업로더가 아닌 경우에만 공용 subject snapshot을 로드하고
`relation.superAdmin`을 제공한다. Optional attribute가 누락되면 해당 `EQ` predicate는 false다.

두 ALLOW statement 중 하나 이상이 일치하면 삭제를 허용한다. `SUPER_ADMIN`은 ChallengerRole이
아닌 전역 member system role이므로 Gisu 기간과 무관하다. File ID, storage key와 requester ID는
policy decision이나 rollout log에 기록하지 않는다.

## Rollout

현재 mode는 `SHADOW`다. 같은 관계 fact로 legacy의 “업로더 또는 SUPER_ADMIN”과 target JSON을
비교하고 실제 S3/metadata 삭제는 한 번만 실행한다. 승인된 정책 차이는 없으므로
`expected-differences.json`은 비어 있다.

`generated/storage-policy-artifacts.md`의 policyVersion과 fingerprint를 검토하고
`rollout/enforcement-receipts.json`에 승인 receipt가 생기기 전에는 ENFORCE로 시작할 수 없다.
