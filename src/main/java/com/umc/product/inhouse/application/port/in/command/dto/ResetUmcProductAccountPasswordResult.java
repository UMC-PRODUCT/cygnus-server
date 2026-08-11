package com.umc.product.inhouse.application.port.in.command.dto;

public record ResetUmcProductAccountPasswordResult(
    String email,
    String temporaryPassword
) {
    @Override
    public String toString() {
        return "ResetUmcProductAccountPasswordResult[email=" + email + ", temporaryPassword=***]";
    }
}
