package com.umc.product.recruiting.application.port.out;

import com.umc.product.recruiting.domain.RecruitingApplicationForm;

public interface SaveRecruitingApplicationFormPort {

    RecruitingApplicationForm save(RecruitingApplicationForm applicationForm);

    void delete(RecruitingApplicationForm applicationForm);
}
