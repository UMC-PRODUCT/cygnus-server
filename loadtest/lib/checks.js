// 공통 check / 커스텀 메트릭. 모든 시나리오가 동일한 지표 이름으로 기록해야
// 전/후 비교 시 같은 축으로 나란히 볼 수 있다.

import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

// 엔드포인트 응답 시간(ms). true = k6 summary 에 avg/min/max/p(90)/p(95) 표시
export const endpointDuration = new Trend('endpoint_duration', true);

// 성공률(check 통과 비율)
export const endpointSuccess = new Rate('endpoint_success');

// 비JSON 응답이어도 예외 없이 JSON 경로 값을 꺼낸다(없으면 undefined).
export function jsonPath(res, path) {
  try {
    return res.json(path);
  } catch (e) {
    return undefined;
  }
}

// 응답을 검증하고 커스텀 메트릭에 기록한다. 성공 여부(boolean) 반환.
// 기본 검증: HTTP 200 + 응답 래퍼 success=true.
// extraChecks 로 엔드포인트별 body 구조 검증을 추가한다.
export function recordResponse(res, name, extraChecks = {}) {
  const checks = {
    [`${name} status 200`]: (r) => r.status === 200,
    [`${name} success=true`]: (r) => jsonPath(r, 'success') === true,
    ...extraChecks,
  };
  const ok = check(res, checks);
  endpointDuration.add(res.timings.duration);
  endpointSuccess.add(ok);
  return ok;
}

// 커서 페이지네이션 응답(CursorResponse: content/nextCursor/hasNext) 구조 검증.
// recordResponse 의 extraChecks 로 넘겨 사용한다.
export function cursorResponseChecks(name) {
  return {
    [`${name} result.content is array`]: (r) => Array.isArray(jsonPath(r, 'result.content')),
    [`${name} result.hasNext is boolean`]: (r) => typeof jsonPath(r, 'result.hasNext') === 'boolean',
    [`${name} result.nextCursor is number or null`]: (r) => {
      const cursor = jsonPath(r, 'result.nextCursor');
      return cursor === null || typeof cursor === 'number';
    },
  };
}
