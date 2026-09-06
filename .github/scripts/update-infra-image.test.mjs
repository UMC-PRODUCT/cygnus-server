import assert from "node:assert/strict";
import test from "node:test";

import { updateImageValues } from "./update-infra-image.mjs";

const digest = `sha256:${"a".repeat(64)}`;

test("updates only the image tag and digest", () => {
    const source = `deployment:
  enabled: false

image:
  tag: bootstrap-required
  digest: ""

ingress:
  enabled: false
`;

    const updated = updateImageValues(source, "0123456789ab", digest);

    assert.match(updated, /tag: "0123456789ab"/);
    assert.match(updated, new RegExp(`digest: "${digest}"`));
    assert.match(updated, /deployment:\n  enabled: false/);
    assert.match(updated, /ingress:\n  enabled: false/);
});

test("rejects a mutable or shortened image tag", () => {
    assert.throws(
        () => updateImageValues("image:\n  tag: old\n  digest: old\n", "latest", digest),
        /12-character lowercase commit SHA/,
    );
});

test("rejects a malformed digest", () => {
    assert.throws(
        () => updateImageValues("image:\n  tag: old\n  digest: old\n", "0123456789ab", "sha256:bad"),
        /64 lowercase hex characters/,
    );
});

test("fails closed when the image block contract changes", () => {
    assert.throws(
        () => updateImageValues("image:\n  tag: old\n", "0123456789ab", digest),
        /expected one image block/,
    );
});
