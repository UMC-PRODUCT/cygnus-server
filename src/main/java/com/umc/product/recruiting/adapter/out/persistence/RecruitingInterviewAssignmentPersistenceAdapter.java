package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewAssignmentPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingInterviewAssignmentPort;
import com.umc.product.recruiting.domain.RecruitingInterviewAssignment;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingInterviewAssignmentPersistenceAdapter
    implements LoadRecruitingInterviewAssignmentPort, SaveRecruitingInterviewAssignmentPort {

    private final RecruitingInterviewAssignmentJpaRepository recruitingInterviewAssignmentJpaRepository;

    @Override
    public Optional<RecruitingInterviewAssignment> findAssignmentById(Long id) {
        return recruitingInterviewAssignmentJpaRepository.findById(id);
    }

    @Override
    public RecruitingInterviewAssignment getAssignmentById(Long id) {
        return findAssignmentById(id)
            .orElseThrow(() -> new RecruitingDomainException(
                RecruitingErrorCode.RECRUITING_INTERVIEW_ASSIGNMENT_NOT_FOUND));
    }

    @Override
    public Optional<RecruitingInterviewAssignment> findByApplicationIdAndInterviewerMemberId(
        Long applicationId,
        Long interviewerMemberId
    ) {
        return recruitingInterviewAssignmentJpaRepository.findByApplication_IdAndInterviewerMemberId(
            applicationId,
            interviewerMemberId
        );
    }

    @Override
    public List<RecruitingInterviewAssignment> listByApplicationId(Long applicationId) {
        return recruitingInterviewAssignmentJpaRepository.findAllByApplication_IdOrderByStartsAtAscIdAsc(applicationId);
    }

    @Override
    public List<RecruitingInterviewAssignment> listByInterviewerMemberId(Long interviewerMemberId) {
        return recruitingInterviewAssignmentJpaRepository
            .findAllByInterviewerMemberIdOrderByStartsAtAscIdAsc(interviewerMemberId);
    }

    @Override
    public RecruitingInterviewAssignment saveAssignment(RecruitingInterviewAssignment assignment) {
        return recruitingInterviewAssignmentJpaRepository.save(assignment);
    }
}
