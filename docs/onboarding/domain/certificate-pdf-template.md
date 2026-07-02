# Certificate PDF Template

이 문서는 인증서 PDF 배경 템플릿 위에 동적으로 올라가는 텍스트와 QR의 조정 방식을 설명한다.

## 렌더링 진입점

- 렌더러: `src/main/java/com/umc/product/certificate/adapter/out/pdf/ThymeleafCertificatePdfAdapter.java`
- 템플릿 enum: `src/main/java/com/umc/product/certificate/domain/CertificateTemplate.java`
- 좌표 설정: `src/main/resources/certificate/config/certificate_template.json`
- 배경 PDF: `src/main/resources/certificate/backgrounds/*.pdf`
- 폰트: `src/main/resources/certificate/fonts/Pretendard-*.ttf`

인증서 PDF는 Thymeleaf HTML 템플릿을 사용하지 않는다. `CertificatePdfRenderCommand.template()`이 가리키는 배경 PDF를 연 뒤 PDFBox로 텍스트와 선, QR을 덧그린다. `certificate_template.json`은 이때 사용할 공통 필드, 품목 필드, 선, 폰트, 좌표계를 정의하는 렌더링 설정 파일이다. `CertificateTemplate.itemCount()` 값으로 `certificate_template.json`의 `layouts.{itemCount}`를 선택한다.

## 좌표계

- 기준 캔버스는 Figma 프레임과 동일한 A4 landscape `2970px x 2100px`이다.
- 품목 레이아웃은 Figma 템플릿의 품목 영역에서 가져온 값이다. `certificate_template.json`의 notes에 남긴 것처럼 5개 품목은 Figma frame `152:1256`, 6개 품목은 `152:3864`, 4개 품목은 `155:2` 기준이다.
- JSON의 `xPx`, `yPx`는 좌상단 원점 기준이다.
- PDFBox는 좌하단 원점이므로 렌더러가 `pageHeight - yPx * pxToPt` 형태로 변환한다.
- 변환 비율은 `pxToPt = 0.283464567`이다.
- 텍스트 박스의 크기는 `widthPx`, `heightPx`로 제한한다.
- 글자는 `fontSizePt`에서 시작하고, 박스에 맞지 않으면 `minFontSizePt`까지 `0.5pt` 단위로 줄인다.
- `allowWrap=true`이면 공백 기준 줄바꿈 후 긴 단어는 코드포인트 단위로 나눈다. `maxLines`를 넘으면 렌더링 실패로 처리한다.

## 템플릿별 품목 개수

현재 모든 enum은 `CertificateTemplate.DEFAULT_ITEM_COUNT = 4`를 사용한다. 따라서 아래 모든 템플릿은 `layouts.4`를 사용한다. 5개 또는 6개 품목 레이아웃은 JSON에 준비되어 있지만, 실제 템플릿에서 쓰려면 enum이 템플릿별 item count를 반환하도록 확장해야 한다.

| Template | Issuer | Type | Event | Background | 품목 개수 | Layout |
| --- | --- | --- | --- | --- | ---: | --- |
| `UMC_COURSE_COMPLETION` | UMC | `COMPLETION` | course | `umc-course-completion.pdf` | 4 | `layouts.4` |
| `UMC_COURSE_MERIT` | UMC | `MERIT` | course | `umc-course-completion.pdf` | 4 | `layouts.4` |
| `UMC_DEMO_DAY_GRAND_PRIZE` | UMC | `MERIT` | demo | `umc-demo-day-grand-prize.pdf` | 4 | `layouts.4` |
| `UMC_DEMO_DAY_FIRST_PRIZE` | UMC | `MERIT` | demo | `umc-demo-day-first-prize.pdf` | 4 | `layouts.4` |
| `UMC_DEMO_DAY_SECOND_PRIZE` | UMC | `MERIT` | demo | `umc-demo-day-second-prize.pdf` | 4 | `layouts.4` |
| `UMC_DEMO_DAY_PARTICIPATION_PRIZE` | UMC | `MERIT` | demo | `umc-demo-day-participation-prize.pdf` | 4 | `layouts.4` |
| `UMC_DEMO_DAY_AWS_SPECIAL_PRIZE` | UMC | `MERIT` | demo | `umc-demo-day-aws-special-prize.pdf` | 4 | `layouts.4` |
| `UMC_DEMO_DAY_BEST_CHALLENGER` | UMC | `MERIT` | demo | `umc-demo-day-best-part-challenger.pdf` | 4 | `layouts.4` |
| `UMC_HACKATHON_CERTIFICATION_OF_COMPLETION` | UMC | `COMPLETION` | hackathon | `umc-course-completion.pdf` | 4 | `layouts.4` |
| `UMC_HACKATHON_GRAND_PRIZE` | UMC | `MERIT` | hackathon | `umc-demo-day-grand-prize.pdf` | 4 | `layouts.4` |
| `UMC_HACKATHON_FIRST_PRIZE` | UMC | `MERIT` | hackathon | `umc-demo-day-first-prize.pdf` | 4 | `layouts.4` |
| `UMC_HACKATHON_SECOND_PRIZE` | UMC | `MERIT` | hackathon | `umc-demo-day-second-prize.pdf` | 4 | `layouts.4` |
| `NEORDINARY_HACKATHON_GRAND_PRIZE` | Ne(O)rdinary | `MERIT` | hackathon | `neordinary-hackathon-grand-prize.pdf` | 4 | `layouts.4` |
| `NEORDINARY_HACKATHON_FIRST_PRIZE` | Ne(O)rdinary | `MERIT` | hackathon | `neordinary-hackathon-first-prize.pdf` | 4 | `layouts.4` |
| `NEORDINARY_HACKATHON_SECOND_PRIZE` | Ne(O)rdinary | `MERIT` | hackathon | `neordinary-hackathon-second-prize.pdf` | 4 | `layouts.4` |
| `NEORDINARY_HACKATHON_CERTIFICATION_OF_COMPLETION` | Ne(O)rdinary | `COMPLETION` | hackathon | `neordinary-hackathon-certification-of-completion.pdf` | 4 | `layouts.4` |

