package com.umc.product.notification.application.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.global.config.notification.NotificationTransportProperties;
import com.umc.product.notification.application.port.in.ManageFcmUseCase;
import com.umc.product.notification.application.port.in.dto.RegisterFcmTokenCommand;
import com.umc.product.notification.application.port.in.dto.UnregisterFcmTokenCommand;
import com.umc.product.notification.application.port.out.LoadFcmPort;
import com.umc.product.notification.application.port.out.ManageExternalFcmInstallationPort;
import com.umc.product.notification.application.port.out.SaveFcmPort;
import com.umc.product.notification.domain.FcmToken;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FcmService implements ManageFcmUseCase {

    private final LoadFcmPort loadFcmPort;
    private final SaveFcmPort saveFcmPort;
    private final ManageExternalFcmInstallationPort externalInstallationPort;
    private final NotificationTransportProperties transportProperties;

    @Autowired
    public FcmService(
        LoadFcmPort loadFcmPort,
        SaveFcmPort saveFcmPort,
        ObjectProvider<ManageExternalFcmInstallationPort> externalInstallationPortProvider,
        NotificationTransportProperties transportProperties
    ) {
        this.loadFcmPort = loadFcmPort;
        this.saveFcmPort = saveFcmPort;
        this.externalInstallationPort = externalInstallationPortProvider.getIfAvailable();
        this.transportProperties = transportProperties;
    }

    @Override
    @Transactional
    public void registerFcmToken(RegisterFcmTokenCommand command) {
        if (transportProperties.transport().sendsLocally()) {
            registerLocally(command);
        }
        if (transportProperties.transport().sendsExternally()) {
            syncExternal(() -> externalPort().register(command));
        }
    }

    private void registerLocally(RegisterFcmTokenCommand command) {
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
        if (transportProperties.transport().sendsLocally()) {
            unregisterLocally(command);
        }
        if (transportProperties.transport().sendsExternally()) {
            syncExternal(() -> externalPort().unregister(command));
        }
    }

    private void unregisterLocally(UnregisterFcmTokenCommand command) {
        loadFcmPort.findByInstallationIdForUpdate(command.installationId())
            .filter(token -> token.belongsTo(command.memberId()))
            .ifPresent(token -> {
                token.deactivate();
                saveFcmPort.save(token);
            });
    }

    private ManageExternalFcmInstallationPort externalPort() {
        if (externalInstallationPort == null) {
            throw new IllegalStateException("외부 notification Lambda가 활성화되지 않았습니다.");
        }
        return externalInstallationPort;
    }

    private void syncExternal(Runnable command) {
        try {
            command.run();
        } catch (RuntimeException exception) {
            if (transportProperties.transport().sendsLocally()) {
                log.warn(
                    "Shadow FCM installation 동기화에 실패했습니다: errorType={}",
                    exception.getClass().getSimpleName()
                );
                return;
            }
            throw exception;
        }
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
