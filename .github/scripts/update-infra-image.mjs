import { readFileSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";

const TAG_PATTERN = /^[a-f0-9]{12}$/;
const DIGEST_PATTERN = /^sha256:[a-f0-9]{64}$/;

export function updateGitOpsValues(source, tag, digest) {
    if (!TAG_PATTERN.test(tag)) {
        throw new Error("image tag must be a 12-character lowercase commit SHA");
    }
    if (!DIGEST_PATTERN.test(digest)) {
        throw new Error("image digest must be sha256 followed by 64 lowercase hex characters");
    }

    // YAML 전체를 다시 직렬화하지 않고 허용된 필드만 수정해 사람이 작성한 형식과 주석을 보존한다.
    const lines = source.split("\n");
    let section = null;
    const blockCounts = { deployment: 0, image: 0, ingress: 0 };
    const enabledIndexes = { deployment: [], ingress: [] };
    const enabledValues = { deployment: null, ingress: null };
    const tagIndexes = [];
    const digestIndexes = [];
    let currentTag = null;
    let currentDigest = null;

    // 먼저 세 최상위 block의 위치와 현재 값을 수집한다. 이 단계에서는 파일을 바꾸지 않는다.
    for (let index = 0; index < lines.length; index += 1) {
        const line = lines[index];

        if (/^\S/.test(line)) {
            section = null;
        }

        if (line === "deployment:" || line === "image:" || line === "ingress:") {
            section = line.slice(0, -1);
            blockCounts[section] += 1;
            continue;
        }

        if ((section === "deployment" || section === "ingress") && /^  enabled:/.test(line)) {
            const value = line.slice(line.indexOf(":") + 1).trim();
            enabledIndexes[section].push(index);
            enabledValues[section] = value;
            continue;
        }

        if (section !== "image") {
            continue;
        }

        if (/^  tag:/.test(line)) {
            tagIndexes.push(index);
            currentTag = line.slice(line.indexOf(":") + 1).trim().replace(/^['"]|['"]$/g, "");
        } else if (/^  digest:/.test(line)) {
            digestIndexes.push(index);
            currentDigest = line.slice(line.indexOf(":") + 1).trim().replace(/^['"]|['"]$/g, "");
        }
    }

    // 구조가 예상과 조금이라도 다르면 잘못된 위치를 수정하지 않고 즉시 중단한다.
    if (
        blockCounts.deployment !== 1 ||
        blockCounts.image !== 1 ||
        blockCounts.ingress !== 1 ||
        enabledIndexes.deployment.length !== 1 ||
        enabledIndexes.ingress.length !== 1 ||
        tagIndexes.length !== 1 ||
        digestIndexes.length !== 1
    ) {
        throw new Error(
            "expected exactly one deployment.enabled, image tag/digest, and ingress.enabled field",
        );
    }

    if (
        !["true", "false"].includes(enabledValues.deployment) ||
        !["true", "false"].includes(enabledValues.ingress)
    ) {
        throw new Error("deployment.enabled and ingress.enabled must be boolean YAML scalars");
    }

    // bootstrap-required는 아직 최초 이미지가 없다는 잠금 표시다.
    // 그 이후에는 commit SHA tag와 registry digest가 모두 있어야 정상 상태로 본다.
    const bootstrapImage = currentTag === "bootstrap-required" && currentDigest === "";
    const immutableImage = TAG_PATTERN.test(currentTag) && DIGEST_PATTERN.test(currentDigest);
    if (!bootstrapImage && !immutableImage) {
        throw new Error("current image must be either the bootstrap placeholder or an immutable reference");
    }
    if (bootstrapImage && (enabledValues.deployment !== "false" || enabledValues.ingress !== "false")) {
        throw new Error("bootstrap image requires closed Deployment and Ingress gates");
    }
    if (enabledValues.deployment === "false" && enabledValues.ingress === "true") {
        throw new Error("Ingress gate cannot be open while the Deployment gate is closed");
    }

    lines[tagIndexes[0]] = `  tag: "${tag}"`;
    lines[digestIndexes[0]] = `  digest: "${digest}"`;
    if (bootstrapImage) {
        // 최초 배포에서만 이미지와 두 gate를 한 커밋으로 연다.
        // 이후 운영자가 점검을 위해 gate를 닫았다면 다음 이미지 발행에서도 그 상태를 보존한다.
        lines[enabledIndexes.deployment[0]] = "  enabled: true";
        lines[enabledIndexes.ingress[0]] = "  enabled: true";
    }

    return lines.join("\n");
}

function main() {
    const [valuesPath, tag, digest] = process.argv.slice(2);
    if (!valuesPath || !tag || !digest) {
        throw new Error("usage: update-infra-image.mjs <values-file> <tag> <digest>");
    }

    // 잘못된 인자로 다른 환경이나 공통 values를 수정하지 못하게 대상 파일을 제한한다.
    if (!/^values-(prod|dev)\.yaml$/.test(valuesPath.split("/").at(-1))) {
        throw new Error("only values-prod.yaml or values-dev.yaml may be updated");
    }

    const source = readFileSync(valuesPath, "utf8");
    const updated = updateGitOpsValues(source, tag, digest);
    writeFileSync(valuesPath, updated, "utf8");
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    main();
}
