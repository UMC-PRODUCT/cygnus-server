// smoke: VU=1, 1분. "정상 동작하는가"만 확인한다(부하가 목적이 아님).
// load 를 돌리기 전에 토큰 발급 / 엔드포인트 경로 / 응답 형태가 맞는지 검증하는 용도.
//
// 실행(권장: run.sh — 결과 디렉토리/메타/summary 자동 생성). loadtest/README.md 참고
//   K6_MEMBER_ID=1 K6_NOTICE_ID=1 \
//     loadtest/run.sh loadtest/scenarios/notice/read-status.smoke.js notice-read-status-smoke

import http from 'k6/http';
import { sleep } from 'k6';

import { BASE_URL, NOTICE_ID } from '../../lib/config.js';
import { issueToken, authHeaders } from '../../lib/auth.js';
import { recordResponse, cursorResponseChecks } from '../../lib/checks.js';

// 종료 시 summary.md/json 자동 생성(run.sh 로 실행 시에도 결과 파일이 남도록 load 와 동일하게 export)
export { handleSummary } from '../../lib/summary.js';

export const options = {
  vus: 1,
  duration: '1m',
  thresholds: {
    endpoint_success: ['rate>0.99'],
    http_req_failed: ['rate<0.01'],
  },
};

export function setup() {
  return { token: issueToken() };
}

export default function (data) {
  const url = `${BASE_URL}/api/v1/notices/${NOTICE_ID}/read-status?filterType=ALL&status=UNREAD`;
  const res = http.get(url, { headers: authHeaders(data.token) });
  recordResponse(res, 'notice-read-status', cursorResponseChecks('notice-read-status'));
  sleep(1);
}
