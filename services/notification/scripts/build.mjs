import { build } from "esbuild";

const entryPoints = [
  "src/handlers/fcm-installation-command.ts",
  "src/handlers/fcm-request-resolver.ts",
  "src/handlers/fcm-batch-sender.ts",
  "src/handlers/fcm-token-validator.ts",
  "src/handlers/email-sender.ts",
  "src/handlers/webhook-sender.ts"
];

await build({
  entryPoints,
  outdir: "dist",
  bundle: true,
  platform: "node",
  target: "node24",
  format: "esm",
  sourcemap: true,
  minify: false
});
