package com.umc.product.authentication.adapter.in.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.umc.product.authentication.application.event.SsoAuthorizationCodeExchangedEvent;
import com.umc.product.authentication.application.event.SsoAuthorizationCodeIssuedEvent;
import com.umc.product.authentication.application.event.SsoBrowserLoginCreatedEvent;
import com.umc.product.authentication.application.event.SsoTokenIssuedEvent;
import com.umc.product.global.logging.OperationalMetrics;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SsoSecurityEventListener {

    private static final String AUTHENTICATION_DOMAIN = "AUTHENTICATION";

    private final OperationalMetrics operationalMetrics;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SsoBrowserLoginCreatedEvent event) {
        operationalMetrics.recordSecurityEvent(AUTHENTICATION_DOMAIN, "SSO_BROWSER_LOGIN", "success");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SsoAuthorizationCodeIssuedEvent event) {
        operationalMetrics.recordSecurityEvent(AUTHENTICATION_DOMAIN, "SSO_AUTHORIZATION_CODE", "issued");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SsoAuthorizationCodeExchangedEvent event) {
        operationalMetrics.recordSecurityEvent(AUTHENTICATION_DOMAIN, "SSO_AUTHORIZATION_CODE", "exchanged");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SsoTokenIssuedEvent event) {
        operationalMetrics.recordSecurityEvent(AUTHENTICATION_DOMAIN, "SSO_TOKEN", "issued");
    }
}
