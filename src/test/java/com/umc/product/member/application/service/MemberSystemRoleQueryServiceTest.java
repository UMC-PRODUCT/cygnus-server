package com.umc.product.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.member.application.port.in.query.dto.MemberSystemRoleInfo;
import com.umc.product.member.application.port.out.LoadMemberSystemRolePort;
import com.umc.product.member.domain.MemberSystemRole;
import com.umc.product.member.domain.MemberSystemRoleType;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberSystemRoleQueryService")
class MemberSystemRoleQueryServiceTest {

    private static final Long MEMBER_ID = 1L;

    @Mock
    LoadMemberSystemRolePort loadMemberSystemRolePort;

    @InjectMocks
    MemberSystemRoleQueryService sut;

    @Test
    @DisplayName("memberId가 null이면 빈 목록을 반환하고 포트를 호출하지 않는다")
    void memberId가_null이면_빈_목록을_반환한다() {
        List<MemberSystemRoleInfo> result = sut.listByMemberId(null);

        assertThat(result).isEmpty();
        verifyNoInteractions(loadMemberSystemRolePort);
    }

    @Test
    @DisplayName("member system role을 조회 DTO로 변환한다")
    void member_system_role을_조회_DTO로_변환한다() {
        given(loadMemberSystemRolePort.listByMemberId(MEMBER_ID)).willReturn(List.of(
            MemberSystemRole.create(MEMBER_ID, MemberSystemRoleType.SUPER_ADMIN)
        ));

        List<MemberSystemRoleInfo> result = sut.listByMemberId(MEMBER_ID);

        assertThat(result).containsExactly(
            new MemberSystemRoleInfo(MEMBER_ID, "SUPER_ADMIN")
        );
    }
}
