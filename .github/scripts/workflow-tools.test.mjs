import assert from "node:assert/strict";
import test from "node:test";

import {
    buildGeminiPayload,
    buildLargePrReview,
    buildPrTitleReview,
    estimateGeminiCost,
    REVIEW_MARKER,
    summarizeChangedFiles,
    validateTitleByRule,
} from "./pr-title-review.mjs";
import {
    buildCiFeedback,
    buildCommitStatusPayload,
    githubFetch,
    selectMarkerComment,
} from "./github-pr-feedback.mjs";
import { buildDiscordDeployPayload } from "./discord-deploy-notify.mjs";

test("Gemini payload uses static style guide without historical title list", () => {
    const payload = buildGeminiPayload({
        currentTitle: "[Feat] 프로젝트 권한 Capability API 추가",
        body: "## 작업 내용\n권한 API를 추가합니다.",
        headRef: "feature/project-capability",
        baseRef: "develop",
        changedFiles: Array.from({ length: 100 }, (_, index) => ({
            filename: `src/File${index}.java`,
            additions: 10,
            deletions: 3,
            changes: 13,
        })),
    });

    assert.equal(payload.currentTitle, "[Feat] 프로젝트 권한 Capability API 추가");
    assert.equal(payload.changedFiles.length, 40);
    assert.ok(payload.styleGuide.includes("[Feat] 프로젝트 권한 Capability API 추가"));
    assert.ok(payload.styleGuide.includes("[Feat] API Rate Limiter 도입"));
    assert.equal(Object.hasOwn(payload, "historicalTitles"), false);
});

test("changed file summary counts GitHub additions and deletions", () => {
    const summary = summarizeChangedFiles([
        { filename: "src/A.java", additions: 1000, deletions: 700, changes: 1700 },
        { filename: "src/B.java", additions: 100, deletions: 250, changes: 350 },
    ]);

    assert.equal(summary.totalChangedLines, 2050);
    assert.equal(summary.totalAdditions, 1100);
    assert.equal(summary.totalDeletions, 950);
    assert.deepEqual(summary.files, ["src/A.java", "src/B.java"]);
});

test("rule validation accepts repository style title", () => {
    const result = validateTitleByRule("[Fix] 프로젝트 지원 통계 인원을 지부 단위가 아닌 기수 단위로 집계하는 오류 수정");

    assert.equal(result.valid, true);
    assert.deepEqual(result.violations, []);
});

test("rule validation accepts only canonical Hotfix casing", () => {
    const canonical = validateTitleByRule("[Hotfix] 테스트 실패 수정");
    const legacy = validateTitleByRule("[HotFix] 테스트 실패 수정");

    assert.equal(canonical.valid, true);
    assert.equal(legacy.valid, false);
    assert.deepEqual(legacy.violations, ["INVALID_TAG"]);
    assert.equal(legacy.suggestedTitle, "[Hotfix] 테스트 실패 수정");
});

test("rule validation rejects unsafe or low quality titles", () => {
    const cases = [
        "WIP: [Feat] UPMS 제작",
        "기능 추가",
        "[feat] 기능 추가",
        "[Feat] 기능  추가",
        "[Feat] 수정",
        "[Feat] 기능 추가!",
    ];

    for (const title of cases) {
        assert.equal(validateTitleByRule(title).valid, false, title);
    }
});

test("PR title review deletes stale comment when title is valid", () => {
    const review = buildPrTitleReview({
        currentTitle: "[Docs] 프로젝트 지원서 상태 변경 정책 문서화",
        geminiResult: null,
    });

    assert.equal(review.valid, true);
    assert.equal(review.commentAction, "delete");
    assert.equal(review.statusState, "success");
    assert.equal(review.marker, REVIEW_MARKER);
});

test("PR title review comments token usage and estimated cost when Gemini is called", () => {
    const review = buildPrTitleReview({
        currentTitle: "[Docs] 프로젝트 지원서 상태 변경 정책 문서화",
        geminiResult: {
            valid: true,
            reason: "저장소 관례에 맞습니다.",
            suggestedTitle: null,
            confidence: 0.93,
            violations: [],
        },
        usage: {
            model: "gemini-3.5-flash",
            inputTokens: 1000,
            outputTokens: 100,
            totalTokens: 1100,
            estimatedCostUsd: 0.0024,
            inputCostUsd: 0.0015,
            outputCostUsd: 0.0009,
        },
    });

    assert.equal(review.valid, true);
    assert.equal(review.commentAction, "upsert");
    assert.equal(review.statusState, "success");
    assert.match(review.commentBody, /Input tokens: `1,000`/);
    assert.match(review.commentBody, /Estimated cost: `\$0\.002400`/);
});

