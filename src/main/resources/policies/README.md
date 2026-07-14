# UMC Policy 문서 모델

이 디렉터리는 애플리케이션에 포함되어 시작 시 compile되는 authorization policy를 관리한다.
정책은 외부 경로, URL, 요청 값 또는 환경변수로 주입하지 않는다. 각 도메인의 고정
resource manifest가 classpath 위치를 선언하며, 같은 위치의 resource가 없거나 둘 이상이면
애플리케이션 시작을 실패시킨다.

## 계약의 우선순위

`schema/`의 JSON Schema는 편집기 자동완성, 사람이 보는 문서 구조, 얕은 정적 검사를 위한
보조 계약이다. 실제 실행 계약의 기준은 다음 코드다.

1. policy 전용 strict JSON parser와 `schemaVersion`별 wire decoder
2. semantic compiler와 등록된 `PolicyDomainSchema`
3. compiled AST evaluator와 outcome merger
4. 도메인별 compiled contract validator

특히 현재 `policy-module.schema.json`은 `statements`의 원소가 object인지만 확인하며 statement,
condition, operand, outcome 내부 필드를 깊게 검증하지 않는다. 따라서 JSON Schema 검사 성공은
runtime compile 성공을 보장하지 않는다. Runtime decoder는 각 node의 허용 필드를 정확히
검사하고 unknown field를 거부한다.

## 디렉터리와 소유권

```text
policies/
├── README.md
├── schema/1.0/
│   ├── policy-bundle.schema.json
│   └── policy-module.schema.json
├── project/
│   ├── bundle.json
│   ├── project-resource.policy.json
│   ├── project-scope.policy.json
│   ├── application-resource.policy.json
│   ├── application-scope.policy.json
│   ├── statistics.policy.json
│   ├── matching-round.policy.json
│   ├── expected-differences.json
│   ├── generated/project-policy-artifacts.md
│   └── rollout/enforcement-receipts.json
└── form/
    └── form.policy.json
```

`form/form.policy.json`은 Form 도메인이 물리적으로 소유한다. 현재 pilot에서는 이 module이
`project-1.0` context와 `project-form:*` action을 사용하므로 Project bundle이 logical resource
`form.policy.json`으로 import한다. 물리적 소유 경로와 평가 context namespace는 서로 다른
개념이다. Form이 독립 context schema와 bundle을 갖게 되면 별도의 migration으로 분리해야 한다.

Project의 logical filename과 실제 classpath path 매핑은 production의
`ProjectPolicyResourceManifest` 한 곳에서만 선언한다. 예를 들면 bundle에 적는
`form.policy.json`은 compiler가 식별하는 logical filename이고 실제 패키지 경로는
`policies/form/form.policy.json`이다. Bundle JSON에는 디렉터리나 URL을 적지 않는다.

## Policy model

JSON 문서가 runtime decision이 되기까지 사용하는 model 관계는 다음과 같다.

```text
PolicyDomainSchema (도메인 context 계약)
├── ActionSchema
│   ├── requiredAttributes
│   ├── optionalAttributes
│   └── allowedOutcomes
├── AttributeSchema
│   ├── PolicyValueType
│   └── enumSymbols
└── OutcomeSchema
    ├── PolicyValueType
    ├── OutcomeMergeStrategy
    └── dominanceOrder

PolicyAttributeSet (한 번의 평가에 제공하는 immutable typed context)
└── PolicyAttributeKey<V> → PolicyValue

CompiledPolicyBundle
└── CompiledPolicyModule
    └── CompiledPolicyStatement
        ├── actions / effect
        ├── PolicyCondition
        │   ├── All
        │   ├── Any
        │   └── Predicate
        └── CompiledPolicyOutcome

PolicyEvaluationResult
├── PolicyDecision
└── PolicyEvaluationFailure
```

| Model | 책임 |
|---|---|
| `PolicyDomainSchema` | JSON policy가 사용할 수 있는 action, attribute, outcome의 전체 typed catalog |
| `ActionSchema` | action별 required/optional attribute와 방출 가능한 outcome 제한 |
| `AttributeSchema` | attribute 이름, `PolicyValueType`, 허용 enum symbol 정의 |
| `OutcomeSchema` | outcome 이름, 값 타입, merge strategy와 dominance 순서 정의 |
| `PolicyValue` | BOOLEAN, LONG, STRING, ENUM, INSTANT와 각 SET을 표현하는 sealed typed value |
| `PolicyAttributeSet` | null과 이름 중복을 허용하지 않는 evaluation context |
| `CompiledPolicyBundle` | strict parse와 semantic compile을 마친 canonical immutable 정책 |
| `CompiledPolicyModule` | bundle에 포함된 logical module과 그 statement 집합 |
| `CompiledPolicyStatement` | action, effect, compiled condition, compiled outcome으로 이루어진 실행 규칙 |
| `PolicyCondition` | operand 타입 검사를 마친 ALL/ANY/PREDICATE AST |
| `PolicyEvaluationResult` | 허용/거부 decision 또는 typed evaluation failure의 합 타입 |
| `PolicyDecision` | effect, matched statement ID, merged outcomes, 평가 시각과 정책 identity |
| `PolicyEvaluationFailure` | failure code와 관련 statement/attribute/outcome, 평가 시각과 정책 identity |

