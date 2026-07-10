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
        deactivateTokensOwnedByOtherMembers(command.memberId(), command.fcmToken());

        loadFcmPort.findByMemberIdAndToken(command.memberId(), command.fcmToken())
            .ifPresentOrElse(
                token -> {
                    token.register(command.platform(), command.deviceId(), command.appVersion());
                    saveFcmPort.save(token);
                },
                () -> saveFcmPort.save(FcmToken.create(
                    command.memberId(),
                    command.fcmToken(),
                    command.platform(),
                    command.deviceId(),
                    command.appVersion()
                ))
            );
    }

    @Override
    @Transactional
    public void unregisterFcmToken(UnregisterFcmTokenCommand command) {
        loadFcmPort.findByMemberIdAndToken(command.memberId(), command.fcmToken())
            .ifPresent(token -> {
                token.deactivate();
                saveFcmPort.save(token);
            });
    }

    private void deactivateTokensOwnedByOtherMembers(Long memberId, String fcmToken) {
        loadFcmPort.listActiveByToken(fcmToken).stream()
            .filter(token -> !token.belongsTo(memberId))
            .forEach(token -> {
                token.deactivate();
                saveFcmPort.save(token);
            });
    }

}
