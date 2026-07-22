import http from "k6/http";

import { BASE_URL } from "./http.js";

// VU 단위 토큰 캐시. 매 iteration 마다 토큰을 새로 발급하면 /test/token/access 자체가 병목이 되므로 재사용한다.
const tokenCache = {};

// 부하 테스트 전용 access token 발급(TEST-007, @Public). 운영 로그인 경로가 아니라 시딩된 memberId 로 바로 토큰을 받는다.
// "로그인 성능"을 재려면 이 helper 대신 실제 로그인 API 를 호출하는 별도 시나리오를 쓴다.
export function getAccessToken(memberId) {
  if (tokenCache[memberId]) {
    return tokenCache[memberId];
  }
  const res = http.get(`${BASE_URL}/test/token/access?memberId=${memberId}`);
  // 컨트롤러가 raw 문자열 토큰을 반환한다. 따옴표가 감싸져 있으면 제거한다.
  const token = (res.body || "").trim().replace(/^"|"$/g, "");
  if (res.status !== 200 || !token) {
    throw new Error(`토큰 발급 실패 memberId=${memberId} status=${res.status}`);
  }
  tokenCache[memberId] = token;
  return token;
}