JSON이나 `JsonNode`, `Map<String, Object>`는 parse boundary 안에서만 사용한다. Compiler 이후에는
위 typed model만 evaluator에 전달한다.

## Bundle 문서

Bundle은 함께 compile하고 평가할 module의 envelope와 결합 규칙을 선언한다.

| 필드 | 의미 |
|---|---|
| `$schema` | 문서 종류와 policy language version을 나타내는 URN. V1 bundle은 `urn:umc:authorization:policy-bundle:1.0`이다. |
| `schemaVersion` | JSON wire 구조와 평가 의미의 decoder version. 문자열이며 현재 `1.0`만 지원한다. |
| `contextSchemaVersion` | 코드가 제공하는 action, attribute, outcome 계약의 version. Project pilot은 `project-1.0`이다. |
| `namespace` | bundle의 도메인 namespace. Project pilot은 `project`다. |
| `policyVersion` | 정책 내용의 SemVer 문자열이다. Compiler는 형식만 검증하며 현재 Project 정책은 `1.1.0`이다. |
| `defaultEffect` | 어떤 statement도 일치하지 않을 때의 결과. 현재 engine은 `DENY`만 허용한다. |
| `combiningAlgorithm` | 일치한 statement를 합성하는 방식. 현재 engine은 `DENY_OVERRIDES`만 지원한다. |
| `modules` | compile할 module reference 목록이다. 하나 이상이어야 한다. |

`modules[].id`는 module의 논리 ID이고 `modules[].resource`는 logical filename이다. V1 compiler는
resource를 정확히 `<id>.policy.json` 형식으로 제한한다. Module ID, resource, 제공된 module map은
중복될 수 없고, 선언했지만 제공하지 않았거나 선언하지 않은 module을 제공하면 compile이
실패한다.

## Module과 statement

Module은 bundle과 같은 envelope 필드와 다음 필드를 가진다.

| 필드 | 의미 |
|---|---|
| `moduleId` | bundle reference의 `id`와 정확히 같아야 하는 module ID |
| `statements` | 이 module의 authorization 규칙 배열 |

Bundle과 모든 module의 `schemaVersion`, `contextSchemaVersion`, `namespace`, `policyVersion`은
정확히 같아야 한다. Statement ID는 bundle 전체에서 유일해야 한다.

| Statement 필드 | 의미 |
|---|---|
| `id` | 관측과 decision에 노출되는 전역 고유 식별자 |
| `actions` | 이 규칙이 적용되는 등록 action 목록. 빈 배열과 중복 action은 허용하지 않는다. |
| `effect` | `ALLOW` 또는 `DENY` |
| `condition` | `ALL`, `ANY`, `PREDICATE` 중 하나인 조건 AST |
| `outcomes` | ALLOW가 일치할 때 방출할 typed 부가 결과. 결과가 없어도 빈 배열을 명시한다. |

DENY statement에는 outcome을 둘 수 없다. Action, condition attribute, outcome은 모두 해당
`PolicyDomainSchema`의 `ActionSchema`가 허용해야 한다.

## Condition AST

V1 condition node는 세 종류다.

```json
{ "type": "ALL", "conditions": [/* 모든 child가 true */] }
```

```json
{ "type": "ANY", "conditions": [/* 하나 이상의 child가 true */] }
```

```json
{
  "type": "PREDICATE",
  "operator": "EQ",
  "left": { "type": "ATTRIBUTE", "name": "subject.kind" },
  "right": { "type": "ENUM", "value": "MEMBER" }
}
```

`ALL.conditions`와 `ANY.conditions`는 비어 있을 수 없다. `EXISTS`, `NOT_EXISTS`는 `left`만
사용하는 단항 predicate이며, 그 외 operator는 `left`와 `right`를 모두 요구한다. V1에는
`NOT`, 함수 호출, 변수, wildcard, 정규식, 문자열 interpolation이 없다.

## Operand와 value type

`ATTRIBUTE`는 runtime context 값을 이름으로 참조한다. 나머지 9개 operand type은 literal이다.

