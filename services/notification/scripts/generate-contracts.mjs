import { readFile, writeFile } from "node:fs/promises";
import { resolve } from "node:path";
import { compile } from "json-schema-to-typescript";
import { parse } from "yaml";

const contractPath = resolve(process.cwd(), "../../contracts/notification/asyncapi.yaml");
const document = parse(await readFile(contractPath, "utf8"));
const schemas = document.components.schemas;

const detailNames = [
  "FcmRequestDetail",
  "VerificationEmailDetail",
  "RecruitingEmailDetail",
  "WebhookDetail",
  "EmailResultDetail"
];

const generatedTypes = [];
for (const name of detailNames) {
  generatedTypes.push(await compile(schemas[name], name, {
    bannerComment: "",
    ignoreMinAndMaxItems: true,
    style: {
      semi: true,
      singleQuote: false,
      tabWidth: 2,
      trailingComma: "none"
    }
  }));
}

await writeFile(
  resolve(process.cwd(), "src/contracts/generated.ts"),
  [
    "// 이 파일은 contracts/notification/asyncapi.yaml에서 생성됩니다. 직접 수정하지 마세요.",
    "",
    ...generatedTypes
  ].join("\n"),
  "utf8"
);

const messageSchemas = {
  notificationFcmRequestedSchema: messageSchema(
    "notification.fcm.requested.v1",
    schemas.FcmRequestDetail
  ),
  authenticationEmailVerificationRequestedSchema: messageSchema(
    "authentication.email.verification.requested.v1",
    schemas.VerificationEmailDetail
  ),
  recruitingInterviewEmailRequestedSchema: messageSchema(
    "recruiting.interview.email.requested.v1",
    schemas.RecruitingEmailDetail
  ),
  notificationWebhookRequestedSchema: messageSchema(
    "notification.webhook.requested.v1",
    schemas.WebhookDetail
  ),
  notificationEmailResultSchema: messageSchema(undefined, schemas.EmailResultDetail)
};

const generatedSchemaSource = [
  "// 이 파일은 contracts/notification/asyncapi.yaml에서 생성됩니다. 직접 수정하지 마세요.",
  ...Object.entries(messageSchemas).map(([name, schema]) =>
    `export const ${name} = ${JSON.stringify(schema, null, 2)} as const;`
  )
].join("\n\n") + "\n";

await writeFile(
  resolve(process.cwd(), "src/contracts/generated-schemas.ts"),
  generatedSchemaSource,
  "utf8"
);

function messageSchema(eventType, detailSchema) {
  const envelope = structuredClone(schemas.EventEnvelope);
  return {
    ...envelope,
    properties: {
      ...envelope.properties,
      ...(eventType === undefined
        ? {}
        : { eventType: { type: "string", const: eventType } }),
      detail: structuredClone(detailSchema)
    }
  };
}
