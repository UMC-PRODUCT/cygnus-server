import http from "k6/http";
import { check } from "k6";

import { errorRate } from "./metrics.js";

// BASE_URL 환경변수로 부하를 보낼 앱 주소를 지정한다.
const BASE_URL = (__ENV.BASE_URL || "http://localhost:8080").replace(/\/+$/, "");

function headers(token) {
  const h = { "Content-Type": "application/json" };
  if (token) {
    h["Authorization"] = `Bearer ${token}`;
  }
  return h;
}

// 공통 GET: 상태코드 check + 실패를 scenario 태그로 errorRate 에 적재한다.
export function get(path, { token, tag, expectedStatus = 200 } = {}) {
  const label = tag || path;
  const res = http.get(`${BASE_URL}${path}`, {
    headers: headers(token),
    tags: { scenario: label },
  });
  const ok = check(res, {
    [`${label} status ${expectedStatus}`]: (r) => r.status === expectedStatus,
  });
  errorRate.add(!ok, { scenario: label });
  return res;
}

// 공통 POST: body 를 JSON 직렬화해 보낸다.
export function post(path, body, { token, tag, expectedStatus = 200 } = {}) {
  const label = tag || path;
  const res = http.post(`${BASE_URL}${path}`, JSON.stringify(body), {
    headers: headers(token),
    tags: { scenario: label },
  });
  const ok = check(res, {
    [`${label} status ${expectedStatus}`]: (r) => r.status === expectedStatus,
  });
  errorRate.add(!ok, { scenario: label });
  return res;
}

// 여러 GET을 홈 화면처럼 동시에(병렬) 발사한다. 각 응답을 개별 태깅·체크한다.
// reqs: [{ path, tag, expectedStatus }]
export function batchGet(token, reqs) {
  const requests = reqs.map((r) => ({
    method: "GET",
    url: `${BASE_URL}${r.path}`,
    params: { headers: headers(token), tags: { scenario: r.tag } },
  }));
  const responses = http.batch(requests);
  responses.forEach((res, i) => {
    const label = reqs[i].tag;
    const expected = reqs[i].expectedStatus || 200;
    const ok = check(res, {
      [`${label} status ${expected}`]: (r) => r.status === expected,
    });
    errorRate.add(!ok, { scenario: label });
  });
  return responses;
}

export { BASE_URL };