| Type | JSON 값 필드 | 의미 |
|---|---|---|
| `BOOLEAN` | `value` | `true` 또는 `false` |
| `LONG` | `value` | signed 64-bit 범위의 JSON 정수 |
| `STRING` | `value` | 일반 문자열 |
| `ENUM` | `value` | context schema가 등록한 enum symbol |
| `INSTANT` | `value` | `Instant.parse`가 해석하는 ISO-8601 문자열 |
| `LONG_SET` | `values` | LONG 배열 |
| `STRING_SET` | `values` | STRING 배열 |
| `ENUM_SET` | `values` | ENUM 배열 |
| `INSTANT_SET` | `values` | INSTANT 배열 |

`ENUM`과 `STRING`은 서로 다른 타입이다. Set literal은 compile 시 중복을 제거하고 compiled
AST canonicalization 과정에서 안정적인 순서로 정규화된다. 암묵적인 scalar coercion은 없다.

## Operator signature

| Operator | Left | Right |
|---|---|---|
| `EQ`, `NEQ` | scalar | 같은 타입의 scalar |
| `IN` | BOOLEAN을 제외한 scalar | 같은 element type의 set |
| `CONTAINS` | set | 같은 타입의 scalar |
| `INTERSECTS` | set | 같은 타입의 set |
| `EXISTS`, `NOT_EXISTS` | ATTRIBUTE | 없음 |
| `LT`, `LTE`, `GT`, `GTE` | LONG 또는 INSTANT | 같은 타입의 scalar |

## Outcome과 merge

Outcome은 `key`와 typed operand인 `value`로 구성한다.

```json
{
  "key": "form.view",
  "value": { "type": "ENUM", "value": "FULL" }
}
```

Literal 대신 같은 타입의 ATTRIBUTE를 반환할 수도 있다. Outcome key, type, 허용 action,
merge strategy는 코드의 `OutcomeSchema`와 `ActionSchema`가 결정한다.

| Merge strategy | 동작 |
|---|---|
| `BOOLEAN_OR` | 일치한 값 중 하나라도 true이면 true |
| `SET_UNION` | 같은 타입 set을 정렬된 union으로 합성 |
| `DOMINANCE` | schema의 우선순위 배열에서 가장 앞선 symbol 선택 |
| `EXACTLY_ONE` | 모든 emission이 같은 값일 때만 성공하고 값이 다르면 evaluation failure |

DENY가 최종 결과이면 ALLOW outcome은 반환하지 않는다. Outcome이 참조하는 optional attribute가
runtime에 없으면 단순 불일치가 아니라 `OUTCOME_ATTRIBUTE_MISSING` evaluation failure다.

## Null, missing attribute와 evaluation

Policy JSON과 runtime `PolicyAttributeSet`에는 null을 넣지 않는다. Optional attribute는 null 대신
map에서 생략한다.

- 등록 action의 required attribute가 없으면 `REQUIRED_ATTRIBUTE_MISSING` failure다.
- Optional attribute가 없으면 `EXISTS`는 false, `NOT_EXISTS`는 true다.
- Optional attribute가 없고 다른 operator가 사용되면 그 predicate는 false다.
- 등록하지 않은 action이나 attribute, 타입이 다른 runtime attribute는 evaluation failure다.
- 등록 action이지만 일치한 statement가 없으면 bundle의 default DENY다.

Evaluator는 action의 모든 statement를 평가하고 statement 순서에 의존하지 않는다. 일치한 DENY가
하나라도 있으면 DENY, DENY 없이 ALLOW가 하나 이상이면 ALLOW, 둘 다 없으면 default DENY다.
Matched statement ID는 정렬되며 decision과 failure에는 평가 시각, 세 version, policy fingerprint가
포함된다.

## Version과 fingerprint

- `schemaVersion`은 policy JSON language 계약이다. Parser는 envelope에서 이 문자열을 먼저 읽어
  정확한 decoder를 선택하며 미지원 version을 거부한다.
- `contextSchemaVersion`은 action/attribute/outcome의 타입 계약이다. Policy와 등록된
  `PolicyDomainSchema`가 정확히 일치해야 한다.
- `policyVersion`은 정책 내용의 SemVer다. Compiler가 강제하는 것은 SemVer 형식과 bundle/module
  일치 여부다.

다음 policyVersion 운용 규칙은 **코드가 자동 강제하지 않는 권장 governance**다.

- 권한 확대나 계약 의미 변경: MAJOR
- 권한 축소나 기존 계약 안의 정책 교정: MINOR
- decision에 영향을 주지 않는 설명 변경: PATCH
- 정책 내용이 달라지면 review에서 version 변경 여부를 함께 확인

