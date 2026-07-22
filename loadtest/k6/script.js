import { buildOptions } from "./config/profiles.js";
import { parseSeed } from "./lib/data.js";

import healthCheck, {
  requiresSeed as healthCheckSeed,
} from "./scenarios/smoke/health-check.js";
import smokeProjectRead, {
  requiresSeed as smokeProjectReadSeed,
} from "./scenarios/smoke/project-read.js";
import loadProjectRead, {
  requiresSeed as loadProjectReadSeed,
} from "./scenarios/load/project-read.js";
import applicationSubmit, {
  requiresSeed as applicationSubmitSeed,
} from "./scenarios/load/application-submit.js";
import stressProjectRead, {
  requiresSeed as stressProjectReadSeed,
} from "./scenarios/stress/project-read.js";
import soakProjectRead, {
  requiresSeed as soakProjectReadSeed,
} from "./scenarios/soak/project-read.js";
import smokeHome, { requiresSeed as smokeHomeSeed } from "./scenarios/smoke/home.js";
import loadHome, { requiresSeed as loadHomeSeed } from "./scenarios/load/home.js";

// PROFILE(부하 유형) × SCENARIO(업무 시나리오) → 실제 실행 함수 매핑.
// k6 는 런타임 동적 import 를 지원하지 않으므로, 정적 import 를 registry 로 모아 __ENV 로 고른다.
const REGISTRY = {
  smoke: {
    "health-check": { fn: healthCheck, requiresSeed: healthCheckSeed },
    "project-read": { fn: smokeProjectRead, requiresSeed: smokeProjectReadSeed },
    home: { fn: smokeHome, requiresSeed: smokeHomeSeed },
  },
  load: {
    "project-read": { fn: loadProjectRead, requiresSeed: loadProjectReadSeed },
    "application-submit": {
      fn: applicationSubmit,
      requiresSeed: applicationSubmitSeed,
    },
    home: { fn: loadHome, requiresSeed: loadHomeSeed },
  },
  stress: {
    "project-read": { fn: stressProjectRead, requiresSeed: stressProjectReadSeed },
  },
  soak: {
    "project-read": { fn: soakProjectRead, requiresSeed: soakProjectReadSeed },
  },
};

const PROFILE = __ENV.PROFILE || "smoke";
const SCENARIO = __ENV.SCENARIO || "health-check";

const selected = (REGISTRY[PROFILE] || {})[SCENARIO];
if (!selected) {
  const available = Object.keys(REGISTRY)
    .map((p) => `${p}[${Object.keys(REGISTRY[p]).join(",")}]`)
    .join(" ");
  throw new Error(
    `알 수 없는 PROFILE/SCENARIO: ${PROFILE}/${SCENARIO}. 사용 가능: ${available}`,
  );
}

// options 는 init 단계에서 평가된다. RATE/DURATION 은 run-umc-k6 가 넘긴다.
export const options = buildOptions(PROFILE, __ENV.RATE, __ENV.DURATION);

// open() 은 init 단계에서만 동작한다. 시딩이 필요한 시나리오일 때만 seed.json 을 읽는다.
// 파일이 없으면 여기서 실패한다 — prepare-data.sh 로 먼저 시딩하거나 seed.example.json 을 복사한다.
const seedRaw = selected.requiresSeed
  ? open(__ENV.SEED_FILE || "./data/seed.json")
  : null;

export function setup() {
  // setup 반환값은 default 함수 data 인자로 전달된다. k6 는 시딩하지 않고 seed 파일만 넘긴다.
  return { seed: parseSeed(seedRaw) };
}

export default function (data) {
  selected.fn(data.seed);
}
