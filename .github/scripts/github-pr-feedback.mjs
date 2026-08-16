import fs from "node:fs";
import { pathToFileURL } from "node:url";

const GITHUB_API = "https://api.github.com";
export const CI_MARKER = "<!-- umc-product-ci-status -->";
export const CI_STATUS_CONTEXT = "Product Team Server CI";

function kstTimestamp(now = new Date()) {
    return new Intl.DateTimeFormat("ko-KR", {
        timeZone: "Asia/Seoul",
        dateStyle: "medium",
        timeStyle: "medium",
    }).format(now);
}

function runUrl(owner, repo, runId) {
    return `https://github.com/${owner}/${repo}/actions/runs/${runId}`;
}

function shortSha(sha) {
    return String(sha ?? "").slice(0, 7);
}

export function selectMarkerComment(comments, marker) {
    return comments.find((comment) => comment.body?.includes(marker)) ?? null;
}

export function buildCiFeedback({
    eventName,
    workflow,
    ref,
    runId,
    repository,
    sha,
    jobResult,
    testOutcome,
    now = new Date(),
}) {
    const { owner, repo } = repositoryParts(repository);
    const failed = jobResult !== "success" || testOutcome === "failure";
    const stateText = failed ? "실패" : "성공";
    const icon = failed ? ":x:" : ":white_check_mark:";
    const kstTime = kstTimestamp(now);
    const targetUrl = runUrl(owner, repo, runId);
    const entry = [
        `${CI_MARKER}`,
        "",
        `## ${icon} CI 상태: ${stateText}`,
        "",
        `- Workflow: \`${workflow}\``,
        `- Branch: \`${ref}\``,
        `- Commit: \`${shortSha(sha)}\``,
        `- Update Time (KST): ${kstTime}`,
        `- Action Logs: [GitHub Actions](${targetUrl})`,
    ].join("\n");

    return {
        marker: CI_MARKER,
        context: CI_STATUS_CONTEXT,
        sha,
        targetUrl,
        statusState: failed ? "failure" : "success",
        statusDescription: failed ? `CI 실패 (${kstTime})` : `CI 성공 (${kstTime})`,
        commentAction: eventName === "pull_request" ? "upsert" : "none",
        commentBody: entry,
    };
}

export function buildCommitStatusPayload(feedback) {
    return {
        sha: feedback.sha,
        state: feedback.statusState,
        target_url: feedback.targetUrl,
        description: feedback.statusDescription.slice(0, 140),
        context: feedback.context,
    };
}

function readEvent() {
    const eventPath = process.env.GITHUB_EVENT_PATH;
    if (!eventPath || !fs.existsSync(eventPath)) {
        return {};
    }
    return JSON.parse(fs.readFileSync(eventPath, "utf8"));
}

function repositoryParts(repository) {
    const [owner, repo] = String(repository ?? "").split("/");
    if (!owner || !repo) {
        throw new Error(`Invalid GITHUB_REPOSITORY: ${repository}`);
    }
    return { owner, repo };
}

export async function githubFetch(path, { token, method = "GET", body, fetchImpl = fetch } = {}) {
    const response = await fetchImpl(`${GITHUB_API}${path}`, {
        method,
        headers: {
            Accept: "application/vnd.github+json",
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
            "X-GitHub-Api-Version": "2022-11-28",
        },
        body: body === undefined ? undefined : JSON.stringify(body),
    });

    if (response.status === 204) {
        return null;
    }
    const text = await response.text();
    if (!response.ok) {
        throw new Error(`GitHub API request failed: ${method} ${path} status=${response.status} body=${text.slice(0, 500)}`);
    }
    const data = text ? JSON.parse(text) : null;
    return data;
}

async function listIssueComments({ owner, repo, issueNumber, token }) {
    const comments = [];
    for (let page = 1; ; page += 1) {
        const pageComments = await githubFetch(
            `/repos/${owner}/${repo}/issues/${issueNumber}/comments?per_page=100&page=${page}`,
            { token },
        );
        comments.push(...pageComments);
        if (pageComments.length < 100) {
            return comments;
        }
    }
}

