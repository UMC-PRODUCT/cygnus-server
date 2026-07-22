import { get } from "../../lib/http.js";

// 최소 부하로 환경/스크립트/기본 응답을 검증한다. 시딩·인증 없이 도는 유일한 시나리오.
export const requiresSeed = false;

export default function () {
  // ALB 를 통해 앱(8080)에 도달하는지 확인. /test/health-check(TEST-011, @Public)는 "OK" 를 반환한다.
  get("/test/health-check", { tag: "health-check" });
}
