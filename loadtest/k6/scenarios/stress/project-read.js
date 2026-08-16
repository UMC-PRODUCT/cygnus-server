import { get } from "../../lib/http.js";
import { getAccessToken } from "../../lib/auth.js";
import { pickMemberId } from "../../lib/data.js";

// stress: 정상 범위 이상으로 rate 를 올려 한계점과 부하 제거 후 복구 거동을 본다.
// 부하 강도/thresholds 완화는 config/profiles.js(stress)가 담당한다.
export const requiresSeed = true;

export default function (seed) {
  const token = getAccessToken(pickMemberId(seed));
  get("/api/v1/projects", { token, tag: "project-read" });
}
