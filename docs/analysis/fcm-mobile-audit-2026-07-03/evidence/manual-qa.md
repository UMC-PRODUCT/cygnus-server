# FCM Mobile Audit Manual QA Matrix

상태: passed

읽기 전용 검증만 수행했다. 서버, tmux, browser, container는 실행하지 않았다.

| scenario id | criterion | surface | invocation | verdict | artifacts |
|---|---|---|---|---|---|
| C001 | 모바일 payload/계약 | CLI/code evidence | `wc -l`, `rg`, `sed -n`, `tail`로 C001 증거와 보고서 확인 | PASS | `.omo/ulw-loop/fcm-mobile-audit-20260701/evidence/C001-mobile-payload.txt`, `.omo/ulw-loop/fcm-mobile-audit-20260701/evidence/fcm-mobile-audit-report.md` |
| C002 | 보안/남용 리스크 | CLI/code evidence | `wc -l`, `rg`, `sed -n`, `tail`로 C002 증거와 보고서 확인 | PASS | `.omo/ulw-loop/fcm-mobile-audit-20260701/evidence/C002-security.txt`, `.omo/ulw-loop/fcm-mobile-audit-20260701/evidence/fcm-mobile-audit-report.md` |
| C003 | 확장성/운영성 | CLI/code evidence | `wc -l`, `rg`, `sed -n`, `tail`로 C003 증거와 보고서 확인 | PASS | `.omo/ulw-loop/fcm-mobile-audit-20260701/evidence/C003-extensibility.txt`, `.omo/ulw-loop/fcm-mobile-audit-20260701/evidence/fcm-mobile-audit-report.md` |

## Adversarial Checks

| scenario id | class | expected behavior | verdict |
|---|---|---|---|
| A001 | 증거 없는 모바일 결론 | notification/data/deepLink/image 및 플랫폼 옵션 부재가 파일:라인과 Firebase 공식 문서 근거로 연결된다. | PASS |
| A002 | 증거 없는 보안 주장 | 관리자 API, token registration abuse, data/deepLink 검증, token 저장/검증 리스크가 파일:라인 근거로 연결된다. | PASS |
| A003 | 추론만 있는 확장성 평가 | event model, audience resolver, outbox config, batching, port shape 근거와 확장성 결론이 연결된다. | PASS |

cleanup: no runtime resources spawned; no server, tmux, browser, or container left running.
