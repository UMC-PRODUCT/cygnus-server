import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { mkdtempSync, mkdirSync, readFileSync, rmSync, symlinkSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";
import { assertOnlyPreviewReleaseChanged, updatePreviewRelease, validatePreviewRelease } from "./update-preview-release.mjs";

const headSha = `123456789012${"a".repeat(28)}`;
const release = () => ({
    release: { prNumber: "42", headSha, imageTag: headSha.slice(0, 12), imageDigest: `sha256:${"a".repeat(64)}` },
});

function repository(t) {
    const root = mkdtempSync(join(tmpdir(), "umc-preview-release-test-"));
    t.after(() => rmSync(root, { recursive: true, force: true }));
    mkdirSync(join(root, "argocd"));
    writeFileSync(join(root, "argocd/.gitkeep"), "");
    execFileSync("git", ["init", "--quiet"], { cwd: root });
    execFileSync("git", ["add", "argocd/.gitkeep"], { cwd: root });
    execFileSync("git", ["-c", "user.name=Test", "-c", "user.email=test@example.invalid", "commit", "--quiet", "-m", "initial"], { cwd: root });
    return root;
}

test("성공 이미지 marker만 생성하고 같은 입력을 재실행하면 변경하지 않는다", (t) => {
    const root = repository(t);
    const document = release();
    const first = updatePreviewRelease(root, document);
    assert.deepEqual(first, { markerPath: "argocd/preview-releases/pr-42.json", changed: true });
    assert.deepEqual(JSON.parse(readFileSync(join(root, first.markerPath), "utf8")), document);
    assert.equal(typeof document.release.imageTag, "string");
    assert.deepEqual(updatePreviewRelease(root, document), { ...first, changed: false });
});

test("후속 성공 이미지의 SHA와 digest를 같은 PR marker에만 갱신한다", (t) => {
    const root = repository(t);
    const first = updatePreviewRelease(root, release());
    execFileSync("git", ["add", first.markerPath], { cwd: root });
    execFileSync("git", ["-c", "user.name=Test", "-c", "user.email=test@example.invalid", "commit", "--quiet", "-m", "release"], { cwd: root });
    const next = release();
    next.release.headSha = "b".repeat(40);
    next.release.imageTag = "b".repeat(12);
    next.release.imageDigest = `sha256:${"c".repeat(64)}`;
    assert.equal(updatePreviewRelease(root, next).changed, true);
    assert.deepEqual(JSON.parse(readFileSync(join(root, first.markerPath), "utf8")), next);
    assertOnlyPreviewReleaseChanged(root, first.markerPath);
});

test("변형된 PR 번호·SHA·tag·digest와 추가 필드는 거부한다", () => {
    for (const prNumber of [42, "0", "-1", "042", "../prod", "42\n", "9007199254740992"]) {
        assert.throws(() => validatePreviewRelease({ release: { ...release().release, prNumber } }));
    }
    for (const patch of [
        { headSha: "a".repeat(39) }, { headSha: "A".repeat(40) },
        { headSha: `${headSha}\n` },
        { imageTag: 123456789012 }, { imageTag: "b".repeat(12) },
        { imageDigest: "sha256:bad" }, { imageDigest: `sha256:${"A".repeat(64)}` },
        { imageDigest: `sha256:${"a".repeat(64)}\n` },
        { file: "values-prod.yaml" },
    ]) {
        assert.throws(() => validatePreviewRelease({ release: { ...release().release, ...patch } }));
    }
    assert.throws(() => validatePreviewRelease({ ...release(), extra: true }));
    assert.throws(() => validatePreviewRelease({ release: null }));
    assert.throws(() => validatePreviewRelease({ release: [] }));
    const missing = release();
    delete missing.release.imageDigest;
    assert.throws(() => validatePreviewRelease(missing));
});

test("다른 파일이 변경되어 있으면 marker를 쓰기 전에 중단한다", (t) => {
    const root = repository(t);
    writeFileSync(join(root, "unexpected.txt"), "do not commit");
    assert.throws(() => updatePreviewRelease(root, release()), /outside the selected Preview release/);
    assert.throws(() => readFileSync(join(root, "argocd/preview-releases/pr-42.json")), { code: "ENOENT" });
});

test("기존 marker의 PR 번호가 파일명과 다르거나 추가 필드가 있으면 덮어쓰지 않는다", (t) => {
    const root = repository(t);
    const { markerPath } = updatePreviewRelease(root, release());
    for (const document of [
        { release: { ...release().release, prNumber: "43" } },
        { ...release(), unexpected: "field" },
    ]) {
        const content = JSON.stringify(document);
        writeFileSync(join(root, markerPath), content);
        assert.throws(() => updatePreviewRelease(root, release()));
        assert.equal(readFileSync(join(root, markerPath), "utf8"), content);
    }
});

test("marker 파일이나 상위 디렉터리가 symlink이면 따라가지 않는다", (t) => {
    for (const directoryLink of [false, true]) {
        const root = repository(t);
        const outside = mkdtempSync(join(tmpdir(), "umc-preview-outside-test-"));
        t.after(() => rmSync(outside, { recursive: true, force: true }));
        const parent = join(root, "argocd/preview-releases");
        if (directoryLink) {
            symlinkSync(outside, parent);
        } else {
            mkdirSync(parent);
            const externalFile = join(outside, "target.json");
            writeFileSync(externalFile, JSON.stringify(release()));
            symlinkSync(externalFile, join(parent, "pr-42.json"));
        }
        assert.throws(() => updatePreviewRelease(root, release()), /not symlinks/);
    }
});
