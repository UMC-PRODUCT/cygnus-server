package com.umc.product.curriculum.application.service.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SystemRoleType;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.curriculum.application.port.out.LoadCurriculumPort;
import com.umc.product.curriculum.application.port.out.LoadWeeklyCurriculumPort;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

@ExtendWith(MockitoExtension.class)
class CurriculumPermissionEvaluatorTest {

    @Mock private LoadCurriculumPort loadCurriculumPort;
    @Mock private LoadWeeklyCurriculumPort loadWeeklyCurriculumPort;
    @Mock private GetGisuUseCase getGisuUseCase;
    @InjectMocks private CurriculumPermissionEvaluator evaluator;

    @Test
    @DisplayName("커리큘럼 생성은 요청 기수의 중앙 운영진에게 허용된다")
    void 생성_기수의_중앙운영진은_허용한다() {
        // given
        given(getGisuUseCase.getById(9L)).willReturn(new GisuInfo(9L, 9L, Instant.EPOCH, Instant.MAX, true));

        // when / then
        assertThat(evaluator.evaluate(central(9L), permission("gisu:9"))).isTrue();
        assertThat(evaluator.evaluate(central(8L), permission("gisu:9"))).isFalse();
    }

    @Test
    @DisplayName("주차 ID는 같은 숫자의 커리큘럼 ID와 혼동하지 않는다")
    void 주차_문맥으로_상위_기수를_확인한다() {
        // given
        Curriculum curriculum = Curriculum.create(9L, ChallengerPart.WEB, "웹");
        ReflectionTestUtils.setField(curriculum, "id", 20L);
        WeeklyCurriculum weekly = WeeklyCurriculum.create(
            curriculum, 1L, false, "1주차", Instant.EPOCH, Instant.MAX);
        given(loadWeeklyCurriculumPort.getById(1L)).willReturn(weekly);

        // when / then
        assertThat(evaluator.evaluate(central(9L), permission("weekly:1"))).isTrue();
        assertThat(evaluator.evaluate(central(8L), permission("weekly:1"))).isFalse();
        verifyNoInteractions(loadCurriculumPort);
    }

    @Test
    @DisplayName("학교 운영진은 커리큘럼을 관리하지 못한다")
    void 학교운영진은_거부한다() {
        // given
        given(loadCurriculumPort.findById(1L))
            .willReturn(Optional.of(Curriculum.create(9L, ChallengerPart.WEB, "웹")));
        SubjectAttributes subject = SubjectAttributes.builder().memberId(2L)
            .roleAttributes(List.of(new RoleAttribute(
                ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, 10L, null, 9L)))
            .build();

        // when / then
        assertThat(evaluator.evaluate(subject, permission("1"))).isFalse();
    }

    @Test
    @DisplayName("SUPER_ADMIN은 기수 중앙 역할 없이도 커리큘럼을 삭제할 수 있다")
    void 시스템관리자는_기수와_무관하게_허용한다() {
        // given
        given(loadCurriculumPort.findById(1L))
            .willReturn(Optional.of(Curriculum.create(9L, ChallengerPart.WEB, "웹")));
        SubjectAttributes subject = SubjectAttributes.builder().memberId(2L)
            .systemRoles(Set.of(SystemRoleType.SUPER_ADMIN)).build();

        // when / then
        assertThat(evaluator.evaluate(subject,
            ResourcePermission.of(ResourceType.CURRICULUM, 1L, PermissionType.DELETE))).isTrue();
    }

    @Test
    @DisplayName("없는 커리큘럼이나 명확하지 않은 리소스 문맥은 거부한다")
    void 리소스가_없거나_잘못된_경우_거부한다() {
        // given / when / then
        assertThat(evaluator.evaluate(central(9L), permission("999"))).isFalse();
        assertThat(evaluator.evaluate(central(9L), permission("weekly:invalid"))).isFalse();
        assertThat(evaluator.evaluate(central(9L),
            ResourcePermission.ofType(ResourceType.CURRICULUM, PermissionType.WRITE))).isFalse();
    }

    private ResourcePermission permission(String resourceId) {
        return ResourcePermission.of(ResourceType.CURRICULUM, resourceId, PermissionType.WRITE);
    }

    private SubjectAttributes central(Long gisuId) {
        return SubjectAttributes.builder().memberId(2L)
            .roleAttributes(List.of(new RoleAttribute(
                ChallengerRoleType.CENTRAL_EDUCATION_TEAM_MEMBER, OrganizationType.CENTRAL, 1L, null, gisuId)))
            .build();
    }
}
