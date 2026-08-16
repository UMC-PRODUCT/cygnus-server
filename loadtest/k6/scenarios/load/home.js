import { batchGet } from "../../lib/http.js";
import { getAccessToken } from "../../lib/auth.js";
import { pickMemberId } from "../../lib/data.js";

// 홈 화면 1회 로드 = 로그인 사용자가 3개 조회를 병렬(batch) 호출.
//   GET /api/v2/member/me    : 누적 활동일(totalActivityDays) + 참여 기수 + 상벌점(challengerHistory) — 무거운 집계
//   GET /api/v2/schedules/me : 이번 달 일정 (from~to)
//   GET /api/v1/notices      : 최근 챌린저 공지 (gisu 기준, 최신 10건)
// 부하 강도(RATE/DURATION)는 config/profiles.js 가 정한다. 서버는 초당 RATE×3 요청을 받는다.
export const requiresSeed = true;

// 이번 달 범위 (UTC). prepare-data.sh 도 이번 달에 일정을 시딩하므로 같은 달에 실행하는 한 일치한다.
// HOME_FROM/HOME_TO 로 오버라이드 (ISO-8601 Instant).
const now = new Date();
const Y = now.getUTCFullYear();
const M = now.getUTCMonth() + 1;
const pad = (n) => String(n).padStart(2, "0");
const LAST_DAY = new Date(Date.UTC(Y, M, 0)).getUTCDate();
const FROM = __ENV.HOME_FROM || `${Y}-${pad(M)}-01T00:00:00Z`;
const TO = __ENV.HOME_TO || `${Y}-${pad(M)}-${LAST_DAY}T23:59:59Z`;

export default function (seed) {
  const token = getAccessToken(pickMemberId(seed));
  const gisuId = seed.gisuId;
  batchGet(token, [
    { path: "/api/v2/member/me", tag: "home:member-me" },
    {
      path: `/api/v2/schedules/me?from=${encodeURIComponent(FROM)}&to=${encodeURIComponent(TO)}&isAttendanceRequired=false`,
      tag: "home:schedule",
    },
    // noticeTab 은 @NotNull — 홈 화면은 일반 챌린저 공지 탭을 조회한다.
    {
      path: `/api/v1/notices?gisuId=${gisuId}&noticeTab=CHALLENGER&page=0&size=10`,
      tag: "home:notice",
    },
  ]);
}
