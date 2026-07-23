# UMC Notification 채용 이메일 디자인 시스템

이 문서는 정적 Thymeleaf 이메일의 디자인 계약이다. 제공된 당근 HTML은 브랜드·문구·자산을 복제하지 않고, 하단 table 구조와 반복되는 spacing grammar만 추출하는 참고 입력으로 사용했다.

## 1. Atmosphere & Identity

지원자가 결과를 빠르게 이해하고 다음 행동을 놓치지 않도록 하는 조용하고 정중한 안내문이다. 빈 배너 행 뒤에 결과 제목, 필요한 정보, (서류 합격 메일에만) 단일 CTA를 순서대로 배치한다. 브랜드 장식 대신 넓은 여백과 얇은 divider가 신뢰감을 만든다.

## 2. Color

### Palette

| Role | Token | Value | Usage |
|---|---|---|---|
| Surface/primary | `--email-surface-primary` | `#ffffff` | 본문 배경 |
| Surface/page | `--email-surface-page` | `#f7f8fa` | 메일 바깥 여백 |
| Text/primary | `--email-text-primary` | `#161919` | 제목·본문 |
| Text/secondary | `--email-text-secondary` | `#4f5757` | 설명·문의 |
| Text/tertiary | `--email-text-tertiary` | `#707878` | footer·보조 문구 |
| Border/divider | `--email-border-divider` | `#dcdee3` | 0.5px 구분선 |
| Accent/action | `--email-accent-action` | `#0b6b64` | CTA·주요 링크 |
| Accent/action-hover | `--email-accent-action-hover` | `#09564f` | 지원 가능한 hover |

이메일 클라이언트는 CSS custom property와 외부 stylesheet를 일관되게 지원하지 않으므로 위 토큰의 literal 값은 fragment의 inline style에 반복한다. 새 색상은 이 표에 먼저 추가한다.

## 3. Typography

| Level | Size | Weight | Line Height | Usage |
|---|---:|---:|---:|---|
| Title | 26px | 800 | 38px | 결과 제목 |
| Body | 16px | 400 | 28px | 안내·설명 |
| Data label | 13px | 700 | 20px | 일정/연락처 label |
| Data value | 16px | 400 | 28px | 동적 값 |
| CTA | 14px | 700 | 20px | 행동 링크 |
| Footer | 12px | 400 | 18px | 법적·서비스 링크 |

본문은 `-apple-system, BlinkMacSystemFont, "Segoe UI", "Apple SD Gothic Neo", "Malgun Gothic", system-ui, sans-serif`만 사용한다. 외부 font 요청과 고정 폭 font는 없다. 한국어 문장은 `word-break:keep-all`로 어미·조사 중간 분리를 막고, 긴 영문·연속 문자열은 `overflow-wrap:anywhere` fallback으로 줄바꿈한다.

## 4. Spacing & Layout

- 외부 좌우 gutter: 30px (`--email-gutter`), `max-width:480px` narrow viewport에서는 16px로 줄여 320px에서 overflow를 막는다.
- 콘텐츠 table: `width:100%; max-width:620px; min-width:300px`; `max-width:480px` media query에서 `min-width:0`으로 intrinsic overflow를 방지한다.
- 주요 vertical rhythm: 44px; content 내부 단락 간격은 16px/24px를 사용한다.
- divider: `0.5px solid #dcdee3`.
- footer: 중앙 정렬, 12px/18px.
- 메일은 한 열 table shell이며, body → banner → title → body/data → CTA → footer의 logical DOM order를 유지한다.

## 5. Components

### EmailShell

- **Structure**: page gutter table → max/min-width content table → empty banner row → content slot → Footer.
- **Variants**: 네 가지 결과 template; `banner`는 항상 빈 상태.
- **Spacing**: 30px outer gutter, 44px banner/section rhythm.
- **States**: static only; loading/error/disabled는 해당 없음.
- **Accessibility**: `lang="ko"`, `role="presentation"` tables, semantic heading/paragraph order.
- **Motion**: 없음.

### Banner

