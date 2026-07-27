// k6 종료 시(handleSummary) 결과를 요약 파일로 자동 생성한다.
// 외부 도구(jq 등) 없이 summary.json + summary.md 를 남겨 전/후 비교를 쉽게 한다.
// 출력 위치: K6_OUT_DIR (기본 docs/loadtest/runs/local-adhoc). run.sh 가 날짜/라벨 디렉토리를 지정한다.

function metricValue(data, metric, key) {
  const m = data.metrics[metric];
  if (!m || m.values == null) {
    return undefined;
  }
  return m.values[key];
}

function fmt(x, digits = 2) {
  return x == null || Number.isNaN(x) ? 'n/a' : Number(x).toFixed(digits);
}

function pct(x) {
  return x == null || Number.isNaN(x) ? 'n/a' : (Number(x) * 100).toFixed(2) + '%';
}

function metricsTable(data) {
  const rows = [
    ['endpoint_duration p95 (ms)', fmt(metricValue(data, 'endpoint_duration', 'p(95)'))],
    ['endpoint_duration p99 (ms)', fmt(metricValue(data, 'endpoint_duration', 'p(99)'))],
    ['endpoint_duration avg (ms)', fmt(metricValue(data, 'endpoint_duration', 'avg'))],
    ['성공률 endpoint_success', pct(metricValue(data, 'endpoint_success', 'rate'))],
    ['HTTP 실패율 http_req_failed', pct(metricValue(data, 'http_req_failed', 'rate'))],
    ['총 요청 http_reqs', fmt(metricValue(data, 'http_reqs', 'count'), 0)],
    ['실측 RPS http_reqs/s', fmt(metricValue(data, 'http_reqs', 'rate'))],
    ['dropped_iterations', fmt(metricValue(data, 'dropped_iterations', 'count'), 0)],
    ['checks 통과율', pct(metricValue(data, 'checks', 'rate'))],
  ];
  return rows.map(([k, v]) => `| ${k} | ${v} |`).join('\n');
}

function renderMarkdown(data) {
  return `# 부하 테스트 결과: ${__ENV.K6_TESTID || 'local-adhoc'}

## 환경
- 일시: ${new Date().toISOString()}
- Git ref: ${__ENV.K6_GITREF || 'unknown'}
- 대상: ${__ENV.K6_BASE_URL || 'http://localhost:8080'}
- 공지 ID: ${__ENV.K6_NOTICE_ID || '1'}
- 목표 RPS(K6_RATE): ${__ENV.K6_RATE || '(scenario 기본)'}
- 시드 데이터 버전: (직접 기입 — 대상 챌린저 수 등)

## 가설
- (직접 기입)

## 결과 — k6 측정 지표
| 지표 | 값 |
|------|-----|
${metricsTable(data)}

## 서버측 지표 (k6 밖에서 확인 — 직접 기입)
> k6 summary 에는 서버 내부 SQL 실행 횟수/DB 풀 사용량이 포함되지 않는다.
> 아래는 P6Spy 로그, Actuator, Prometheus 등에서 확인해 기입한다.
- 요청당 SQL 실행 수:
- DB connection peak:

## 후속 액션
- (직접 기입)
`;
}

function renderStdout(data) {
  return (
    [
      `p95=${fmt(metricValue(data, 'endpoint_duration', 'p(95)'))}ms`,
      `p99=${fmt(metricValue(data, 'endpoint_duration', 'p(99)'))}ms`,
      `success=${pct(metricValue(data, 'endpoint_success', 'rate'))}`,
      `http_fail=${pct(metricValue(data, 'http_req_failed', 'rate'))}`,
      `rps=${fmt(metricValue(data, 'http_reqs', 'rate'))}`,
      `dropped=${fmt(metricValue(data, 'dropped_iterations', 'count'), 0)}`,
    ].join('  ') + '\n'
  );
}

export function handleSummary(data) {
  const outDir = __ENV.K6_OUT_DIR || 'docs/loadtest/runs/local-adhoc';
  const out = {};
  out['stdout'] = renderStdout(data);
  out[`${outDir}/summary.json`] = JSON.stringify(data, null, 2);
  out[`${outDir}/summary.md`] = renderMarkdown(data);
  return out;
}