test("large PR skips Gemini call and leaves approved status comment", () => {
    const review = buildLargePrReview({
        currentTitle: "[Feat] 대규모 프로젝트 권한 정책 정비",
        changeSummary: {
            files: ["src/A.java", "src/B.java"],
            totalChangedLines: 2000,
            totalAdditions: 1200,
            totalDeletions: 800,
        },
    });

    assert.equal(review.valid, true);
    assert.equal(review.commentAction, "upsert");
    assert.equal(review.statusState, "success");
    assert.match(review.statusDescription, /approved/);
    assert.match(review.commentBody, /Gemini API 호출을 생략/);
    assert.equal(review.usage, null);
});

test("Gemini cost estimate uses current default paid tier pricing", () => {
    const estimate = estimateGeminiCost({
        model: "gemini-3.5-flash",
        inputTokens: 1000,
        outputTokens: 100,
    });

    assert.equal(estimate.inputCostUsd, 0.0015);
    assert.equal(estimate.outputCostUsd, 0.0009);
    assert.equal(estimate.estimatedCostUsd, 0.0024);
});

test("PR title review comments with suggestion when title is invalid", () => {
    const review = buildPrTitleReview({
        currentTitle: "[feat] 기능 추가!",
        geminiResult: {
            valid: false,
            reason: "태그 대소문자와 특수문자가 저장소 관례에 맞지 않습니다.",
            suggestedTitle: "[Feat] 기능 추가",
            confidence: 0.9,
            violations: ["INVALID_TAG", "INVALID_SPECIAL_CHAR"],
        },
    });

    assert.equal(review.valid, false);
    assert.equal(review.commentAction, "upsert");
    assert.equal(review.statusState, "failure");
    assert.match(review.commentBody, /\[Feat\] 기능 추가/);
});

test("marker comment selection prefers first matching comment", () => {
    const selected = selectMarkerComment(
        [
            { id: 1, body: "일반 댓글" },
            { id: 2, body: "<!-- marker -->\nold" },
            { id: 3, body: "<!-- marker -->\nnew" },
        ],
        "<!-- marker -->",
    );

    assert.equal(selected.id, 2);
});

test("CI feedback keeps existing status context and comment marker", () => {
    const feedback = buildCiFeedback({
        eventName: "pull_request",
        workflow: "CI",
        ref: "refs/pull/1/merge",
        runId: "123",
        repository: "UMC-PRODUCT/umc-product-server",
        sha: "abcdef1234567890",
        jobResult: "failure",
        testOutcome: "success",
    });

    assert.equal(feedback.statusState, "failure");
    assert.equal(feedback.commentAction, "upsert");
    assert.match(feedback.commentBody, /<!-- umc-product-ci-status -->/);
    assert.match(feedback.commentBody, /## :x: CI 상태: 실패/);

    const status = buildCommitStatusPayload(feedback);
    assert.equal(status.context, "Product Team Server CI");
    assert.equal(status.state, "failure");
});

test("CI feedback renders success icon as GitHub emoji shortcode", () => {
    const feedback = buildCiFeedback({
        eventName: "pull_request",
        workflow: "CI",
        ref: "refs/pull/1/merge",
        runId: "123",
        repository: "UMC-PRODUCT/umc-product-server",
        sha: "abcdef1234567890",
        jobResult: "success",
        testOutcome: "success",
    });

    assert.match(feedback.commentBody, /## :white_check_mark: CI 상태: 성공/);
});

test("CI feedback validates repository format", () => {
    assert.throws(
        () => buildCiFeedback({
            eventName: "pull_request",
            workflow: "CI",
            ref: "refs/pull/1/merge",
            runId: "123",
            repository: "invalid-repository",
            sha: "abcdef1234567890",
            jobResult: "success",
            testOutcome: "success",
        }),
        /Invalid GITHUB_REPOSITORY/,
    );
});

test("GitHub fetch preserves non JSON error body", async () => {
    await assert.rejects(
        () => githubFetch("/boom", {
            token: "token",
            fetchImpl: async () => ({
                ok: false,
                status: 502,
                text: async () => "<html>Bad Gateway</html>",
            }),
        }),
        /status=502 body=<html>Bad Gateway<\/html>/,
    );
});

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
