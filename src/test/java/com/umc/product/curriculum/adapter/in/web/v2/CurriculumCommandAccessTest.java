package com.umc.product.curriculum.adapter.in.web.v2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.umc.product.authorization.adapter.in.aspect.AccessControlAspect;
import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.CreateCurriculumRequest;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.EditWeeklyCurriculumRequest;
import com.umc.product.curriculum.application.port.in.command.ManageCurriculumUseCase;
import com.umc.product.curriculum.application.port.in.command.ManageWeeklyCurriculumUseCase;
import com.umc.product.curriculum.application.port.out.LoadCurriculumPort;
import com.umc.product.curriculum.application.port.out.LoadWeeklyCurriculumPort;
import com.umc.product.curriculum.application.service.evaluator.CurriculumPermissionEvaluator;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

@ExtendWith(MockitoExtension.class)
class CurriculumCommandAccessTest {

    @Mock private ManageCurriculumUseCase manageCurriculumUseCase;
    @Mock private ManageWeeklyCurriculumUseCase manageWeeklyCurriculumUseCase;
    @Mock private LoadCurriculumPort loadCurriculumPort;
    @Mock private LoadWeeklyCurriculumPort loadWeeklyCurriculumPort;
    @Mock private GetGisuUseCase getGisuUseCase;
    @Mock private CheckPermissionUseCase checkPermissionUseCase;
    private CurriculumCommandV2Controller controller;
    private SubjectAttributes subject;

    @BeforeEach
    void 실제_컨트롤러와_권한_평가기의_AOP를_연결한다() {
        subject = central(9L);
        MemberPrincipal principal = MemberPrincipal.builder().memberId(99L).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        CurriculumPermissionEvaluator evaluator = new CurriculumPermissionEvaluator(
            loadCurriculumPort, loadWeeklyCurriculumPort, getGisuUseCase);
        given(checkPermissionUseCase.check(eq(99L), any(ResourcePermission.class)))
            .willAnswer(invocation -> evaluator.evaluate(subject, invocation.getArgument(1)));
        AspectJProxyFactory factory = new AspectJProxyFactory(
            new CurriculumCommandV2Controller(manageCurriculumUseCase, manageWeeklyCurriculumUseCase));
        factory.addAspect(new AccessControlAspect(checkPermissionUseCase));
        controller = factory.getProxy();
    }

    @AfterEach
    void 인증_문맥을_비운다() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 생성_API는_요청_기수의_중앙운영진에게만_도달한다() {
        // given
        given(getGisuUseCase.getById(9L)).willReturn(new GisuInfo(9L, 9L, Instant.EPOCH, Instant.MAX, true));
        given(manageCurriculumUseCase.create(any())).willReturn(10L);
        var request = new CreateCurriculumRequest(9L, ChallengerPart.WEB, "웹");

        // when / then
        assertThat(controller.createCurriculum(request)).isEqualTo(10L);
        subject = central(8L);
        assertThatThrownBy(() -> controller.createCurriculum(request))
            .isInstanceOf(AuthorizationDomainException.class);
        verify(manageCurriculumUseCase).create(any());
    }

    @Test
    void 주차_수정_API는_주차가_속한_기수로_권한을_확인한다() {
        // given
        WeeklyCurriculum weekly = WeeklyCurriculum.create(Curriculum.create(9L, ChallengerPart.WEB, "웹"),
            1L, false, "1주차", Instant.EPOCH, Instant.MAX);
        given(loadWeeklyCurriculumPort.getById(20L)).willReturn(weekly);
        var request = new EditWeeklyCurriculumRequest(null, null, "변경 제목", null, null);

        // when / then
        assertThatCode(() -> controller.editWeeklyCurriculum(20L, request)).doesNotThrowAnyException();
        subject = central(8L);
        assertThatThrownBy(() -> controller.editWeeklyCurriculum(20L, request))
            .isInstanceOf(AuthorizationDomainException.class);
        verify(manageWeeklyCurriculumUseCase).edit(any());
    }

    private SubjectAttributes central(Long gisuId) {
        return SubjectAttributes.builder().memberId(99L).roleAttributes(List.of(new RoleAttribute(
            ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER, OrganizationType.CENTRAL, 1L, null, gisuId)))
            .build();
    }
}
