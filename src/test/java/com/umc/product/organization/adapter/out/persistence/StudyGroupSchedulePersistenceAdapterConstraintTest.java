package com.umc.product.organization.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.umc.product.organization.domain.StudyGroupSchedule;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

@ExtendWith(MockitoExtension.class)
class StudyGroupSchedulePersistenceAdapterConstraintTest {

    @Mock
    private StudyGroupScheduleJpaRepository jpaRepository;

    @Test
    void 동일_그룹_주차_일정_UNIQUE는_명시적인_409로_변환한다() {
        StudyGroupSchedule schedule = mock(StudyGroupSchedule.class);
        DataIntegrityViolationException duplicate = constraintViolation(
            "uk_study_group_schedule_group_week"
        );
        when(jpaRepository.saveAndFlush(schedule)).thenThrow(duplicate);

        StudyGroupSchedulePersistenceAdapter sut = new StudyGroupSchedulePersistenceAdapter(jpaRepository);

        assertThatThrownBy(() -> sut.save(schedule))
            .isInstanceOfSatisfying(OrganizationDomainException.class, exception ->
                assertThat(exception.getBaseCode())
                    .isEqualTo(OrganizationErrorCode.STUDY_GROUP_SCHEDULE_ALREADY_EXISTS));
    }

    @Test
    void 일정의_다른_무결성_오류는_중복으로_오분류하지_않는다() {
        StudyGroupSchedule schedule = mock(StudyGroupSchedule.class);
        DataIntegrityViolationException unrelated = constraintViolation("some_other_constraint");
        when(jpaRepository.saveAndFlush(schedule)).thenThrow(unrelated);

        StudyGroupSchedulePersistenceAdapter sut = new StudyGroupSchedulePersistenceAdapter(jpaRepository);

        assertThatThrownBy(() -> sut.save(schedule)).isSameAs(unrelated);
    }

    private DataIntegrityViolationException constraintViolation(String constraintName) {
        ConstraintViolationException cause = new ConstraintViolationException(
            "constraint violation",
            new SQLException(),
            constraintName
        );
        return new DataIntegrityViolationException("data integrity violation", cause);
    }
}
