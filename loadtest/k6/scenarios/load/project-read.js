import { get } from "../../lib/http.js";
import { getAccessToken } from "../../lib/auth.js";
import { pickMemberId } from "../../lib/data.js";

// load: 예상 정상/피크 트래픽에서 프로젝트 조회의 목표 응답시간/TPS/에러율을 확인한다.
// 시나리오 로직은 tier 별로 동일하고, 부하 패턴 차이는 config/profiles.js 가 정한다.
// (tier 별 파일을 분리해 둔 이유: 나중에 읽기 대상/혼합 비율을 tier 마다 다르게 튜닝하기 위함.)
export const requiresSeed = true;

export default function (seed) {
  const token = getAccessToken(pickMemberId(seed));
  get("/api/v1/projects", { token, tag: "project-read" });
}