## 공통 텍스트 필드

공통 필드는 모든 템플릿에 동일하게 적용된다. 위치와 크기는 `commonFields`에서 조정한다.

| Key | 용도 | x/y px | box px | font pt | 정렬 | 줄 수 |
| --- | --- | --- | --- | ---: | --- | ---: |
| `award_en_subtitle` | 좌상단 영문 부제목 | `200 / 200` | `1450 x 50` | 11.905 | LEFT | 1 |
| `award_ko_subtitle` | 우상단 국문 부제목 | `1555 / 200` | `500 x 50` | 11.905 | RIGHT | 1 |
| `award_en_title_line1` | 대형 영문 타이틀 1행 | `200 / 330` | `1216 x 192` | 45.354 | LEFT | 1 |
| `award_en_title_line2` | 대형 영문 타이틀 2행 | `200 / 522` | `1216 x 192` | 45.354 | LEFT | 1 |
| `award_ko_title` | 국문 상장 타이틀 | `200 / 852` | `1855 x 96` | 22.677 | LEFT | 1 |
| `award_description` | 본문 설명 | `200 / 990` | `1855 x 144` | 13.606 | LEFT | 2 |
| `static_issue_title` | 하단 고정 문구 | `200 / 1627` | `1587 x 96` | 22.677 | LEFT | 1 |
| `static_issue_date_label` | 발급일 라벨 | `200 / 1766` | `107 x 63` | 11.905 | LEFT | 1 |
| `issue_date` | 발급일 값 | `349 / 1766` | `500 x 63` | 11.905 | LEFT | 1 |
| `static_issuer_label` | 발급기관 라벨 | `1069 / 1766` | `143 x 63` | 11.905 | LEFT | 1 |
| `static_issuer_value` | 발급기관 값 | `1254 / 1766` | `533 x 63` | 11.905 | LEFT | 1 |
| `static_issuance_number_label` | 발급번호 라벨 | `1069 / 1837` | `143 x 63` | 11.905 | LEFT | 1 |
| `issuanceNumber` | 발급번호 값 | `1254 / 1837` | `500 x 63` | 11.905 | LEFT | 1 |

## 품목 필드

품목 라벨과 값은 `layouts.{itemCount}.fields`에서 조정한다. 현재 렌더러가 채우는 품목 값은 4개다.

| 순서 | Label | Value |
| ---: | --- | --- |
| 1 | `성명` | `recipientName` |
| 2 | `소속` | `recipientSchoolName`, 없으면 `-` |
| 3 | `기수` | `{gisuGeneration}기` |
| 4 | `구분` | 공로증/수료증/상장의 상장명 |

### 4개 품목 레이아웃

| 품목 | label x/y/box/font | value x/y/box/font |
| ---: | --- | --- |
| 1 | `200 / 1238 / 436.75x50 / 11.905pt` | `200 / 1353.48 / 436.75x116 / 13.606pt` |
| 2 | `672.75 / 1238 / 436.75x50 / 11.905pt` | `672.75 / 1353.48 / 436.75x116 / 13.606pt` |
| 3 | `1145.5 / 1238 / 436.75x50 / 11.905pt` | `1145.5 / 1353.48 / 436.75x116 / 13.606pt` |
| 4 | `1618.25 / 1238 / 436.75x50 / 11.905pt` | `1618.25 / 1353.48 / 436.75x116 / 13.606pt` |

### 5개 품목 레이아웃

| 품목 | label x/y/box/font | value x/y/box/font |
| ---: | --- | --- |
| 1 | `200 / 1238 / 342.2x50 / 11.905pt` | `200 / 1353.5 / 342.2x116 / 13.606pt` |
| 2 | `578.2 / 1238 / 342.2x50 / 11.905pt` | `578.2 / 1353.5 / 342.2x116 / 13.606pt` |
| 3 | `956.4 / 1238 / 342.2x50 / 11.905pt` | `956.4 / 1353.5 / 342.2x116 / 13.606pt` |
| 4 | `1334.6 / 1238 / 342.2x50 / 11.905pt` | `1334.6 / 1353.5 / 342.2x116 / 13.606pt` |
| 5 | `1712.8 / 1238 / 342.2x50 / 11.905pt` | `1712.8 / 1353.5 / 342.2x116 / 13.606pt` |

