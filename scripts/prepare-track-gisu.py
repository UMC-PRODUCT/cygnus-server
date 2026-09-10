#!/usr/bin/env python3
"""기수·지부·Track 커리큘럼을 관리 API로 등록한다. 기본 실행은 요청 미리보기다."""

import argparse
import fcntl
import hashlib
import json
import os
from pathlib import Path
import sys
from urllib.error import HTTPError, URLError
from urllib.parse import urlparse
from urllib.request import HTTPRedirectHandler, Request, build_opener


BASIC_TRACKS = {"PLAN", "DESIGN", "WEB_PRODUCT_ENGINEER", "MOBILE_PRODUCT_ENGINEER"}


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, request, response, code, message, headers, new_url):
        raise HTTPError(request.full_url, code, "등록 API 리다이렉트는 지원하지 않습니다.", headers, response)


def build_operations(config):
    operations = []

    def add(key, path, body, result_field=None):
        operations.append({"key": key, "path": path, "body": body, "resultField": result_field})

    gisu = dict(config["gisu"])
    if gisu.get("learningType", "TRACK") != "TRACK":
        raise ValueError("이 스크립트는 TRACK 기수를 준비합니다.")
    gisu["learningType"] = "TRACK"
    add("gisu", "/api/v1/gisu", gisu)
    schools = set()
    for i, chapter in enumerate(config.get("chapters", [])):
        school_ids = chapter.get("schoolIds", [])
        if schools.intersection(school_ids) or len(school_ids) != len(set(school_ids)):
            raise ValueError("학교는 한 지부에만 배정할 수 있습니다.")
        schools.update(school_ids)
        add(f"chapter:{i}", "/api/v1/chapters", {**chapter, "gisuId": {"ref": "gisu"}})

    tracks = set()
    for curriculum in config["curricula"]:
        track = curriculum["track"]
        if track not in BASIC_TRACKS or track in tracks:
            raise ValueError("기본 Track마다 커리큘럼을 하나만 등록할 수 있습니다.")
        tracks.add(track)
        curriculum_key = f"curriculum:{track}"
        add(curriculum_key, "/api/v2/curriculums", {
            "gisuId": {"ref": "gisu"}, "track": track, "title": curriculum["title"]
        })
        weeks = set()
        for week in curriculum["weeks"]:
            week_identity = (week["weekNo"], week.get("isExtra", False))
            if week_identity in weeks:
                raise ValueError(f"{track}에 중복된 주차·부록 구분이 있습니다.")
            weeks.add(week_identity)
            week_key = f"{curriculum_key}:week:{week_identity[0]}:{week_identity[1]}"
            add(week_key, "/api/v2/curriculums/weekly", {
                "curriculumId": {"ref": curriculum_key},
                **{key: week[key] for key in ("weekNo", "title", "startsAt", "endsAt")},
                "isExtra": week_identity[1]
            })
            for j, workbook in enumerate(week["workbooks"]):
                workbook_key = f"{week_key}:workbook:{j}"
                add(workbook_key, "/api/v2/curriculums/original-workbooks/draft", {
                    **{key: value for key, value in workbook.items() if key != "missions"},
                    "weeklyCurriculumId": {"ref": week_key}
                })
                for k, mission in enumerate(workbook.get("missions", [])):
                    add(f"{workbook_key}:mission:{k}",
                        "/api/v2/curriculums/original-workbooks/missions", {
                            **mission, "originalWorkbookId": {"ref": workbook_key}
                        }, "originalWorkbookMissionId")
    if not tracks:
        raise ValueError("등록할 커리큘럼을 하나 이상 입력하세요.")
    return operations


def resolve(value, completed):
    if isinstance(value, dict):
        if set(value) == {"ref"}:
            return completed[value["ref"]]
        return {key: resolve(item, completed) for key, item in value.items()}
    if isinstance(value, list):
        return [resolve(item, completed) for item in value]
    return value


def save_state(path, state):
    temporary = path.with_name(path.name + ".tmp")
    with temporary.open("w", encoding="utf-8") as output:
        json.dump(state, output, ensure_ascii=False, indent=2)
        output.flush()
        os.fsync(output.fileno())
    temporary.replace(path)


