package com.umc.product.recruiting.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingRoundGraphQlResponse;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

@ExtendWith(MockitoExtension.class)
class RecruitingManagementFieldResolverTest {

    @Mock
    RecruitingGraphQlPermissionSupport permissionSupport;

    @InjectMocks
    RecruitingAdminGraphQlController sut;

    @Test
    @DisplayName("round management는 권한이 없으면 base resource를 유지하고 null을 반환한다")
    void round_management는_권한이_없으면_null을_반환한다() {
        RecruitingRoundGraphQlResponse round = round();
        given(permissionSupport.nullableCurrentMemberId()).willReturn(10L);
        given(permissionSupport.hasRecruitmentPermission(10L, 20L, PermissionType.READ)).willReturn(false);

        assertThat(sut.roundManagement(round)).isNull();
    }

    @Test
    @DisplayName("round management는 READ 권한이 있으면 관리 필드를 반환한다")
    void round_management는_READ_권한이_있으면_관리_필드를_반환한다() {
        RecruitingRoundGraphQlResponse round = round();
        given(permissionSupport.nullableCurrentMemberId()).willReturn(10L);
        given(permissionSupport.hasRecruitmentPermission(10L, 20L, PermissionType.READ)).willReturn(true);

        assertThat(sut.roundManagement(round).roundId()).isEqualTo(1L);
    }

    private static RecruitingRoundGraphQlResponse round() {
        return new RecruitingRoundGraphQlResponse(
            1L,
            20L,
            30L,
            40L,
            "정규 모집",
            RecruitingRoundType.REGULAR,
            1,
            RecruitingRoundStatus.OPEN,
            List.of(),
            false,
            null,
            null,
            null,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            null
        );
    }
}
