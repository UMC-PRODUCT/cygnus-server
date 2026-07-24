# Blog policy

Blog content, series, comment의 작성자·SUPER_ADMIN 권한과 관리자용 masking fact를
`blog-1.0` JSON policy로 관리한다. 공개 여부, soft-delete 상태, content/series/comment 존재,
댓글 계층과 상태 전이는 domain rule에 남는다.

## Action matrix

| Action | Author | SUPER_ADMIN |
|---|---:|---:|
| `blog-content:create` | - | ALLOW |
| `blog-content:read` | ALLOW | ALLOW |
| `blog-content:update` | ALLOW | DENY |
| `blog-content:delete` | ALLOW | ALLOW |
| `blog-series:create` | - | ALLOW |
| `blog-series:read` | ALLOW | ALLOW |
| `blog-series:update` | ALLOW | DENY |
| `blog-series:delete` | ALLOW | ALLOW |
| `blog-comment:update` | ALLOW | DENY |
| `blog-comment:delete` | ALLOW | ALLOW |
| `blog:admin-view` | - | ALLOW |

수정에 SUPER_ADMIN override가 없고 삭제에는 있는 현행 차이를 그대로 보존한다.

## Context 연결

`relation.isAuthor`는 서버가 조회한 resource의 author member ID와 공용 subject의 member ID를
비교해 계산한다. `relation.superAdmin`은 member domain의 system role에서만 온다. 두 attribute는
모든 action의 required typed fact이며 요청 JSON이 제공할 수 없다.

`blog:admin-view`는 공개 목록·상세·댓글 masking과 관리자 삭제 표시에서 기존에 중복 호출하던
SUPER_ADMIN 판단을 동일 policy decision으로 통일한다. 공개/익명 viewer는 이 action을 평가하지
않고 false를 사용한다.

## Rollout

SHADOW에서는 기존 author/SUPER_ADMIN effect가 authoritative이며 target JSON과 같은 snapshot으로
비교한다. 승인된 정책 차이는 없다. Resource 조회와 command side effect는 한 번만 실행한다.
Generated artifact와 fingerprint 검토, 24시간 관측, enforcement receipt 없이는 action을
ENFORCE로 전환할 수 없다.
