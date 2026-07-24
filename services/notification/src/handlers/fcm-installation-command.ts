import type { Handler } from "aws-lambda";
import { assertValid, validateInstallationCommand } from "../contracts/schemas.js";
import type { FcmInstallationCommand } from "../contracts/types.js";
import { installationRepository } from "../repositories/installation-repository.js";
import { logger } from "../shared/observability.js";

export const handler: Handler<FcmInstallationCommand, { processed: boolean }> = async (command) => {
  assertValid(validateInstallationCommand, command);

  if (command.action === "UPSERT") {
    if (command.fcmToken === undefined || command.fcmToken === "") {
      throw new Error("UPSERT command에는 fcmToken이 필요합니다.");
    }
    await installationRepository.register({
      memberId: command.memberId,
      installationId: command.installationId,
      fcmToken: command.fcmToken,
      ...(command.platform !== undefined ? { platform: command.platform } : {}),
      ...(command.appVersion !== undefined ? { appVersion: command.appVersion } : {}),
      occurredAt: command.occurredAt
    });
    logger.info("FCM installation 등록을 처리했습니다.", {
      commandId: command.commandId,
      installationPresent: true
    });
    return { processed: true };
  }

  const processed = await installationRepository.deactivateOwned(
    command.memberId,
    command.installationId,
    command.occurredAt
  );
  logger.info("FCM installation 해제를 처리했습니다.", {
    commandId: command.commandId,
    processed
  });
  return { processed };
};
