import { get } from "../../lib/http.js";
import { getAccessToken } from "../../lib/auth.js";
import { pickMemberId } from "../../lib/data.js";

// soak: 낮거나 중간 부하를 오래 유지해 메모리 누수, 커넥션 누수, DB lock 누적 같은 장기 문제를 본다.
// 긴 duration 은 config/profiles.js(soak, 기본 2h)가 정한다.
export const requiresSeed = true;

export default function (seed) {
  const token = getAccessToken(pickMemberId(seed));
  get("/api/v1/projects", { token, tag: "project-read" });
}
