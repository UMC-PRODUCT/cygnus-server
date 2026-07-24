// 이 파일은 contracts/notification/asyncapi.yaml에서 생성됩니다. 직접 수정하지 마세요.

export interface FcmRequestDetail {
  chunkIndex: number;
  chunkCount: number;
  /**
   * @maxItems 500
   */
  memberIds: number[];
  title: string;
  body: string;
  data: {
    [k: string]: string;
  };
  imageUrl?: string | null;
  deepLink?: string | null;
}

export interface VerificationEmailDetail {
  email: string;
  verificationCode: string;
  expiresAt: string;
}

export interface RecruitingEmailDetail {
  applicationId: number;
  email: string;
  applicantName: string;
  availabilityFormId: number;
  contactText: string;
}

export interface WebhookDetail {
  /**
   * @minItems 1
   */
  platforms: ("TELEGRAM" | "DISCORD" | "SLACK")[];
  title: string;
  content: string;
  attempt: number;
}

export interface EmailResultDetail {
  correlationType: "EMAIL_VERIFICATION" | "RECRUITING_INTERVIEW";
  correlationId: string;
  status: "ACCEPTED" | "FAILED" | "EXPIRED";
  providerMessageId?: string | null;
  failureCode?: string | null;
  attemptedAt: string;
}
