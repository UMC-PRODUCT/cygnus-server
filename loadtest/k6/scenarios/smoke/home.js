import { batchGet } from "../../lib/http.js";
import { getAccessToken } from "../../lib/auth.js";
import { pickMemberId } from "../../lib/data.js";

// 홈 화면 1회 로드 = 로그인 사용자가 3개 조회를 병렬(batch) 호출.
//   GET /api/v2/member/me    : 누적 활동일(totalActivityDays) + 참여 기수 + 상벌점(challengerHistory) — 무거운 집계
//   GET /api/v2/schedules/me : 7월 일정 (from~to)
//   GET /api/v1/notices      : 최근 공지 (gisu 기준, 최신 10건)
export const requiresSeed = true;

// 7월 범위. HOME_YEAR/HOME_FROM/HOME_TO 로 오버라이드 (ISO-8601 Instant).
const YEAR = __ENV.HOME_YEAR || String(new Date().getUTCFullYear());
const FROM = __ENV.HOME_FROM || `${YEAR}-07-01T00:00:00Z`;
const TO = __ENV.HOME_TO || `${YEAR}-07-31T23:59:59Z`;

export default function (seed) {
  const token = getAccessToken(pickMemberId(seed));
  const gisuId = seed.gisuId;
  batchGet(token, [
    { path: "/api/v2/member/me", tag: "home:member-me" },
    {
      path: `/api/v2/schedules/me?from=${encodeURIComponent(FROM)}&to=${encodeURIComponent(TO)}&isAttendanceRequired=false`,
      tag: "home:schedule",
    },
    { path: `/api/v1/notices?gisuId=${gisuId}&page=0&size=10`, tag: "home:notice" },
  ]);
}
