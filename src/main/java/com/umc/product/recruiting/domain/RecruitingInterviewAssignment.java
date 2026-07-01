package com.umc.product.recruiting.domain;

import java.time.Instant;

import com.umc.product.common.BaseEntity;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewAssignmentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "recruiting_interview_assignment",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_recruiting_interview_assignment_application_interviewer",
        columnNames = {"recruiting_application_id", "interviewer_member_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitingInterviewAssignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiting_application_id", nullable = false)
    private RecruitingApplication application;

    @Column(nullable = false, name = "interviewer_member_id")
    private Long interviewerMemberId;

    @Column(nullable = false, name = "starts_at")
    private Instant startsAt;

    @Column(nullable = false, name = "ends_at")
    private Instant endsAt;

    @Column(length = 255)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitingInterviewAssignmentStatus status;

    @Builder(access = AccessLevel.PRIVATE)
    private RecruitingInterviewAssignment(
        RecruitingApplication application,
        Long interviewerMemberId,
        Instant startsAt,
        Instant endsAt,
        String location
    ) {
        this.application = application;
        this.interviewerMemberId = interviewerMemberId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.location = location;
        this.status = RecruitingInterviewAssignmentStatus.ASSIGNED;
    }

    public static RecruitingInterviewAssignment assign(
        RecruitingApplication application,
        Long interviewerMemberId,
        Instant startsAt,
        Instant endsAt,
        String location
    ) {
        return RecruitingInterviewAssignment.builder()
            .application(application)
            .interviewerMemberId(interviewerMemberId)
            .startsAt(startsAt)
            .endsAt(endsAt)
            .location(location)
            .build();
    }
}
