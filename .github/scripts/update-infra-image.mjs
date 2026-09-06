import { readFileSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";

const TAG_PATTERN = /^[a-f0-9]{12}$/;
const DIGEST_PATTERN = /^sha256:[a-f0-9]{64}$/;

export function updateImageValues(source, tag, digest) {
    if (!TAG_PATTERN.test(tag)) {
        throw new Error("image tag must be a 12-character lowercase commit SHA");
    }
    if (!DIGEST_PATTERN.test(digest)) {
        throw new Error("image digest must be sha256 followed by 64 lowercase hex characters");
    }

    const lines = source.split("\n");
    let insideImage = false;
    let imageBlocks = 0;
    let tagUpdates = 0;
    let digestUpdates = 0;

    for (let index = 0; index < lines.length; index += 1) {
        const line = lines[index];

        if (line === "image:") {
            insideImage = true;
            imageBlocks += 1;
            continue;
        }

        if (insideImage && /^\S/.test(line)) {
            insideImage = false;
        }

        if (!insideImage) {
            continue;
        }

        if (/^  tag:/.test(line)) {
            lines[index] = `  tag: "${tag}"`;
            tagUpdates += 1;
        } else if (/^  digest:/.test(line)) {
            lines[index] = `  digest: "${digest}"`;
            digestUpdates += 1;
        }
    }

    if (imageBlocks !== 1 || tagUpdates !== 1 || digestUpdates !== 1) {
        throw new Error(
            `expected one image block with one tag and digest, got blocks=${imageBlocks}, tags=${tagUpdates}, digests=${digestUpdates}`,
        );
    }

    return lines.join("\n");
}

function main() {
    const [valuesPath, tag, digest] = process.argv.slice(2);
    if (!valuesPath || !tag || !digest) {
        throw new Error("usage: update-infra-image.mjs <values-file> <tag> <digest>");
    }

    if (!/^values-(prod|dev)\.yaml$/.test(valuesPath.split("/").at(-1))) {
        throw new Error("only values-prod.yaml or values-dev.yaml may be updated");
    }

    const source = readFileSync(valuesPath, "utf8");
    const updated = updateImageValues(source, tag, digest);
    writeFileSync(valuesPath, updated, "utf8");
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    main();
}
