package com.umc.product.recruiting.adapter.out.id;

import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.IssueRecruitingApplicationNoPort;

@Component
public class RecruitingApplicationNoGenerator implements IssueRecruitingApplicationNoPort {

    private static final int TOKEN_LENGTH = 12;

    @Override
    public String issue() {
        String token = UUID.randomUUID()
            .toString()
            .replace("-", "")
            .substring(0, TOKEN_LENGTH)
            .toUpperCase(Locale.ROOT);
        return "APP-" + token;
    }
}
