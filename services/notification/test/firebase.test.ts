import { describe, expect, it } from "vitest";
import { classifyFcmError } from "../src/adapters/firebase.js";

describe("FCM 오류 분류", () => {
  it("등록 해제된 token을 invalid로 분류한다", () => {
    expect(classifyFcmError("messaging/registration-token-not-registered")).toBe("INVALID");
  });

  it("서버 오류와 quota 오류를 retryable로 분류한다", () => {
    expect(classifyFcmError("messaging/server-unavailable")).toBe("RETRYABLE");
    expect(classifyFcmError("messaging/quota-exceeded")).toBe("RETRYABLE");
  });

  it("그 외 오류는 영구 실패로 분류한다", () => {
    expect(classifyFcmError("messaging/invalid-argument")).toBe("PERMANENT");
  });
});
