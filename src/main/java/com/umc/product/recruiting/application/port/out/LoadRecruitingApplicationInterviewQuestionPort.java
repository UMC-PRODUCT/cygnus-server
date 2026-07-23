package com.umc.product.recruiting.application.port.out;

import java.util.List;

import com.umc.product.recruiting.domain.RecruitingApplicationInterviewQuestion;

public interface LoadRecruitingApplicationInterviewQuestionPort {

    RecruitingApplicationInterviewQuestion getById(Long id);

    List<RecruitingApplicationInterviewQuestion> listByApplicationId(Long applicationId);

    List<RecruitingApplicationInterviewQuestion> listActiveByApplicationId(Long applicationId);
}
