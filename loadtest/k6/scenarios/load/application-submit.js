import { get } from "../../lib/http.js";
import { getAccessToken } from "../../lib/auth.js";
import { pickMemberId, pickProjectId } from "../../lib/data.js";

// load: 지원서 제출 경로의 쓰기 부하를 본다.
//
// ⚠️ 스켈레톤이다. 제출 플로우(createDraft → fill → submit)는 폼 스키마 의존이 커서
//    이 시나리오를 실제로 쓰는 사람이 정확한 엔드포인트/요청 body 를 채워 별도 PR 로 완성한다.
//    (참고: SeedController SEED-006, ProjectApplicationController)
//    v1 에서는 로그인 + 대상 프로젝트 조회까지만 안전하게 수행한다.
export const requiresSeed = true;

export default function (seed) {
  const memberId = pickMemberId(seed);
  const token = getAccessToken(memberId);
  const projectId = pickProjectId(seed);

  // 대상 프로젝트를 읽어 지원 전 상태를 확인한다. (읽기 전용 — 여기까지는 안전하게 반복 가능)
  if (projectId) {
    get(`/api/v1/projects/${projectId}`, { token, tag: "application-submit:read" });
  }

  // TODO: 아래 쓰기 체인을 실제 API 계약에 맞춰 채운다.
  //   1) createDraft: POST /api/v1/projects/{projectId}/applications (draft 생성)
  //   2) fill:        폼 응답(form response) 작성
  //   3) submit:      제출 상태 전환
  // 쓰기는 반복 실행 시 데이터가 누적되므로, 매 run 전 prepare-data.sh 로 상태를 초기화하는 전제로 설계한다.
}
