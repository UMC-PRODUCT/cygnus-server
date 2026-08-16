import { get } from "../../lib/http.js";
import { getAccessToken } from "../../lib/auth.js";
import { pickMemberId } from "../../lib/data.js";

// 최소 부하로 인증 + 프로젝트 목록 조회 경로를 검증한다. seed.json 이 필요하다.
export const requiresSeed = true;

export default function (seed) {
  const token = getAccessToken(pickMemberId(seed));
  get("/api/v1/projects", { token, tag: "project-read" });
}
