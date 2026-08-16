import { pathToFileURL } from "node:url";

const COLORS = {
    success: 0x2ea44f,
    failure: 0xd73a49,
    cancelled: 0x6a737d,
    skipped: 0xdbab09,
};

function shortSha(sha) {
    return String(sha ?? "").slice(0, 7);
}

function normalizeStatus(status) {
    const normalized = String(status ?? "").toLowerCase();
    if (["success", "failure", "cancelled", "skipped"].includes(normalized)) {
        return normalized;
    }
    return normalized || "unknown";
}

function statusIcon(status) {
    return {
        success: "OK",
        failure: "FAIL",
        cancelled: "CANCELLED",
        skipped: "SKIPPED",
    }[status] ?? "INFO";
}

export function buildDiscordDeployPayload({
    workflow,
    status,
    environment,
    branch,
    sha,
    imageTag,
    target,
    runUrl,
    repository = "UMC-PRODUCT/umc-product-server",
    timestamp = new Date().toISOString(),
}) {
    const normalizedStatus = normalizeStatus(status);
    const fields = [
        { name: "Status", value: normalizedStatus, inline: true },
        { name: "Environment", value: environment || "unknown", inline: true },
        { name: "Target", value: target || "unknown", inline: true },
        { name: "Branch", value: branch || "unknown", inline: true },
        { name: "Commit", value: shortSha(sha) || "unknown", inline: true },
        { name: "Image Tag", value: imageTag || "n/a", inline: true },
    ];

    return {
        embeds: [
            {
                title: `${statusIcon(normalizedStatus)} ${workflow || "CD"} 결과`,
                url: runUrl,
                color: COLORS[normalizedStatus] ?? 0x6a737d,
                fields,
                footer: { text: repository },
                timestamp,
            },
        ],
    };
}

export async function run() {
    const webhookUrl = process.env.DISCORD_WEBHOOK_URL;
    if (!webhookUrl) {
        console.log("DISCORD_WEBHOOK_URL is empty. Skip Discord deployment notification.");
        return;
    }

    const payload = buildDiscordDeployPayload({
        workflow: process.env.DEPLOY_WORKFLOW || process.env.GITHUB_WORKFLOW,
        status: process.env.DEPLOY_STATUS,
        environment: process.env.DEPLOY_ENVIRONMENT,
        branch: process.env.DEPLOY_BRANCH || process.env.GITHUB_REF_NAME,
        sha: process.env.DEPLOY_SHA || process.env.GITHUB_SHA,
        imageTag: process.env.DEPLOY_IMAGE_TAG,
        target: process.env.DEPLOY_TARGET,
        runUrl: process.env.DEPLOY_RUN_URL,
        repository: process.env.GITHUB_REPOSITORY,
    });

    const response = await fetch(webhookUrl, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
    });

    if (!response.ok) {
        const body = await response.text();
        throw new Error(`Discord webhook request failed: status=${response.status}, body=${body.slice(0, 500)}`);
    }
    console.log("Discord deployment notification sent.");
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
    run().catch((error) => {
        console.error(error);
        process.exitCode = 1;
    });
}
