package com.umc.product.demoday.adapter.in.web.dto.response;

import java.util.List;

import com.umc.product.demoday.application.port.in.command.dto.CreateDemodayEntryCodesInfo;

public record CreateDemodayEntryCodeResponse(List<String> codes) {

    public static CreateDemodayEntryCodeResponse from(CreateDemodayEntryCodesInfo info) {
        return new CreateDemodayEntryCodeResponse(info.codes());
    }
}
