import { describe, expect, it } from "vitest";
import {
  renderRecruitingEmail,
  renderVerificationEmail
} from "../src/adapters/email-templates.js";
import { isVerificationExpired } from "../src/domain/email.js";

describe("이메일 template", () => {
  it("인증 코드를 template에 출력한다", () => {
    expect(renderVerificationEmail("123456")).toContain("123456");
  });

  it("채용 이메일 입력값의 HTML을 escape한다", () => {
    const html = renderRecruitingEmail({
      applicantName: "<script>alert(1)</script>",
      availabilityFormId: 10,
      contactText: "<b>문의</b>"
    });

    expect(html).not.toContain("<script>");
    expect(html).not.toContain("<b>문의</b>");
    expect(html).toContain("&lt;script&gt;");
  });

  it("인증 메일 만료 시각이 지난 요청을 판별한다", () => {
    const now = Date.parse("2026-07-23T10:00:00.000Z");

    expect(isVerificationExpired("2026-07-23T09:59:59.000Z", now)).toBe(true);
    expect(isVerificationExpired("2026-07-23T10:00:01.000Z", now)).toBe(false);
  });
});
