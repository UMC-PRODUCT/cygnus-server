// load: 평시 부하에서 SLO(p95 latency, error rate)를 검증한다.
// 이 스크립트가 성능 리팩토링 "전/후 비교"의 본체다.
//
// 부하 모델은 arrival-rate(개방형)이다. 목표 RPS를 고정하므로 응답 시간이 변해도
// 제공 부하가 일정하다 → 리팩토링 전/후를 "동일 부하"에서 비교할 수 있다.
// (ramping-vus + sleep 은 응답이 빨라질수록 실제 RPS가 올라가 비교가 오염된다.)
//
// 실행(권장: run.sh — 결과 디렉토리/메타/summary 자동 생성). loadtest/README.md 참고
//   K6_MEMBER_ID=1 K6_NOTICE_ID=1 K6_RATE=20 \
//     loadtest/run.sh loadtest/scenarios/notice/read-status.load.js notice-read-status-before

import http from 'k6/http';

import { BASE_URL, NOTICE_ID } from '../../lib/config.js';
import { issueToken, authHeaders } from '../../lib/auth.js';
import { recordResponse, cursorResponseChecks } from '../../lib/checks.js';

// 종료 시 summary.md/json 자동 생성(K6_OUT_DIR)
export { handleSummary } from '../../lib/summary.js';

// 목표 RPS. read-status 는 운영진 조회 엔드포인트(ADR-013 Tier 3, 평시 트래픽 작음)라
// 기본 20 RPS 를 평시 가정으로 둔다. 실제 capacity 측정 시 K6_RATE 로 조정한다.
const RATE = Number(__ENV.K6_RATE || 20);
const HOLD = __ENV.K6_DURATION || '2m';

export const options = {
  scenarios: {
    read_status_load: {
      executor: 'ramping-arrival-rate',
      startRate: 5,
      timeUnit: '1s',
      preAllocatedVUs: Number(__ENV.K6_PREALLOC_VUS || 50),
      maxVUs: Number(__ENV.K6_MAX_VUS || 200),
      stages: [
        { target: 5, duration: '20s' }, // 워밍업(JVM JIT 안정화)
        { target: RATE, duration: '20s' }, // 목표 RPS 램프업
        { target: RATE, duration: HOLD }, // 평시 부하 유지(측정 구간)
        { target: 0, duration: '10s' }, // 종료
      ],
    },
  },
  // 전/후 비교 시 실행을 구분하기 위한 태그. gitref 등은 CLI --tag 로 추가한다.
  tags: {
    endpoint: 'notice-read-status',
    testid: __ENV.K6_TESTID || 'local-adhoc',
  },
  // summary 에 p99 를 포함시키기 위해 명시(기본값엔 p90/p95 만 있음)
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  thresholds: {
    // ADR-013 기준(p95<500ms). baseline 이 이를 넘으면 곧 개선 대상이라는 신호.
    endpoint_duration: ['p(95)<500', 'p(99)<1000'],
    endpoint_success: ['rate>0.99'],
    http_req_failed: ['rate<0.01'],
    // 부하 생성기가 목표 RPS 를 못 따라가 요청을 흘리면 측정이 왜곡됨 → 감시
    dropped_iterations: ['count<1'],
  },
};

export function setup() {
  return { token: issueToken() };
}

export default function (data) {
  const url = `${BASE_URL}/api/v1/notices/${NOTICE_ID}/read-status?filterType=ALL&status=UNREAD`;
  const res = http.get(url, { headers: authHeaders(data.token) });
  recordResponse(res, 'notice-read-status', cursorResponseChecks('notice-read-status'));
  // arrival-rate 모델은 도착률로 페이싱하므로 sleep 을 두지 않는다(VU 를 빨리 반환).
}
