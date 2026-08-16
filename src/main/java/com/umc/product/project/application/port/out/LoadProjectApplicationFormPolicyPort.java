package com.umc.product.project.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.umc.product.project.domain.ProjectApplicationFormPolicy;

/**
 * ProjectApplicationFormPolicy 조회 Port (Driven / Port Out).
 */
public interface LoadProjectApplicationFormPolicyPort {

    /**
     * 특정 지원 폼에 속한 모든 섹션 정책을 조회합니다.
     */
    List<ProjectApplicationFormPolicy> listByApplicationFormId(Long applicationFormId);

    /**
     * 여러 지원 폼의 섹션 정책을 한 번에 조회합니다.
     *
     * @return applicationFormId -> 정책 목록
     */
    Map<Long, List<ProjectApplicationFormPolicy>> listByApplicationFormIds(Collection<Long> applicationFormIds);
}
