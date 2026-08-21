package com.umc.product.demoday.application.port.in.command.dto;

import java.util.List;

public record CreateDemodayEntryCodesInfo(List<String> codes) {

    public static CreateDemodayEntryCodesInfo from(List<String> rawCodes) {
        return new CreateDemodayEntryCodesInfo(List.copyOf(rawCodes));
    }
}
