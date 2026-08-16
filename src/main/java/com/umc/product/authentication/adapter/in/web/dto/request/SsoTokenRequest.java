package com.umc.product.authentication.adapter.in.web.dto.request;

import com.umc.product.authentication.application.port.in.command.dto.ExchangeSsoAuthorizationCodeCommand;

import jakarta.validation.constraints.NotBlank;

public class SsoTokenRequest {

    @NotBlank private String grant_type;

    @NotBlank private String code;

    @NotBlank private String client_id;

    @NotBlank private String redirect_uri;

    @NotBlank private String code_verifier;

    public String getGrant_type() {
        return grant_type;
    }

    public void setGrant_type(String grant_type) {
        this.grant_type = grant_type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getClient_id() {
        return client_id;
    }

    public void setClient_id(String client_id) {
        this.client_id = client_id;
    }

    public String getRedirect_uri() {
        return redirect_uri;
    }

    public void setRedirect_uri(String redirect_uri) {
        this.redirect_uri = redirect_uri;
    }

    public String getCode_verifier() {
        return code_verifier;
    }

    public void setCode_verifier(String code_verifier) {
        this.code_verifier = code_verifier;
    }

    public ExchangeSsoAuthorizationCodeCommand toCommand() {
        return ExchangeSsoAuthorizationCodeCommand.of(
            grant_type,
            code,
            client_id,
            redirect_uri,
            code_verifier
        );
    }
}
