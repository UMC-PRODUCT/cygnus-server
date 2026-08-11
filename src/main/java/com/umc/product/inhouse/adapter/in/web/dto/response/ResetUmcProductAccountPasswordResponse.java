package com.umc.product.inhouse.adapter.in.web.dto.response;

import com.umc.product.inhouse.application.port.in.command.dto.ResetUmcProductAccountPasswordResult;

public record ResetUmcProductAccountPasswordResponse(
    String email,
    String temporaryPassword
) {
    public static ResetUmcProductAccountPasswordResponse from(ResetUmcProductAccountPasswordResult result) {
        return new ResetUmcProductAccountPasswordResponse(result.email(), result.temporaryPassword());
    }

    @Override
    public String toString() {
        return "ResetUmcProductAccountPasswordResponse[email=" + email + ", temporaryPassword=***]";
    }
}
