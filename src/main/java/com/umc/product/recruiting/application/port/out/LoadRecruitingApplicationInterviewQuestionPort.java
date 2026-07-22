package com.umc.product.recruiting.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.umc.product.recruiting.domain.RecruitingApplicationInterviewQuestion;

public interface LoadRecruitingApplicationInterviewQuestionPort {

    RecruitingApplicationInterviewQuestion getById(Long id);

    List<RecruitingApplicationInterviewQuestion> listByApplicationId(Long applicationId);

    List<RecruitingApplicationInterviewQuestion> listActiveByApplicationId(Long applicationId);

    Map<Long, List<RecruitingApplicationInterviewQuestion>> listActiveByApplicationIds(Set<Long> applicationIds);
}
