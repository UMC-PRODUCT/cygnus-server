package com.umc.product.demoday.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.sql.SQLException;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayStamp;
import com.umc.product.demoday.domain.DemodayVote;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

@ExtendWith(MockitoExtension.class)
class DemodayPersistenceAdapterConstraintTranslationTest {

    @Mock
    private DemodayVoteJpaRepository voteRepository;

    @Mock
    private DemodayBoothJpaRepository boothRepository;

    @Mock
    private DemodayVoteQueryRepository voteQueryRepository;

    @Mock
    private DemodayStampJpaRepository stampRepository;

    @Mock
    private DemodayVote vote;

    @Mock
    private DemodayStamp stamp;

    @Mock
    private DemodayBooth booth;

    private DemodayBoothPersistenceAdapter boothAdapter;
    private DemodayVotePersistenceAdapter voteAdapter;
    private DemodayStampPersistenceAdapter stampAdapter;

    @BeforeEach
    void setUp() {
        boothAdapter = new DemodayBoothPersistenceAdapter(boothRepository);
        voteAdapter = new DemodayVotePersistenceAdapter(voteRepository, voteQueryRepository);
        stampAdapter = new DemodayStampPersistenceAdapter(stampRepository);
    }

    @Test
    @DisplayName("같은 Poll의 부스 코드 중복 제약 위반을 중복 코드 도메인 오류로 변환한다")
    void translateDuplicateBoothCodeConstraint() {
        // Given
        DataIntegrityViolationException exception = constraintViolation("uk_demoday_booth_poll_code");
        given(boothRepository.saveAndFlush(any(DemodayBooth.class))).willThrow(exception);

        // When & Then
        assertThatThrownBy(() -> boothAdapter.save(booth))
            .isInstanceOfSatisfying(DemodayDomainException.class, domainException -> {
                assertThat(domainException.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_BOOTH_CODE_DUPLICATED);
                assertThat(domainException.getCause()).isSameAs(exception);
            });
    }

    @Test
    @DisplayName("회원 중복 투표 제약 위반을 이미 투표한 도메인 오류로 변환한다")
    void translateDuplicateMemberVoteConstraint() {
        assertVoteConstraintTranslation("uk_demoday_vote_poll_member", DemodayErrorCode.DEMODAY_VOTE_ALREADY_CAST);
    }

    @Test
    @DisplayName("방문자 중복 투표 제약 위반을 이미 투표한 도메인 오류로 변환한다")
    void translateDuplicateVisitorVoteConstraint() {
        assertVoteConstraintTranslation("uk_demoday_vote_entry_code", DemodayErrorCode.DEMODAY_VOTE_ALREADY_CAST);
    }

    @Test
    @DisplayName("회원 중복 스탬프 제약 위반을 이미 수집한 도메인 오류로 변환한다")
    void translateDuplicateMemberStampConstraint() {
        assertStampConstraintTranslation(
            "uk_demoday_stamp_member_booth",
            DemodayErrorCode.DEMODAY_STAMP_ALREADY_COLLECTED
        );
    }

    @Test
    @DisplayName("방문자 중복 스탬프 제약 위반을 이미 수집한 도메인 오류로 변환한다")
    void translateDuplicateVisitorStampConstraint() {
        assertStampConstraintTranslation(
            "uk_demoday_stamp_entry_code_booth",
            DemodayErrorCode.DEMODAY_STAMP_ALREADY_COLLECTED
        );
    }

    @Test
    @DisplayName("알 수 없는 투표 무결성 오류는 원래 예외를 유지한다")
    void preserveUnknownVoteIntegrityViolation() {
        // Given
        DataIntegrityViolationException exception = constraintViolation("unknown_vote_constraint");
        given(voteRepository.saveAndFlush(any(DemodayVote.class))).willThrow(exception);

        // When & Then
        assertThatThrownBy(() -> voteAdapter.save(vote)).isSameAs(exception);
    }

    @Test
    @DisplayName("알 수 없는 스탬프 무결성 오류는 원래 예외를 유지한다")
    void preserveUnknownStampIntegrityViolation() {
        // Given
        DataIntegrityViolationException exception = constraintViolation("unknown_stamp_constraint");
        given(stampRepository.saveAndFlush(any(DemodayStamp.class))).willThrow(exception);

        // When & Then
        assertThatThrownBy(() -> stampAdapter.save(stamp)).isSameAs(exception);
    }

    @Test
    @DisplayName("알 수 없는 부스 무결성 오류는 원래 예외를 유지한다")
    void preserveUnknownBoothIntegrityViolation() {
        // Given
        DataIntegrityViolationException exception = constraintViolation("unknown_booth_constraint");
        given(boothRepository.saveAndFlush(any(DemodayBooth.class))).willThrow(exception);

        // When & Then
        assertThatThrownBy(() -> boothAdapter.save(booth)).isSameAs(exception);
    }

    private void assertVoteConstraintTranslation(String constraintName, DemodayErrorCode errorCode) {
        // Given
        DataIntegrityViolationException exception = constraintViolation(constraintName);
        given(voteRepository.saveAndFlush(any(DemodayVote.class))).willThrow(exception);

        // When & Then
        assertThatThrownBy(() -> voteAdapter.save(vote))
            .isInstanceOfSatisfying(DemodayDomainException.class, domainException -> {
                assertThat(domainException.getBaseCode()).isEqualTo(errorCode);
                assertThat(domainException.getCause()).isSameAs(exception);
            });
    }

    private void assertStampConstraintTranslation(String constraintName, DemodayErrorCode errorCode) {
        // Given
        DataIntegrityViolationException exception = constraintViolation(constraintName);
        given(stampRepository.saveAndFlush(any(DemodayStamp.class))).willThrow(exception);

        // When & Then
        assertThatThrownBy(() -> stampAdapter.save(stamp))
            .isInstanceOfSatisfying(DemodayDomainException.class, domainException -> {
                assertThat(domainException.getBaseCode()).isEqualTo(errorCode);
                assertThat(domainException.getCause()).isSameAs(exception);
            });
    }

    private DataIntegrityViolationException constraintViolation(String constraintName) {
        ConstraintViolationException cause = new ConstraintViolationException(
            "duplicate key",
            new SQLException("duplicate key"),
            constraintName
        );
        return new DataIntegrityViolationException("constraint violation", cause);
    }
}
