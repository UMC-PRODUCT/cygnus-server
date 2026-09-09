# loadtest/k6

부하 테스트 실행 코드. 문서가 아니라 러너가 실제로 쓰는 k6 스크립트다.
실행 명령과 설계 결정은 `loadtest/README.md` 참조.

## 구조

- `script.js` — 단일 entrypoint. `PROFILE`(부하 유형) × `SCENARIO`(업무 시나리오)를 REGISTRY 에서 고른다.
  `systemTags` 에서 url·name 을 제거한다 (시리즈 카디널리티 — 설계 결정 "관측" 참조).
- `config/profiles.js` — smoke(VU)/load·stress·soak(arrival-rate)/breakpoint(점증) 별 options·thresholds.
- `lib/` — `http`(공통 요청/체크·batch), `auth`(토큰 발급+VU 캐시, scenario=auth 태그), `data`(seed.json 로드), `metrics`(시나리오 에러율).
- `scenarios/<profile>/*.js` — 업무 시나리오. `requiresSeed` 와 default 함수를 export 한다.
- `data/seed.json` — `prepare-data.sh` 산출물(gitignore). 형태는 `seed.example.json`.

## 시나리오 추가

`scenarios/<profile>/<name>.js` 에 `requiresSeed` 와 default 함수를 만들고 `script.js` REGISTRY 에 한 줄 등록.
인증은 `lib/auth.js`, 요청은 `lib/http.js`(scenario 태그 필수 — 안 붙이면 지표가 executor 이름으로 뭉개진다).
새 시드 데이터가 필요하면 `loadtest/README.md` "확장" 절의 규모별 분기를 따른다.