def apply_operations(base_url, operations, state_path, token):
    fingerprint = hashlib.sha256(json.dumps(
        {"baseUrl": base_url, "operations": operations}, sort_keys=True, ensure_ascii=False
    ).encode()).hexdigest()
    lock_path = state_path.with_name(state_path.name + ".lock")
    with lock_path.open("a") as lock:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        state = json.loads(state_path.read_text()) if state_path.exists() else {
            "fingerprint": fingerprint, "completed": {}, "pending": None
        }
        if state["fingerprint"] != fingerprint:
            raise ValueError("입력 또는 서버 주소가 이전 실행과 다릅니다. 결과 파일을 확인하세요.")
        if state["pending"] is not None:
            raise ValueError(
                f"이전 요청의 성공 여부를 먼저 확인하세요: {state['pending']}. "
                "서버에 생성되었다면 completed에 ID를 기록한 뒤 pending을 null로 변경하세요. "
                "생성되지 않은 것이 확인된 경우에만 pending을 null로 변경하고 재실행하세요."
            )
        for operation in operations:
            key = operation["key"]
            if key in state["completed"]:
                continue
            body = resolve(operation["body"], state["completed"])
            request = Request(base_url + operation["path"], method="POST",
                              data=json.dumps(body).encode(), headers={
                                  "Authorization": f"Bearer {token}",
                                  "Content-Type": "application/json"
                              })
            state["pending"] = key
            save_state(state_path, state)
            with build_opener(NoRedirect).open(request, timeout=30) as response:
                payload = json.load(response)
            result = payload.get("result")
            if operation["resultField"]:
                result = result.get(operation["resultField"]) if isinstance(result, dict) else None
            if isinstance(result, str) and result.isascii() and result.isdigit():
                result = int(result)
            if payload.get("success") is not True or type(result) is not int or result <= 0:
                raise ValueError(f"{key} 요청의 생성 ID를 확인할 수 없습니다. 결과 파일을 확인하세요.")
            state["completed"][key] = result
            state["pending"] = None
            save_state(state_path, state)
            print(f"등록: {key} → {result}")
    print(f"준비 완료: {state_path} (기수 비활성, 워크북 DRAFT)")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", type=Path, help="기수·커리큘럼 입력 JSON")
    parser.add_argument("--base-url", help="관리 API 서버 주소")
    parser.add_argument("--state", type=Path, help="생성 ID와 실행 상태를 저장할 파일")
    parser.add_argument("--apply", action="store_true", help="미리보기 대신 실제 등록")
    args = parser.parse_args()
    try:
        operations = build_operations(json.loads(args.input.read_text(encoding="utf-8")))
        if not args.apply:
            print(json.dumps(operations, ensure_ascii=False, indent=2))
            return 0
        if not args.base_url or not args.state:
            raise ValueError("등록 시 --base-url과 --state가 필요합니다.")
        base_url = args.base_url.rstrip("/")
        url = urlparse(base_url)
        if (url.scheme not in {"http", "https"} or not url.netloc or url.username
                or url.password or url.query or url.fragment or url.path):
            raise ValueError("서버 주소는 경로·계정정보 없는 http(s) origin을 입력하세요.")
        if url.scheme == "http" and url.hostname not in {"localhost", "127.0.0.1", "::1"}:
            raise ValueError("로컬 서버 이외에는 HTTPS 주소를 사용하세요.")
        token = os.environ.get("UMC_API_TOKEN")
        if not token:
            raise ValueError("UMC_API_TOKEN 환경변수에 관리 권한 JWT를 설정하세요.")
        apply_operations(base_url, operations, args.state, token)
        return 0
    except HTTPError as error:
        print(f"등록 중단: HTTP {error.code}. 결과 파일의 pending 요청을 확인하세요.", file=sys.stderr)
    except (OSError, ValueError, KeyError, TypeError, URLError) as error:
        print(f"등록 중단: {error}", file=sys.stderr)
    return 1


if __name__ == "__main__":
    sys.exit(main())
