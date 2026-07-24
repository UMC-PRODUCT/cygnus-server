function required(name: string): string {
  const value = process.env[name];
  if (value === undefined || value.trim() === "") {
    throw new Error(`${name} 환경변수는 필수입니다.`);
  }
  return value;
}

export const config = {
  get installationsTable(): string {
    return required("INSTALLATIONS_TABLE");
  },
  get tokenOwnersTable(): string {
    return required("TOKEN_OWNERS_TABLE");
  },
  get deliveriesTable(): string {
    return required("DELIVERIES_TABLE");
  },
  get fcmBatchQueueUrl(): string {
    return required("FCM_BATCH_QUEUE_URL");
  },
  get webhookQueueUrl(): string {
    return required("WEBHOOK_QUEUE_URL");
  },
  get eventBusName(): string {
    return required("EVENT_BUS_NAME");
  },
  get firebaseSecretId(): string {
    return required("FIREBASE_SECRET_ID");
  },
  get webhookSecretId(): string {
    return required("WEBHOOK_SECRET_ID");
  },
  get emailFromAddress(): string {
    return required("EMAIL_FROM_ADDRESS");
  },
  get emailFromName(): string {
    return process.env.EMAIL_FROM_NAME ?? "University MakeUs Challenge";
  },
  get sesConfigurationSet(): string | undefined {
    return process.env.SES_CONFIGURATION_SET || undefined;
  },
  get deliveryMode(): "LIVE" | "VALIDATE" {
    return process.env.DELIVERY_MODE === "LIVE" ? "LIVE" : "VALIDATE";
  }
};
