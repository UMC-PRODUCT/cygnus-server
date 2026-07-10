# 인증서 운영진 발급 API 가이드

## 엔드포인트

`POST /api/v1/certificates/admin`

운영진이 PDF 배경 템플릿이 준비된 수료증, 공로증, 상장을 단건 발급할 때 사용한다.

## 필드 사용 기준

| 필드 | 사용 시점 |
| --- | --- |
| `template` | 필수다. 서버가 발급 정책, 일련번호 코드, 발급 주체, 기본 상명을 결정한다. |
| `recipientMemberId` | 인증서를 받을 회원 ID다. |
| `gisuId` | 인증서에 표시할 기수이자 자격 판정 기준 기수다. |
| `meritTitle` | 공로증/상장 제목을 커스터마이즈할 때 사용한다. 비우면 `template`의 기본 상명이 들어간다. |
| `meritDescription` | 공로증/상장 본문을 커스터마이즈할 때 사용한다. 비우면 서버가 템플릿과 기수 기반 기본 문구를 생성한다. |
| `reissue` | 동일 범위 유효 인증서가 있을 때 기존 인증서를 폐기하고 새로 발급할지 여부다. |

`template`만 인증서 종류와 발급 주체를 선택하는 입력이다. 새로운 인증서를 추가할 때는 `CertificateTemplate` enum에 템플릿을 추가한다.

## 템플릿 목록

- `UMC_COURSE_COMPLETION`
- `UMC_COURSE_MERIT`
- `UMC_DEMO_DAY_GRAND_PRIZE`
- `UMC_DEMO_DAY_FIRST_PRIZE`
- `UMC_DEMO_DAY_SECOND_PRIZE`
- `UMC_DEMO_DAY_PARTICIPATION_PRIZE`
- `UMC_DEMO_DAY_AWS_SPECIAL_PRIZE`
- `UMC_DEMO_DAY_BEST_CHALLENGER`
- `UMC_HACKATHON_CERTIFICATION_OF_COMPLETION`
- `UMC_HACKATHON_GRAND_PRIZE`
- `UMC_HACKATHON_FIRST_PRIZE`
- `UMC_HACKATHON_SECOND_PRIZE`
- `NEORDINARY_HACKATHON_GRAND_PRIZE`
- `NEORDINARY_HACKATHON_FIRST_PRIZE`
- `NEORDINARY_HACKATHON_SECOND_PRIZE`
- `NEORDINARY_HACKATHON_CERTIFICATION_OF_COMPLETION`

## 요청 예시

UMC 데모데이 최우수상:

```json
{
  "template": "UMC_DEMO_DAY_FIRST_PRIZE",
  "recipientMemberId": 1,
  "gisuId": 7,
  "reissue": true
}
```

Ne(o)rdinary 해커톤 대상:

```json
{
  "template": "NEORDINARY_HACKATHON_GRAND_PRIZE",
  "recipientMemberId": 1,
  "gisuId": 7,
  "reissue": true
}
```

CUSTOM 상장:

```json
{
  "template": "UMC_DEMO_DAY_SECOND_PRIZE",
  "recipientMemberId": 1,
  "gisuId": 7,
  "meritTitle": "커스텀 공로상",
  "meritDescription": "프로젝트와 커뮤니티 성장에 크게 기여하였기에 이 상장을 수여합니다.",
  "reissue": true
}
```
