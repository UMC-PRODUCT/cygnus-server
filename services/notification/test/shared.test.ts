import { describe, expect, it } from "vitest";
import { deterministicUuid } from "../src/shared/id.js";
import { partition } from "../src/shared/sqs.js";

describe("공통 유틸리티", () => {
  it("목록을 순서를 유지해 지정 크기로 나눈다", () => {
    expect(partition([1, 2, 3, 4, 5], 2)).toEqual([[1, 2], [3, 4], [5]]);
  });

  it("같은 seed는 같은 UUID를 만든다", () => {
    const first = deterministicUuid("request:batch:0");
    const second = deterministicUuid("request:batch:0");

    expect(first).toBe(second);
    expect(first).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-8[0-9a-f]{3}-[0-9a-f]{12}$/);
  });
});
