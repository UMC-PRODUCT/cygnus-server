package com.umc.product.curriculum.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.application.port.in.query.dto.GetWorkbookSubmissionsQuery;
import com.umc.product.global.exception.NotImplementedException;

@DisplayName("WorkbookSubmission QueryDSL 조건")
class WorkbookSubmissionQueryRepositoryTest {

    @Test
    @DisplayName("미구현 조회와 필수 week 조건은 명시적인 예외를 반환한다")
    void explicit_not_implemented_paths() {
        WorkbookSubmissionQueryRepository sut = sut();
        var query = new GetWorkbookSubmissionsQuery(null, 1, null, null, null, 20);

        assertThatThrownBy(() -> sut.findSubmissions(query)).isInstanceOf(NotImplementedException.class);
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(sut, "weekNoEq", 1))
            .isInstanceOf(NotImplementedException.class);
    }

    @Test
    @DisplayName("nullable 동적 filter는 미제공 시 null, 제공 시 BooleanExpression을 반환한다")
    void nullable_filter_boundaries() {
        WorkbookSubmissionQueryRepository sut = sut();

        assertThat((Object) ReflectionTestUtils.invokeMethod(sut, "schoolIdEq", (Long) null)).isNull();
        assertThat((Object) ReflectionTestUtils.invokeMethod(sut, "schoolIdEq", 1L)).isNotNull();
        assertThat((Object) ReflectionTestUtils.invokeMethod(sut, "studyGroupIdEq", (Long) null)).isNull();
        assertThat((Object) ReflectionTestUtils.invokeMethod(sut, "studyGroupIdEq", 2L)).isNotNull();
        assertThat((Object) ReflectionTestUtils.invokeMethod(sut, "partEq", (ChallengerPart) null)).isNull();
        assertThat((Object) ReflectionTestUtils.invokeMethod(sut, "partEq", ChallengerPart.SPRINGBOOT)).isNotNull();
        assertThat((Object) ReflectionTestUtils.invokeMethod(sut, "cursorGt", (Long) null)).isNull();
        assertThat((Object) ReflectionTestUtils.invokeMethod(sut, "cursorGt", 3L)).isNotNull();
    }

    private WorkbookSubmissionQueryRepository sut() {
        return new WorkbookSubmissionQueryRepository(mock(JPAQueryFactory.class));
    }
}
