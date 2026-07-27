// 인증 helper. k6 의 setup() 단계에서 1회 호출해 토큰을 발급하고,
// 각 VU 는 default() 에서 authHeaders() 로 그 토큰을 재사용한다.
//
// 하드코딩 토큰(만료 시 수동 교체)을 없애기 위한 하네스의 핵심 조각.
// 우선순위: RAW_TOKEN > MEMBER_ID(test 토큰 API) > EMAIL/PASSWORD(로그인 API)

import http from 'k6/http';
import { check } from 'k6';

import { BASE_URL, MEMBER_ID, EMAIL, PASSWORD, RAW_TOKEN, TOKEN_TTL_MIN } from './config.js';

// 오류 메시지에 raw 응답 본문을 그대로 노출하지 않는다(민감정보 방어).
// 상태 코드와, JSON 래퍼라면 비민감 필드(code/message)만 요약한다.
function describeRes(res) {
  let detail = '';
  try {
    const code = res.json('code');
    const message = res.json('message');
    if (code != null || message != null) {
      detail = ` code=${code} message=${message}`;
    }
  } catch (e) {
    // 비JSON 본문(예: raw 토큰 문자열) → 상태 코드만 노출
  }
  return `status=${res.status}${detail}`;
}

// AccessToken 문자열을 반환한다. setup() 안에서만 호출할 것.
export function issueToken() {
  // 방식 C: 직접 주입된 토큰이 있으면 그대로 사용
  if (RAW_TOKEN) {
    return RAW_TOKEN;
  }
  // 방식 A(권장, local 전용): test 도메인 토큰 발급 API
  if (MEMBER_ID) {
    return issueTokenByMemberId();
  }
  // 방식 B: 이메일 로그인 API
  if (EMAIL && PASSWORD) {
    return issueTokenByLogin();
  }

  throw new Error(
    'K6_TOKEN / K6_MEMBER_ID / (K6_EMAIL + K6_PASSWORD) 중 하나가 필요합니다. loadtest/README.md 참고.'
  );
}

// GET /test/token/access?memberId=... ( @Profile("local | dev") 전용 )
// 이 엔드포인트는 응답 래퍼(ApiResponse) 없이 raw 토큰 문자열을 반환한다.
function issueTokenByMemberId() {
  // TOKEN_TTL_MIN 을 주면 만료를 늘려 장기 실행(soak) 중 토큰 만료를 방지한다.
  let url = `${BASE_URL}/test/token/access?memberId=${MEMBER_ID}`;
  if (TOKEN_TTL_MIN) {
    url += `&expirationInMinutes=${TOKEN_TTL_MIN}`;
  }
  const res = http.get(url);

  const ok = check(res, { 'test token 200': (r) => r.status === 200 });
  if (!ok) {
    throw new Error(`토큰 발급 실패(memberId=${MEMBER_ID}): ${describeRes(res)}`);
  }

  // raw 문자열. 혹시 따옴표로 감싸여 오는 경우까지 방어적으로 벗겨낸다.
  const token = (res.body || '').trim().replace(/^"|"$/g, '');
  if (!token) {
    throw new Error(`발급된 토큰이 비어 있습니다: ${describeRes(res)}`);
  }
  return token;
}

// POST /api/v1/auth/login/email — 응답 래퍼: { success, code, message, result: { accessToken, ... } }
function issueTokenByLogin() {
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login/email`,
    JSON.stringify({ email: EMAIL, password: PASSWORD, clientType: 'WEB' }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const ok = check(res, { 'login 200': (r) => r.status === 200 });
  if (!ok) {
    throw new Error(`로그인 실패: ${describeRes(res)}`);
  }

  const token = res.json('result.accessToken');
  if (!token) {
    throw new Error(`accessToken 파싱 실패: ${describeRes(res)}`);
  }
  return token;
}

// default() 에서 요청 헤더를 만들 때 사용
export function authHeaders(token) {
  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  };
}
