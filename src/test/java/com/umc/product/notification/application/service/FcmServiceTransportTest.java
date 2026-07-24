package com.umc.product.notification.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import com.umc.product.global.config.notification.NotificationTransport;
import com.umc.product.global.config.notification.NotificationTransportProperties;
import com.umc.product.notification.application.port.in.dto.RegisterFcmTokenCommand;
import com.umc.product.notification.application.port.out.LoadFcmPort;
import com.umc.product.notification.application.port.out.ManageExternalFcmInstallationPort;
import com.umc.product.notification.application.port.out.SaveFcmPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmService transport")
class FcmServiceTransportTest {

    @Mock
    private LoadFcmPort loadFcmPort;

    @Mock
    private SaveFcmPort saveFcmPort;

    @Mock
    private ManageExternalFcmInstallationPort externalInstallationPort;

    @Test
    @DisplayName("Shadow 동기화 실패는 기존 local 등록 API를 실패시키지 않는다")
    void shadowExternalFailureDoesNotFailLocalRegistration() {
        RegisterFcmTokenCommand command =
            RegisterFcmTokenCommand.of(1L, "installation-1", "token", "IOS", "1.0.0");
        doThrow(new IllegalStateException("Lambda unavailable"))
            .when(externalInstallationPort)
            .register(command);
        when(loadFcmPort.listActiveByToken(anyString())).thenReturn(List.of());
        when(loadFcmPort.findByInstallationIdForUpdate(anyString())).thenReturn(Optional.empty());

        FcmService service = service(NotificationTransport.SHADOW);

        assertThatCode(() -> service.registerFcmToken(command)).doesNotThrowAnyException();
        verify(saveFcmPort).save(org.mockito.ArgumentMatchers.any());
        verify(externalInstallationPort).register(command);
    }

    @Test
    @DisplayName("External 동기화 실패는 클라이언트 요청 실패로 전파한다")
    void externalFailureIsPropagated() {
        RegisterFcmTokenCommand command =
            RegisterFcmTokenCommand.of(1L, "installation-1", "token", "IOS", "1.0.0");
        doThrow(new IllegalStateException("Lambda unavailable"))
            .when(externalInstallationPort)
            .register(command);

        FcmService service = service(NotificationTransport.EXTERNAL);

        assertThatThrownBy(() -> service.registerFcmToken(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Lambda unavailable");
    }

    @SuppressWarnings("unchecked")
    private FcmService service(NotificationTransport transport) {
        ObjectProvider<ManageExternalFcmInstallationPort> provider = mock(ObjectProvider.class);
        org.mockito.Mockito.when(provider.getIfAvailable()).thenReturn(externalInstallationPort);
        return new FcmService(
            loadFcmPort,
            saveFcmPort,
            provider,
            new NotificationTransportProperties(transport)
        );
    }
}
