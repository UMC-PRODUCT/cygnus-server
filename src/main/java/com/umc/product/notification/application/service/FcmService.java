package com.umc.product.notification.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.notification.application.port.in.ManageFcmUseCase;
import com.umc.product.notification.application.port.in.dto.RegisterFcmTokenCommand;
import com.umc.product.notification.application.port.in.dto.UnregisterFcmTokenCommand;
import com.umc.product.notification.application.port.out.LoadFcmPort;
import com.umc.product.notification.application.port.out.SaveFcmPort;
import com.umc.product.notification.domain.FcmToken;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService implements ManageFcmUseCase {

    private final LoadFcmPort loadFcmPort;
    private final SaveFcmPort saveFcmPort;

    @Override
    @Transactional
    public void registerFcmToken(RegisterFcmTokenCommand command) {
        deactivateTokenAssignedToOtherInstallations(command.installationId(), command.fcmToken());

        loadFcmPort.findByInstallationIdForUpdate(command.installationId())
            .ifPresentOrElse(
                token -> {
                    token.register(command.memberId(), command.fcmToken(), command.platform(), command.appVersion());
                    saveFcmPort.save(token);
                },
                () -> saveFcmPort.save(FcmToken.create(
                    command.memberId(),
                    command.installationId(),
                    command.fcmToken(),
                    command.platform(),
                    command.appVersion()
                ))
            );
    }

    @Override
    @Transactional
    public void unregisterFcmToken(UnregisterFcmTokenCommand command) {
        loadFcmPort.findByInstallationIdForUpdate(command.installationId())
            .filter(token -> token.belongsTo(command.memberId()))
            .ifPresent(token -> {
                token.deactivate();
                saveFcmPort.save(token);
            });
    }

    private void deactivateTokenAssignedToOtherInstallations(String installationId, String fcmToken) {
        loadFcmPort.listActiveByToken(fcmToken).stream()
            .filter(token -> !token.isInstalledAs(installationId))
            .forEach(token -> {
                token.deactivate();
                saveFcmPort.save(token);
            });
    }

}
