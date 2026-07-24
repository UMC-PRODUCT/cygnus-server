import { GetSecretValueCommand } from "@aws-sdk/client-secrets-manager";
import type { WebhookPlatform } from "../contracts/types.js";
import { secretsManager } from "../shared/aws.js";
import { config } from "../shared/config.js";

interface WebhookSecrets {
  slackUrl?: string;
  discordUrl?: string;
  telegramBotToken?: string;
  telegramChatId?: string;
}

let secretsPromise: Promise<WebhookSecrets> | undefined;

export async function sendWebhook(
  platform: WebhookPlatform,
  title: string,
  content: string
): Promise<void> {
  const secrets = await webhookSecrets();
  switch (platform) {
    case "SLACK":
      await post(requiredSecret(secrets.slackUrl, "slackUrl"), {
        text: `*${title}*\n${content}`
      });
      return;
    case "DISCORD":
      await post(requiredSecret(secrets.discordUrl, "discordUrl"), {
        embeds: [{ title, description: content }]
      });
      return;
    case "TELEGRAM": {
      const token = requiredSecret(secrets.telegramBotToken, "telegramBotToken");
      await post(`https://api.telegram.org/bot${token}/sendMessage`, {
        chat_id: requiredSecret(secrets.telegramChatId, "telegramChatId"),
        text: `${title}\n${content}`
      });
      return;
    }
  }
}

async function webhookSecrets(): Promise<WebhookSecrets> {
  secretsPromise ??= loadSecrets();
  return secretsPromise;
}

async function loadSecrets(): Promise<WebhookSecrets> {
  const response = await secretsManager.send(new GetSecretValueCommand({
    SecretId: config.webhookSecretId
  }));
  if (response.SecretString === undefined) {
    throw new Error("Webhook secret에 SecretString이 없습니다.");
  }
  return JSON.parse(response.SecretString) as WebhookSecrets;
}

async function post(url: string, body: unknown): Promise<void> {
  const response = await fetch(url, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body),
    signal: AbortSignal.timeout(10_000)
  });
  if (!response.ok) {
    throw new WebhookHttpError(response.status);
  }
}

function requiredSecret(value: string | undefined, name: string): string {
  if (value === undefined || value === "") {
    throw new Error(`Webhook secret의 ${name} 값이 없습니다.`);
  }
  return value;
}

export class WebhookHttpError extends Error {
  constructor(readonly status: number) {
    super(`Webhook provider가 HTTP ${status}를 반환했습니다.`);
    this.name = "WebhookHttpError";
  }

  get retryable(): boolean {
    return this.status === 408 || this.status === 429 || this.status >= 500;
  }
}
