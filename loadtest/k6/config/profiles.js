// 부하 유형(profile)별 k6 실행 옵션·thresholds 를 만든다.
// RATE/DURATION 환경변수가 없으면 유형별 기본값을 쓴다.
const DEFAULT_DURATION = {
  smoke: "1m",
  load: "10m",
  stress: "20m",
  soak: "2h",
  breakpoint: "10m",
  spike: "2m", // 급등 후 유지 구간 길이 (앞 10s 급등 + 뒤 30s 회복은 고정)
};

function thresholds(profile) {
  // stress 는 한계점 탐색이라 에러/지연 허용치를 완화한다. 나머지는 SLO 에 가깝게 죈다.
  const failRate = profile === "stress" ? "rate<0.05" : "rate<0.01";
  const p95 = profile === "stress" ? 2000 : 800;
  return {
    http_req_failed: [failRate],
    http_req_duration: [`p(95)<${p95}`],
  };
}

// arrival-rate executor 의 VU 풀 크기. rate 기준으로 여유 있게 잡되 상한을 둔다.
function vuPool(rate) {
  return {
    preAllocatedVUs: Math.min(Math.max(rate, 10), 500),
    maxVUs: Math.min(Math.max(rate * 2, 50), 1000),
  };
}

export function buildOptions(profile, rateEnv, durationEnv) {
  const rate = Math.max(parseInt(rateEnv, 10) || 1, 1);
  const duration =
    durationEnv && durationEnv.length ? durationEnv : DEFAULT_DURATION[profile];

  // smoke 는 "테스트 자체가 도는지"를 보는 사전 점검이라 소수 VU 로 짧게 돈다.
  if (profile === "smoke") {
    return {
      vus: rate,
      duration: duration,
      thresholds: thresholds(profile),
    };
  }

  // spike 는 "급등 기울기가 죽이는가"를 본다 — breakpoint 와 다른 테스트다.
  // 같은 rate 라도 완만 점증은 버티고 10초 급등은 죽을 수 있다(풀·JIT·캐시가 식은 상태에서 맞기 때문).
  // 평시 저부하 → 10초 급등 → duration 유지 → 30초 rate 0 으로 회복(backlog 소화)까지 관찰.
  // 이벤트성 폭주(지원 오픈·공지 푸시·출석) 재현. thresholds 는 stress 완화치 재사용.
  if (profile === "spike") {
    return {
      scenarios: {
        spike: {
          executor: "ramping-arrival-rate",
          startRate: Math.max(Math.floor(rate / 20), 1),
          timeUnit: "1s",
          stages: [
            { target: rate, duration: "10s" },
            { target: rate, duration: duration },
            { target: 0, duration: "30s" },
          ],
          ...vuPool(rate),
        },
      },
      thresholds: thresholds("stress"),
    };
  }

  // breakpoint 는 도착률을 1 → RATE 로 선형 점증시켜 포화점(knee)을 찾는다:
  // TPS 가 평탄해지고 응답시간이 급등하는 지점이 시스템 한계. Grafana 의
  // "포화점 탐색" 패널(VUs·TPS·응답시간 겹침)과 짝으로 본다.
  // thresholds 를 두지 않는다 — "언제부터 깨지는가" 자체가 이 테스트의 결과라서다.
  if (profile === "breakpoint") {
    return {
      scenarios: {
        breakpoint: {
          executor: "ramping-arrival-rate",
          startRate: 1,
          timeUnit: "1s",
          stages: [{ target: rate, duration: duration }],
          ...vuPool(rate),
        },
      },
    };
  }

  // load/stress/soak 은 목표 처리량(RATE req/s)을 constant-arrival-rate 로 고정한다.
  // 도착률 기반이라 SUT 가 느려져도 부하가 밀리지 않고 목표 rate 를 유지한다 —
  // "요청을 얼마나 던졌나"가 아니라 "SUT 가 얼마나 받아내나"를 본다.
  return {
    scenarios: {
      [profile]: {
        executor: "constant-arrival-rate",
        rate: rate,
        timeUnit: "1s",
        duration: duration,
        ...vuPool(rate),
      },
    },
    thresholds: thresholds(profile),
  };
}
