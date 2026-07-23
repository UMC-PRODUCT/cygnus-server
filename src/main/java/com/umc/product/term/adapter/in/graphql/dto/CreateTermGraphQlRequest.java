package com.umc.product.term.adapter.in.graphql.dto;

import com.umc.product.term.application.port.in.command.dto.CreateTermCommand;
import com.umc.product.term.domain.enums.TermType;

public record CreateTermGraphQlRequest(
    String link,
    boolean mandatory,
    TermType type
) {

    public CreateTermCommand toCommand() {
        return CreateTermCommand.builder()
            .link(link)
            .required(mandatory)
            .type(type)
            .build();
    }
}