`policyFingerprint`는 version envelope와 정규화된 compiled AST를 deterministic JSON으로 직렬화한
뒤 계산한 SHA-256이다. Module, statement, action, outcome, set 순서와 JSON 공백에 영향받지 않는다.
따라서 이번처럼 physical classpath 위치만 바꾸고 logical filename과 정책 bytes/의미를 유지하면
fingerprint도 유지된다.

Generated artifact의 `Raw Policy Source SHA-256`은 각 logical resource의 원본 bytes를 해시하므로
공백이나 줄바꿈 변경도 탐지한다. Rollout receipt의 artifact SHA-256은 생성된 review artifact 파일
전체 bytes의 해시다. Compiled fingerprint, raw source SHA, generated artifact SHA는 목적이 다른
식별자이므로 서로 대체하지 않는다.

## Compile, load와 보안 경계

Project bundle은 Spring component 생성 시 한 번 compile되어 immutable 객체로 공유된다. Runtime
hot reload는 없다. 고정 manifest의 resource를 classpath에서 정확히 하나씩 읽고, 다음 pipeline을
통과하지 못하면 startup이 실패한다.

```text
고정 classpath load
→ strict JSON parse
→ schemaVersion decoder 선택
→ envelope와 구조 검증
→ context schema/type/action/outcome 검증
→ AST canonicalization
→ fingerprint 계산
→ 도메인 compiled contract 검증
→ immutable compiled bundle
```

Policy 전용 mapper는 공용 ObjectMapper와 분리되어 있으며 다음 입력을 거부한다.

- duplicate key, unknown field, JSON null, trailing content
- scalar coercion과 enum number
- floating-point literal
- Java/YAML comment, single quote, trailing comma, unquoted field name
- `NaN`/`Infinity` 같은 non-numeric number와 앞·뒤 decimal point 생략
- Jackson default typing

Classpath 경로는 production manifest에 상수로 고정한다. 외부 입력으로 경로를 조립하거나 `..`,
절대 경로, URL을 전달하는 API가 없다. 요청의 subject/resource attribute를 그대로 신뢰하지 않고
도메인 context builder가 내부 조회 결과로 typed fact를 만든다.

## Resource limit

| 대상 | 제한 |
|---|---:|
| bundle JSON과 모든 module JSON의 합계 | 1 MiB |
| bundle 전체 statement | 500 |
| statement당 action | 32 |
| condition depth | 16 |
| ALL/ANY child | 64 |
| statement당 condition node | 128 |
| set literal 원소 | 256 |
| identifier 길이 | 128 characters |
| string/enum/instant literal 길이 | 1,024 characters |

## Generated artifact, shadow 비교와 rollout

`project/generated/project-policy-artifacts.md`는 source policy와 runtime surface catalog에서 생성한
검토 산출물이다. 직접 수정하지 않는다. 다음 명령이 stale source, runtime catalog, package entry를
검증한다.

```bash
./gradlew verifyProjectPolicyArtifacts
./gradlew verifyPackagedProjectPolicyArtifacts
```

`expected-differences.json`은 SHADOW에서 legacy와 target 차이를 분류하는 exact context matrix다.
Target decision을 허용하거나 HTTP 응답을 변경하는 allowlist가 아니다. `rollout/enforcement-receipts.json`
은 승인된 artifact와 action wave를 묶는 startup admission 자료다. 실제 전환과 rollback 절차는
`docs/onboarding/project/project-authorization-rollout-runbook.md`를 따른다.

## 변경 절차

1. 대상 도메인의 context schema와 production resource manifest에서 소유권·logical filename·physical
   path를 확인한다.
2. Policy JSON과 필요한 `PolicyDomainSchema`를 함께 수정한다. 외부 경로나 동적 resource 주입을
   추가하지 않는다.
3. Bundle과 모든 module의 envelope version이 일치하는지 확인하고 권장 governance에 따라
   `policyVersion`을 결정한다.
4. Policy/compiler/context 단위 테스트를 RED에서 시작해 GREEN으로 만든다.
5. `./gradlew generateProjectPolicyArtifacts`로 review artifact를 생성하고 권한 확대·축소 및
   fingerprint/raw SHA 변경을 검토한다.
6. `./gradlew verifyProjectPolicyArtifacts verifyPackagedProjectPolicyArtifacts`로 source와 bootJar를
   검증한다.
7. 전체 test, Spotless, Checkstyle, Asciidoctor를 통과시킨다.
8. Target matrix와 artifact identity를 승인받은 뒤 rollout runbook의 SHADOW/ENFORCE wave를 따른다.

7~8번의 승인, SemVer 분류와 관찰 시간은 **운영 governance 권장 사항**이며 compiler 자체가 자동으로
수행하지 않는다.
