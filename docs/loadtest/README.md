# docs/loadtest — 부하 테스트 실행 기록

숫자는 리그와 함께 사라진다. **기록이 부하 테스트의 실제 산출물**이다.

## 규칙

1. **실행 전에** 기록 문서를 만든다 — 가설부터 적는다. 하네스에 따라:
   - k6 하네스: `loadtest/scripts/new-run.sh <이름>` → `runs/<날짜>-<이름>/summary.md` 스캐폴드. 날짜·커밋은 자동으로 채우고 이미지·자원 스펙·실행 명령은 직접 기입한다.
   - notice 전/후 비교 하네스: `loadtest/run.sh` 가 실행 종료 시 `runs/<YYYY-MM-DD-HHmmss>-<label>/` 에 `summary.md`·`summary.json`·`run-meta.txt` 를 자동 생성한다. (디렉터리명에 시각 포함 → 같은 날 같은 label 재실행에도 덮어쓰지 않음)
2. **실행 후** 결과 표·Grafana 스크린샷(포화점 뷰 필수)·관찰·결론을 채우고, 맨 위 한 줄 요약을 쓴다. 자동 생성된 `summary.md` 도 가설·시드 칸은 수기 보완.
3. 좋은 기록의 기준은 하나다: **다른 사람이 이 문서만 보고 같은 실험을 재현하고, 결론에 동의할 수 있는가.**

## 구조

- `diagnosis.md` — 메트릭 읽는 법 (증상 조합 매트릭스·커넥션 풀·시나리오 설계·혼합 테스트)
- `runs/_template/` — summary.md 템플릿 (new-run.sh 가 사용)
- `runs/<날짜>-<이름>/` — 실행 단위 기록. 스크린샷도 같은 디렉터리에 둔다.

실행 방법은 [loadtest/README.md](../../loadtest/README.md), 전략 배경은 `docs/adr/013` 참조.

## 실행 목록

| 실행 | 도메인 | 시나리오 | 목적 |
|------|--------|----------|------|
| _(아직 없음)_ | notice | read-status load | P0 리팩토링 전/후 비교 예정 |
