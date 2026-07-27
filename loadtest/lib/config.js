// 부하 테스트 공통 설정. 모든 값은 환경변수(__ENV)로 주입하고, 없으면 local 기본값을 쓴다.
// 같은 스크립트를 local / staging 에 재사용하기 위한 단일 진입점.

// 대상 서버 origin. local bootRun 기본 포트 8080.
export const BASE_URL = __ENV.K6_BASE_URL || 'http://localhost:8080';

// 테스트 대상 공지 ID.
export const NOTICE_ID = __ENV.K6_NOTICE_ID || '1';

// --- 토큰 발급용 (auth.js 에서 사용). 우선순위: RAW_TOKEN > MEMBER_ID > EMAIL/PASSWORD ---

// 방식 A(권장, local 전용): test 도메인 토큰 발급 API 로 memberId 만으로 비번 없이 발급.
//   GET /test/token/access?memberId=... ( @Profile("local | dev") 에서만 활성 )
export const MEMBER_ID = __ENV.K6_MEMBER_ID || '';

// 방식 B: 이메일 로그인 API 로 실시간 발급 (staging 등 test 토큰 API 가 없는 환경).
export const EMAIL = __ENV.K6_EMAIL || '';
export const PASSWORD = __ENV.K6_PASSWORD || '';

// 방식 C(대체): 이미 발급받은 AccessToken 을 직접 주입. 지정 시 발급 과정을 건너뛴다.
export const RAW_TOKEN = __ENV.K6_TOKEN || '';

// 발급 토큰 만료(분). 장기 실행(soak) 시 토큰 만료로 인한 401 을 막기 위해 테스트 길이보다 크게 잡는다.
// test 토큰 API(방식 A)에만 적용된다. (로그인 API 는 서버 고정 만료를 따름)
export const TOKEN_TTL_MIN = __ENV.K6_TOKEN_TTL_MIN || '';
