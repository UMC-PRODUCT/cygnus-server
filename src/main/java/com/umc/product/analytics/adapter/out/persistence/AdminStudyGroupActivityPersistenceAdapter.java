package com.umc.product.analytics.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.umc.product.analytics.application.port.in.query.dto.AdminStudyGroupActivityInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminStudyGroupListInfo;
import com.umc.product.analytics.application.port.out.LoadAdminStudyGroupActivityPort;
import com.umc.product.analytics.application.port.out.LoadAdminStudyGroupListPort;
import com.umc.product.analytics.domain.AdminAnalyticsScope;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminStudyGroupActivityPersistenceAdapter implements
    LoadAdminStudyGroupListPort,
    LoadAdminStudyGroupActivityPort {

    private final AdminStudyGroupActivityQueryRepository queryRepository;

    @Override
    public AdminStudyGroupListInfo getStudyGroupList(AdminAnalyticsScope scope) {
        return queryRepository.getStudyGroupList(scope);
    }

    @Override
    public AdminStudyGroupActivityInfo getStudyGroupActivity(AdminAnalyticsScope scope) {
        return queryRepository.getStudyGroupActivity(scope);
    }
}
