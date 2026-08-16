package com.umc.product.recruiting.application.port.out;

import com.umc.product.recruiting.domain.RecruitingApplicationInterviewQuestion;

public interface SaveRecruitingApplicationInterviewQuestionPort {

    RecruitingApplicationInterviewQuestion save(RecruitingApplicationInterviewQuestion question);
}
