#!/usr/bin/env python3
"""tracks·역할을 함께 지원하는 새 서버 버전의 배포를 확인한 뒤 실행한다.

확정된 비공개 요청 JSON으로 코드를 발급하고, 응답이 불확실하면 재요청을 중단한다.
"""

import argparse
import hashlib
import json
import os
from pathlib import Path
import tempfile
from urllib.error import HTTPError
from urllib.request import HTTPRedirectHandler, Request, build_opener


API_BASES = {
    "dev": "https://api-dev.university.neordinary.com",
    "prod": "https://api.university.neordinary.com",
}


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def save_private(path, value):
    path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    path.parent.chmod(0o700)
    descriptor, temporary = tempfile.mkstemp(prefix=".code-results-", dir=path.parent)
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8") as stream:
            json.dump(value, stream, ensure_ascii=False, indent=2)
            stream.write("\n")
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(temporary, path)
        path.chmod(0o600)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def canonical_request_tracks(body):
    tracks, track = body.get("tracks"), body.get("track")
    if tracks is not None and track is not None:
        raise ValueError("track과 tracks를 동시에 지정할 수 없습니다.")
    selected = tracks if tracks is not None else ([] if track is None else [track])
    if not isinstance(selected, list) or any(not isinstance(value, str) for value in selected):
        raise ValueError("수강 Track은 문자열 목록이어야 합니다.")
    return list(dict.fromkeys(selected))


def load_requests(path):
    content = path.read_bytes()
    data = json.loads(content)
    environment = data.get("environment")
    if environment not in API_BASES or data.get("apiBase") != API_BASES[environment]:
        raise ValueError("요청 파일의 환경과 API 주소가 일치하지 않습니다.")
    if data.get("readyForIssue") is not True:
        raise ValueError("Track·직책·중복 처리와 조직 ID가 확정된 요청 파일이 필요합니다.")
    records = data.get("records")
    if not isinstance(records, list) or not records:
        raise ValueError("발급 요청이 없습니다.")
    keys = set()
    for record in records:
        key = record.get("candidateKey")
        body = record.get("request")
        if not isinstance(key, str) or not key or key in keys:
            raise ValueError("발급 대상 키가 없거나 중복되었습니다.")
        keys.add(key)
        if not isinstance(body, dict) or "code" in body:
            raise ValueError("코드는 서버가 생성해야 합니다.")
        if not isinstance(body.get("memberName"), str) or not body["memberName"].strip():
            raise ValueError("회원 이름이 비어 있습니다.")
        if body.get("gisuId") != data.get("gisuId") or not isinstance(body.get("gisuId"), int):
            raise ValueError("요청의 기수 ID가 환경 설정과 다릅니다.")
        canonical_request_tracks(body)
    return data, hashlib.sha256(content).hexdigest()


