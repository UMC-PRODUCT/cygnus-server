// seed.json 로드 전용. k6 는 시딩하지 않는다 — 시딩은 prepare-data.sh 가 소유한다.
// open() 은 init 단계에서만 동작하므로 script.js init 에서 raw 를 읽어 parseSeed 로 넘긴다.
export function parseSeed(raw) {
  if (!raw) {
    return null;
  }
  return JSON.parse(raw);
}

// VU 마다 seed memberIds 중 하나를 골라 로그인에 쓴다.
export function pickMemberId(seed) {
  const ids = (seed && seed.memberIds) || [];
  if (ids.length === 0) {
    throw new Error(
      "seed.memberIds 가 비어 있습니다. loadtest/scripts/prepare-data.sh 로 먼저 시딩하세요.",
    );
  }
  return ids[Math.floor(Math.random() * ids.length)];
}

// 지원/조회 대상 projectId. targets 가 비면 null.
export function pickProjectId(seed) {
  const targets = (seed && seed.targets) || [];
  if (targets.length === 0) {
    return null;
  }
  return targets[Math.floor(Math.random() * targets.length)].projectId;
}
