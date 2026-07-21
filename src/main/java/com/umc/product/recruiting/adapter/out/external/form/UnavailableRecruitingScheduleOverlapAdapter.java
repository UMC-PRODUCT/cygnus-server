package com.umc.product.recruiting.adapter.out.external.form;

import java.util.List;

import org.springframework.stereotype.Component;

import com.umc.product.global.exception.NotImplementedException;
import com.umc.product.recruiting.application.port.out.FindRecruitingScheduleOverlapPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingInterviewScheduleCandidate;

@Component
public class UnavailableRecruitingScheduleOverlapAdapter implements FindRecruitingScheduleOverlapPort {

    @Override
    public List<RecruitingInterviewScheduleCandidate> findOverlaps(Long formId, List<Long> formResponseIds) {
        // TODO(#1146): Form 일정 교집합 Query UseCase가 제공되면 이 unavailable adapter를 실제 연동 adapter로 교체한다.
        throw new NotImplementedException("지원자 가능 일정 겹침 계산은 Form 일정 질문 PR이 병합된 뒤 사용할 수 있어요.");
    }
}
