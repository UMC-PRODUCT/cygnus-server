package com.umc.product.recruiting.application.port.out;

import java.util.Collection;
import java.util.List;

import com.umc.product.recruiting.domain.RecruitingApplication;

public interface SaveRecruitingApplicationPort {

    RecruitingApplication save(RecruitingApplication application);

    List<RecruitingApplication> saveAll(Collection<RecruitingApplication> applications);
}
