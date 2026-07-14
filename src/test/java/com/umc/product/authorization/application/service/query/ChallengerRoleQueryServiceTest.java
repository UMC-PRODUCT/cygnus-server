package com.umc.product.authorization.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleBasicInfo;
import com.umc.product.authorization.application.port.out.LoadChallengerRolePort;
import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;

@ExtendWith(MockitoExtension.class)
class ChallengerRoleQueryServiceTest {

    private static final Long MEMBER_ID = 1L;

    @Mock
    LoadChallengerRolePort loadChallengerRolePort;

    @Mock
    GetGisuUseCase getGisuUseCase;

    @InjectMocks
    ChallengerRoleQueryService service;

    @Test
    @DisplayName("경량 역할 조회는 역할 범위만 매핑하고 기수 상세를 조회하지 않는다")
    void 경량_역할_조회는_기수_상세를_조회하지_않는다() {
        // given
        ChallengerRole centralRole = ChallengerRole.create(
            10L, ChallengerRoleType.CENTRAL_PRESIDENT, null, null, 3L
        );
        ChallengerRole schoolRole = ChallengerRole.create(
            11L, ChallengerRoleType.SCHOOL_VICE_PRESIDENT, 7L, null, 4L
        );
        given(loadChallengerRolePort.findByMemberId(MEMBER_ID)).willReturn(List.of(centralRole, schoolRole));

        // when
        List<ChallengerRoleBasicInfo> result = service.findAllBasicByMemberId(MEMBER_ID);

        // then
        assertThat(result).containsExactly(
            new ChallengerRoleBasicInfo(ChallengerRoleType.CENTRAL_PRESIDENT, OrganizationType.CENTRAL, null),
            new ChallengerRoleBasicInfo(ChallengerRoleType.SCHOOL_VICE_PRESIDENT, OrganizationType.SCHOOL, 7L)
        );
        then(loadChallengerRolePort).should().findByMemberId(MEMBER_ID);
        then(loadChallengerRolePort).shouldHaveNoMoreInteractions();
        then(getGisuUseCase).shouldHaveNoInteractions();
    }
}
