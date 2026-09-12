import { execFileSync } from "node:child_process";
import { lstatSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";

function hasExactKeys(value, keys) {
    return value !== null && typeof value === "object" && !Array.isArray(value)
        && Object.keys(value).sort().join(",") === [...keys].sort().join(",");
}

export function validatePreviewRelease(document) {
    if (!hasExactKeys(document, ["release"])
        || !hasExactKeys(document.release, ["prNumber", "headSha", "imageTag", "imageDigest"])) {
        throw new Error("Preview release schema has missing or unexpected fields");
    }
    const { prNumber, headSha, imageTag, imageDigest } = document.release;
    if (typeof prNumber !== "string" || !/^[1-9][0-9]*$/.test(prNumber)
        || !Number.isSafeInteger(Number(prNumber)) || prNumber !== String(Number(prNumber))) {
        throw new Error("PR number must be a positive canonical integer string");
    }
    if (typeof headSha !== "string" || headSha.length !== 40 || !/^[a-f0-9]{40}$/.test(headSha)) {
        throw new Error("PR head must be a full lowercase commit SHA");
    }
    if (typeof imageTag !== "string" || imageTag !== headSha.slice(0, 12)) {
        throw new Error("Image tag must match the first 12 characters of the PR head");
    }
    if (typeof imageDigest !== "string" || imageDigest.length !== 71 || !/^sha256:[a-f0-9]{64}$/.test(imageDigest)) {
        throw new Error("Image digest must be a lowercase sha256 digest");
    }
    return `argocd/preview-releases/pr-${prNumber}.json`;
}

function inspectPath(path, directory) {
    const stat = lstatSync(path, { throwIfNoEntry: false });
    if (stat && (stat.isSymbolicLink() || !(directory ? stat.isDirectory() : stat.isFile()))) {
        throw new Error("Preview release paths must be regular directories and files, not symlinks");
    }
    return stat;
}

export function assertOnlyPreviewReleaseChanged(repository, markerPath) {
    // untracked 파일도 포함한다. rename/copy/delete 및 대상 외 변경은 commit 전에 거부한다.
    const status = execFileSync("git", ["status", "--porcelain=v1", "-z", "--untracked-files=all"], {
        cwd: repository,
        encoding: "utf8",
    });
    for (const entry of status.split("\0").filter(Boolean)) {
        if (!["??", " M", "M ", "A "].includes(entry.slice(0, 2)) || entry.slice(3) !== markerPath) {
            throw new Error("Infrastructure has changes outside the selected Preview release");
        }
    }
}

export function updatePreviewRelease(repository, document) {
    const markerPath = validatePreviewRelease(document);
    const root = resolve(repository);
    if (!inspectPath(root, true) || !inspectPath(resolve(root, "argocd"), true)) {
        throw new Error("Expected an infrastructure checkout containing argocd");
    }
    const directory = resolve(root, "argocd/preview-releases");
    inspectPath(directory, true);
    const target = resolve(root, markerPath);
    const exists = inspectPath(target, false);
    assertOnlyPreviewReleaseChanged(root, markerPath);

    let previous = null;
    if (exists) {
        previous = readFileSync(target, "utf8");
        const previousPath = validatePreviewRelease(JSON.parse(previous));
        if (previousPath !== markerPath) {
            throw new Error("Existing release PR number does not match its filename");
        }
    }
    const updated = `${JSON.stringify(document, null, 2)}\n`;
    if (previous === updated) {
        return { markerPath, changed: false };
    }
    mkdirSync(directory, { recursive: true });
    writeFileSync(target, updated, "utf8");
    assertOnlyPreviewReleaseChanged(root, markerPath);
    return { markerPath, changed: true };
}

function main() {
    const [repository, prNumber, headSha, imageTag, imageDigest, ...extra] = process.argv.slice(2);
    if (!repository || extra.length) {
        throw new Error("usage: update-preview-release.mjs <infra-checkout> <pr-number> <head-sha> <tag> <digest>");
    }
    const result = updatePreviewRelease(repository, {
        release: { prNumber, headSha, imageTag, imageDigest },
    });
    console.log(`${result.markerPath}: ${result.changed ? "updated" : "unchanged"}`);
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    main();
}
