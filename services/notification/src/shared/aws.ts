import { DynamoDBClient } from "@aws-sdk/client-dynamodb";
import { EventBridgeClient } from "@aws-sdk/client-eventbridge";
import { SecretsManagerClient } from "@aws-sdk/client-secrets-manager";
import { SESv2Client } from "@aws-sdk/client-sesv2";
import { SQSClient } from "@aws-sdk/client-sqs";
import { DynamoDBDocumentClient } from "@aws-sdk/lib-dynamodb";
import { tracer } from "./observability.js";

const marshallOptions = {
  removeUndefinedValues: true,
  convertClassInstanceToMap: true
};

export const dynamodb = DynamoDBDocumentClient.from(
  tracer.captureAWSv3Client(new DynamoDBClient({})),
  { marshallOptions }
);
export const sqs = tracer.captureAWSv3Client(new SQSClient({}));
export const eventBridge = tracer.captureAWSv3Client(new EventBridgeClient({}));
export const secretsManager = tracer.captureAWSv3Client(new SecretsManagerClient({}));
export const ses = tracer.captureAWSv3Client(new SESv2Client({}));
