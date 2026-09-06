import assert from "node:assert/strict";
import test from "node:test";

import { buildDiscordDeployPayload } from "./discord-deploy-notify.mjs";

test("Discord deployment payload maps deployment states", () => {
    const payload = buildDiscordDeployPayload({
        workflow: "CD [ASG]",
        status: "failure",
        environment: "Production",
        branch: "main",
        sha: "abcdef1234567890",
        imageTag: "production-abcdef1",
        target: "ASG",
        runUrl: "https://github.com/UMC-PRODUCT/umc-product-server/actions/runs/1",
    });

    assert.equal(payload.embeds[0].color, 0xd73a49);
    assert.equal(payload.embeds[0].fields.some((field) => field.name === "Status" && field.value === "failure"), true);
});
