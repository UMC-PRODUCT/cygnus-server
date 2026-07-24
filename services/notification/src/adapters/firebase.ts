import { cert, getApps, initializeApp } from "firebase-admin/app";
import { getMessaging, type Messaging } from "firebase-admin/messaging";
import { GetSecretValueCommand } from "@aws-sdk/client-secrets-manager";
import { config } from "../shared/config.js";
import { secretsManager } from "../shared/aws.js";

interface FirebaseServiceAccount {
  project_id: string;
  client_email: string;
  private_key: string;
}

let messagingPromise: Promise<Messaging> | undefined;

export function firebaseMessaging(): Promise<Messaging> {
  messagingPromise ??= initializeMessaging();
  return messagingPromise;
}

async function initializeMessaging(): Promise<Messaging> {
  if (getApps().length === 0) {
    const response = await secretsManager.send(new GetSecretValueCommand({
      SecretId: config.firebaseSecretId
    }));
    if (response.SecretString === undefined) {
      throw new Error("Firebase service account secret에 SecretString이 없습니다.");
    }
    const serviceAccount = JSON.parse(response.SecretString) as FirebaseServiceAccount;
    initializeApp({
      credential: cert({
        projectId: serviceAccount.project_id,
        clientEmail: serviceAccount.client_email,
        privateKey: serviceAccount.private_key
      }),
      projectId: serviceAccount.project_id
    });
  }
  return getMessaging();
}

const INVALID_CODES = new Set([
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
  "messaging/mismatched-credential"
]);

const RETRYABLE_CODES = new Set([
  "messaging/internal-error",
  "messaging/server-unavailable",
  "messaging/unknown-error",
  "messaging/quota-exceeded"
]);

export function classifyFcmError(code: string | undefined): "INVALID" | "RETRYABLE" | "PERMANENT" {
  if (code !== undefined && INVALID_CODES.has(code)) {
    return "INVALID";
  }
  if (code !== undefined && RETRYABLE_CODES.has(code)) {
    return "RETRYABLE";
  }
  return "PERMANENT";
}
