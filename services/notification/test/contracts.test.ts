import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";
import {
  assertValid,
  ContractValidationError,
  validateFcmBatch,
  validateFcmRequest,
  validateVerificationEmail
} from "../src/contracts/schemas.js";

const base = {
  schemaVersion: 1,
  eventId: "b62a440e-4084-4f15-8b41-b9958412ad93",
  source: "umc-product.notice",
  occurredAt: "2026-07-23T10:00:00.000Z",
  requestId: "80255775-ab55-4df2-a84d-c476061c4c83"
} as const;

describe("notification event 계약", () => {
  it("Java가 생성하는 FCM fixture를 역직렬화한다", () => {
    const fixture = JSON.parse(readFileSync(
      resolve(process.cwd(), "../../contracts/notification/fixtures/fcm-requested.v1.json"),
      "utf8"
    )) as unknown;

    expect(validateFcmRequest(fixture)).toBe(true);
  });

  it("500명의 FCM 대상 요청을 허용한다", () => {
    const event = {
      ...base,
      eventType: "notification.fcm.requested.v1",
      detail: {
        chunkIndex: 0,
        chunkCount: 1,
        memberIds: Array.from({ length: 500 }, (_, index) => index + 1),
        title: "공지",
        body: "본문",
        data: {}
      }
    };

    expect(() => assertValid(validateFcmRequest, event)).not.toThrow();
  });

  it("중복 member ID와 500명 초과 요청을 거부한다", () => {
    const event = {
      ...base,
      eventType: "notification.fcm.requested.v1",
      detail: {
        chunkIndex: 0,
        chunkCount: 1,
        memberIds: [...Array.from({ length: 500 }, (_, index) => index + 1), 1],
        title: "공지",
        body: "본문",
        data: {}
      }
    };

    expect(() => assertValid(validateFcmRequest, event)).toThrow(ContractValidationError);
  });

  it("인증 코드는 6자리 숫자이고 만료 시각이 있어야 한다", () => {
    const event = {
      ...base,
      eventType: "authentication.email.verification.requested.v1",
      detail: {
        email: "alice@example.com",
        verificationCode: "abc",
        expiresAt: "2026-07-23T10:10:00.000Z"
      }
    };

    expect(() => assertValid(validateVerificationEmail, event)).toThrow(ContractValidationError);
  });

  it("FCM batch는 installation 500개와 최대 5회 재시도만 허용한다", () => {
    const event = {
      ...base,
      eventType: "notification.fcm.batch.requested.v1",
      detail: {
        batchId: "batch-1",
        installationIds: Array.from({ length: 500 }, (_, index) => `installation-${index}`),
        title: "공지",
        body: "본문",
        data: {},
        attempt: 5
      }
    };

    expect(() => assertValid(validateFcmBatch, event)).not.toThrow();
  });
});