async function applyCommentFeedback({ owner, repo, issueNumber, token, marker, action, body }) {
    if (!issueNumber || action === "none") {
        return;
    }

    const comments = await listIssueComments({ owner, repo, issueNumber, token });
    const existing = selectMarkerComment(comments, marker);

    if (action === "delete") {
        if (existing) {
            await githubFetch(`/repos/${owner}/${repo}/issues/comments/${existing.id}`, { token, method: "DELETE" });
        }
        return;
    }

    if (action !== "upsert") {
        throw new Error(`Unsupported comment action: ${action}`);
    }

    if (existing) {
        await githubFetch(`/repos/${owner}/${repo}/issues/comments/${existing.id}`, {
            token,
            method: "PATCH",
            body: { body },
        });
    } else {
        await githubFetch(`/repos/${owner}/${repo}/issues/${issueNumber}/comments`, {
            token,
            method: "POST",
            body: { body },
        });
    }
}

async function applyCommitStatus({ owner, repo, token, feedback }) {
    const status = buildCommitStatusPayload(feedback);
    await githubFetch(`/repos/${owner}/${repo}/statuses/${status.sha}`, {
        token,
        method: "POST",
        body: {
            state: status.state,
            target_url: status.target_url,
            description: status.description,
            context: status.context,
        },
    });
}

function buildPrTitleFeedback({ result, event, repository, runId }) {
    const { owner, repo } = repositoryParts(repository);
    const sha = event.pull_request?.head?.sha ?? process.env.GITHUB_SHA;
    return {
        marker: result.marker,
        context: result.context,
        sha,
        targetUrl: runUrl(owner, repo, runId),
        statusState: result.statusState,
        statusDescription: result.statusDescription,
        commentAction: result.commentAction,
        commentBody: result.commentBody,
    };
}

export async function run() {
    const token = process.env.GITHUB_TOKEN || process.env.GH_TOKEN;
    if (!token) {
        throw new Error("GITHUB_TOKEN or GH_TOKEN is required.");
    }

    const mode = process.env.FEEDBACK_MODE;
    const event = readEvent();
    const repository = process.env.GITHUB_REPOSITORY;
    const { owner, repo } = repositoryParts(repository);
    const issueNumber = event.pull_request?.number ?? event.issue?.number;

    let feedback;
    if (mode === "ci") {
        const sha = event.pull_request?.head?.sha ?? process.env.STATUS_SHA ?? process.env.GITHUB_SHA;
        feedback = buildCiFeedback({
            eventName: process.env.GITHUB_EVENT_NAME,
            workflow: process.env.GITHUB_WORKFLOW,
            ref: process.env.GITHUB_REF,
            runId: process.env.GITHUB_RUN_ID,
            repository,
            sha,
            jobResult: process.env.JOB_RESULT || "success",
            testOutcome: process.env.TEST_OUTCOME || "success",
        });
    } else if (mode === "pr-title") {
        const resultPath = process.env.RESULT_PATH;
        if (!resultPath) {
            throw new Error("RESULT_PATH is required for pr-title feedback.");
        }
        const result = JSON.parse(fs.readFileSync(resultPath, "utf8"));
        feedback = buildPrTitleFeedback({
            result,
            event,
            repository,
            runId: process.env.GITHUB_RUN_ID,
        });
    } else {
        throw new Error(`Unsupported FEEDBACK_MODE: ${mode}`);
    }

    await applyCommentFeedback({
        owner,
        repo,
        issueNumber,
        token,
        marker: feedback.marker,
        action: feedback.commentAction,
        body: feedback.commentBody,
    });
    await applyCommitStatus({ owner, repo, token, feedback });
    console.log(`GitHub feedback applied: mode=${mode}, state=${feedback.statusState}, commentAction=${feedback.commentAction}`);
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
    run().catch((error) => {
        console.error(error);
        process.exitCode = 1;
    });
}
