package com.umc.product.curriculum.application.service.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;

@ExtendWith(MockitoExtension.class)
class ChallengerWorkbookPermissionEvaluatorTest {

    @Mock private LoadChallengerWorkbookPort loadChallengerWorkbookPort;
    @Mock private CurriculumStudyGroupStaffPolicy staffPolicy;
    @InjectMocks private ChallengerWorkbookPermissionEvaluator evaluator;

    @Test
    @DisplayName("리소스 ID 없는 워크북 타입 단위 권한은 예외 없이 false를 반환한다")
    void typePermissionWithoutResourceIdFailsClosed() {
        boolean allowed = evaluator.evaluate(
            SubjectAttributes.builder().memberId(30L).build(),
            ResourcePermission.ofType(ResourceType.CHALLENGER_WORKBOOK, PermissionType.WRITE)
        );

        assertThat(allowed).isFalse();
    }
}
