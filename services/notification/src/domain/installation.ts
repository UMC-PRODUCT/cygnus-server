export interface Installation {
  installationId: string;
  memberId: number;
  fcmToken: string;
  tokenHash: string;
  platform?: string | null;
  appVersion?: string | null;
  status: "ACTIVE" | "INACTIVE";
  registeredAt: string;
  deactivatedAt?: string | null;
  lastValidatedAt: string;
  memberStatusKey?: string;
  validationStatusKey?: string;
}

export interface RegisterInstallation {
  memberId: number;
  installationId: string;
  fcmToken: string;
  platform?: string | null;
  appVersion?: string | null;
  occurredAt: string;
}