- **Structure**: 높이 44px의 빈 `td`와 non-content marker.
- **Variants**: empty만 허용.
- **Spacing**: 44px.
- **States**: empty; `<img>`와 `src`를 절대 추가하지 않는다.
- **Accessibility**: `aria-hidden="true"`.
- **Motion**: 없음.

### DataRow

- **Structure**: label/value가 세로로 쌓이는 한 열 table row.
- **Variants**: 날짜, 시간, 장소, 연락처, 합격 트랙.
- **Spacing**: row 사이 16px, label 20px line-height.
- **States**: populated; catalog가 blank를 거부하므로 empty state 없음.
- **Accessibility**: label을 먼저 읽고 `th:text` 값으로 escape한다.
- **Motion**: 없음.

### CTA

- **Structure**: centered anchor with inline background, padding, focus-friendly contrast.
- **Variants**: document-passed template에만 1개.
- **Spacing**: 12px vertical, 20px horizontal padding.
- **States**: default/visited; email client hover/focus는 user-agent에 맡긴다.
- **Accessibility**: 목적을 설명하는 정확한 link text, `th:href`로 URL attribute escaping.
- **Motion**: 없음.

### Footer

- **Structure**: divider → 안내 문구 → 홈페이지/개인정보/이용약관 links → copyright.
- **Variants**: 네 template 공통.
- **Spacing**: 24px top/bottom, 12px/18px text. 각 footer link는 `inline-block`과 13px 세로 padding으로 최소 44px hit area를 갖는다.
- **States**: static.
- **Accessibility**: 설명적인 link text와 충분한 대비.
- **Motion**: 없음.

### SemanticToken

- **Structure**: 짧은 한국어 의미구 또는 보조용언 표현만 inline `white-space: nowrap` span으로 감싼다.
- **Variants**: `제출해 주세요`, `확인 후`, `일정을 확인해 주세요`, `합격 트랙은`, `아래와 같습니다`, `함께하기를 응원합니다`처럼 폭 안에 들어오는 정적 문구만 허용한다.
- **States**: static only. 전체 paragraph/title은 절대 nowrap 처리하지 않는다.
- **Accessibility**: 원문 순서와 text semantics를 유지하며, 동적·무제한 값은 `overflow-wrap:anywhere` fallback을 유지한다.

## 6. Motion & Interaction

정적 이메일에는 애니메이션, script, hover 전용 의미 변화가 없다. 링크는 기본/visited 상태만 제공하며, reduced-motion 대응이 필요한 motion은 의도적으로 존재하지 않는다. CTA와 footer 링크만 상호작용 표면이다.

## 7. Depth & Surface

`borders-only` 전략을 사용한다. 카드 그림자, gradient, radius 장식은 사용하지 않고 흰 본문 표면과 연한 page background, 0.5px divider로 계층을 만든다. 이메일 client마다 box-shadow/gradient 지원이 달라 literal border가 가장 안정적이다.

## 8. Accessibility Constraints & Accepted Debt

### Constraints

- WCAG 2.2 AA를 목표로 body 대비 4.5:1 이상, 큰 제목 대비 3:1 이상을 유지한다.
- 320px viewport와 200% text sizing에서 가로 overflow 없이 읽히고, CJK 문장이 semantic phrase 단위로 자연스럽게 줄바꿈되어야 한다.
- 이름·연락처·URL·일정은 `th:text`/`th:href`로 escape하며 `th:utext`를 사용하지 않는다.
- CTA는 한 메일에 하나만 두고, 결과를 먼저 읽은 뒤 다음 행동으로 이동하는 logical order를 유지한다.
- 동적 값에 긴 CJK 또는 unbroken string이 들어와도 `word-break:keep-all`과 `overflow-wrap:anywhere` 조합으로 의미 단위와 화면 폭을 함께 보존한다.

### Accepted Debt

| Item | Location | Why accepted | Owner / Exit |
|---|---|---|---|
| 이메일 client별 CSS media-query 편차 | `email/recruitment/shell.html` | 320px fallback은 inline base + 좁은 viewport override를 함께 제공한다. | Notification, 실제 Gmail/Outlook matrix가 추가되면 재검증 |