### 6개 품목 레이아웃

| 품목 | label x/y/box/font | value x/y/box/font |
| ---: | --- | --- |
| 1 | `200 / 1238 / 279.17x50 / 11.905pt` | `200 / 1353.5 / 279.17x116 / 13.606pt` |
| 2 | `515.17 / 1238 / 279.17x50 / 11.905pt` | `515.17 / 1353.5 / 279.17x116 / 13.606pt` |
| 3 | `830.33 / 1238 / 279.17x50 / 11.905pt` | `830.33 / 1353.5 / 279.17x116 / 13.606pt` |
| 4 | `1145.5 / 1238 / 279.17x50 / 11.905pt` | `1145.5 / 1353.5 / 279.17x116 / 13.606pt` |
| 5 | `1460.67 / 1238 / 279.17x50 / 11.905pt` | `1460.67 / 1353.5 / 279.17x116 / 13.606pt` |
| 6 | `1775.83 / 1238 / 279.17x50 / 11.905pt` | `1775.83 / 1353.5 / 279.17x116 / 13.606pt` |

품목 라벨은 중앙 정렬, 1줄, 줄바꿈 없음이다. 품목 값은 중앙 정렬, 최대 2줄, 줄바꿈 허용이다. 둘 다 `minFontSizePt = 6.0`까지 자동 축소된다.

## 선과 QR

- 품목 구분선은 `layouts.{itemCount}.shapes`에 있다.
- 현재 선은 각 품목 상자마다 `bottom_line`, `yPx = 1214`, `heightPx = 98`, `strokeRGB = #B9B9B9`, `strokeWidthPx = 1`로 그린다.
- QR은 JSON이 아니라 렌더러 상수로 제어한다.
- `QR_CODE_ENABLED = true`이면 검증 URL을 `58pt x 58pt`로 그린다.
- QR 위치는 페이지 오른쪽에서 `188pt`, 아래에서 `36pt` 떨어진 곳이다.

## 조정 절차

1. 텍스트 위치나 크기만 바꿀 때는 `certificate_template.json`의 해당 필드 `xPx`, `yPx`, `widthPx`, `heightPx`, `fontSizePt`, `minFontSizePt`, `maxLines`, `allowWrap`을 수정한다.
2. 품목 개수를 바꿀 때는 `CertificateTemplate.itemCount()`가 해당 템플릿에 대해 `4`, `5`, `6` 중 맞는 값을 반환하도록 먼저 확장한다.
3. 새로운 품목 값을 추가할 때는 `buildFieldValues()`에서 `item_label_N`, `item_value_N` 값을 채우고, JSON의 `layouts.N.fields`에 같은 key가 있어야 한다.
4. 배경 PDF가 바뀌면 `CertificateTemplate.backgroundResourcePath()`가 가리키는 리소스를 교체하고, 샘플 PDF를 생성해 실제 겹침 여부를 확인한다.
5. QR을 숨겨야 하면 `QR_CODE_ENABLED`를 `false`로 바꾸면 된다.

## 로컬 미리보기

local 또는 dev profile에서는 테스트 도메인에서 PDF를 바로 내려받을 수 있다.

```bash
curl -L \
  "http://localhost:8080/test/certificates/preview?template=UMC_DEMO_DAY_FIRST_PRIZE&recipientName=%EA%B9%80%EC%9C%A0%EC%97%A0&gisuGeneration=7" \
  -o build/certificate-preview.pdf
```

주요 query parameter는 아래와 같다.

| Parameter | 필수 | 설명 |
| --- | --- | --- |
| `template` | 예 | `CertificateTemplate` enum 이름 |
| `issuanceNumber` | 아니오 | 생략하면 `UMC-{typeCode}-20260703-SAMPLE01` 사용 |
| `recipientName` | 아니오 | 생략하면 `김유엠` 사용 |
| `recipientSchoolName` | 아니오 | 생략하면 `유엠씨대학교` 사용 |
| `gisuGeneration` | 아니오 | 생략하면 `7` 사용 |
| `projectName` | 아니오 | 프로젝트 참가 확인서 계열 확인용 값 |
| `meritTitle` | 아니오 | 상장명 override |
| `meritDescription` | 아니오 | 본문 설명 override |
| `verificationUrl` | 아니오 | QR에 넣을 검증 URL override |

`verificationUrl`을 생략하면 `certificate.verification-url-template`을 사용한다. 해당 값이 `/api/...`처럼 path-only이면 preview endpoint가 현재 요청 origin을 붙여 `http://localhost:8080/api/...` 형태로 QR을 만든다. 실제 발급 플로우는 요청 origin을 자동 합성하지 않으므로 운영 환경에서는 `CERTIFICATE_VERIFICATION_URL_TEMPLATE`에 absolute URL을 설정해야 한다.