def run(args):
    os.umask(0o077)
    input_path = args.input.resolve()
    data, digest = load_requests(input_path)
    environment = data["environment"]
    journal_path = (args.journal or input_path.with_name(f"{environment}-issuance-results.private.json")).resolve()
    repository = Path(__file__).resolve().parents[1]
    if input_path.is_relative_to(repository) or journal_path.is_relative_to(repository):
        raise ValueError("이름·코드 파일은 Git 저장소 밖의 비공개 폴더에 저장해주세요.")
    if input_path == journal_path:
        raise ValueError("입력 파일과 결과 파일은 서로 다른 경로여야 합니다.")
    if not args.execute:
        print(json.dumps({"environment": environment, "requests": len(data["records"]), "executed": False}))
        return
    token = os.environ.get(args.token_env)
    if not token:
        raise ValueError("지정한 환경변수에 관리자 Bearer 토큰이 없습니다.")
    journal_path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    journal_path.parent.chmod(0o700)
    lock = journal_path.with_suffix(journal_path.suffix + ".lock")
    descriptor = os.open(lock, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    os.close(descriptor)
    try:
        if journal_path.exists():
            journal = json.loads(journal_path.read_text(encoding="utf-8"))
            if journal.get("inputSha256") != digest or journal.get("environment") != environment:
                raise ValueError("기존 발급 결과와 입력 파일이 다릅니다. 기존 결과를 먼저 대조해주세요.")
        else:
            journal = {"environment": environment, "apiBase": data["apiBase"], "inputSha256": digest, "records": {}}
        expected_records = {record["candidateKey"]: record for record in data["records"]}
        for key, result in journal["records"].items():
            if key not in expected_records or result.get("sourceRows") != expected_records[key].get("sourceRows", []):
                raise ValueError("결과 파일의 발급 대상이 입력과 다릅니다.")
            if result["status"] == "ISSUED" and (not isinstance(result.get("code"), str) or len(result["code"]) != 6):
                raise ValueError("기존 발급 결과에 서버 코드가 없습니다.")
        for result in journal["records"].values():
            if result["status"] == "IN_FLIGHT":
                result["status"] = "UNKNOWN"
        save_private(journal_path, journal)
        if any(value["status"] != "ISSUED" for value in journal["records"].values()):
            raise ValueError("미확정·거절 결과가 있습니다. 서버 기록과 대조하기 전에는 재요청하지 않습니다.")
        opener = build_opener(NoRedirect())
        for record in data["records"]:
            key = record["candidateKey"]
            if key in journal["records"]:
                continue
            journal["records"][key] = {"sourceRows": record.get("sourceRows", []), "status": "IN_FLIGHT", "recordId": None, "code": None}
            save_private(journal_path, journal)
            result = journal["records"][key]
            try:
                request = Request(
                    data["apiBase"] + "/api/v1/challenger-record",
                    data=json.dumps(record["request"], ensure_ascii=False).encode("utf-8"),
                    headers={"Authorization": "Bearer " + token, "Content-Type": "application/json", "Accept": "application/json"},
                    method="POST",
                )
                with opener.open(request, timeout=30) as response:
                    envelope = json.load(response)
                payload = envelope.get("result")
                if envelope.get("success") is not True or not isinstance(payload, dict):
                    raise ValueError("서버 발급 결과를 확인할 수 없습니다.")
                record_id, code = payload.get("id"), payload.get("code")
                if not isinstance(code, str) or len(code) != 6:
                    raise ValueError("서버의 6자리 코드를 확인할 수 없습니다.")
                if record_id is not None and (not isinstance(record_id, int) or record_id <= 0):
                    raise ValueError("서버의 발급 ID를 확인할 수 없습니다.")
                if any(payload.get(field) != record["request"].get(field) for field in [
                    "gisuId", "schoolId", "memberName", "challengerRoleType", "chapterId", "part"
                ]):
                    raise ValueError("서버 응답이 요청한 발급 대상과 다릅니다.")
                if payload.get("tracks") != canonical_request_tracks(record["request"]):
                    raise ValueError("서버 응답의 수강 Track이 요청과 다릅니다.")
                result.update(status="ISSUED", recordId=record_id, code=code)
            except HTTPError as error:
                result.update(status="REJECTED" if 400 <= error.code < 500 else "UNKNOWN", httpStatus=error.code)
            except (Exception, KeyboardInterrupt) as error:
                result.update(status="UNKNOWN", errorType=type(error).__name__)
            save_private(journal_path, journal)
            if result["status"] != "ISSUED":
                raise ValueError("요청 결과를 비공개 결과 파일에 기록했습니다. 자동 재시도 없이 중단합니다.")
            if len(journal["records"]) % 25 == 0:
                print(json.dumps({"environment": environment, "issued": len(journal["records"])}), flush=True)
        print(json.dumps({"environment": environment, "issued": len(journal["records"]), "resultPath": str(journal_path)}))
    finally:
        lock.unlink()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, required=True, help="환경·조직 ID·매핑이 확정된 비공개 요청 JSON")
    parser.add_argument("--journal", type=Path, help="결과 JSON 경로. 기본값: 입력 폴더의 dev/prod-issuance-results.private.json")
    parser.add_argument("--token-env", default="UMC_CODE_API_TOKEN", help="관리자 Bearer 토큰을 읽을 환경변수 이름")
    parser.add_argument("--execute", action="store_true", help="새 서버 버전 배포 확인 후 발급. 생략하면 외부 요청 없이 입력만 검증")
    args = parser.parse_args()
    try:
        run(args)
    except (OSError, ValueError, KeyError, TypeError):
        parser.exit(1, "발급을 중단했습니다. 입력·환경·잠금 및 비공개 결과 파일의 상태를 확인해주세요. 자동 재시도하지 않았습니다.\n")


if __name__ == "__main__":
    main()
