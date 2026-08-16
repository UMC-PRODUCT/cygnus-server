package com.umc.product.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.member.application.port.out.LoadMemberPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberExistenceQueryService")
class MemberExistenceQueryServiceTest {

    @Mock
    LoadMemberPort loadMemberPort;

    @Test
    @DisplayName("회원 ID가 없으면 저장소를 조회하지 않고 존재하지 않는 것으로 판단한다")
    void null_member_id_does_not_exist() {
        MemberExistenceQueryService sut = new MemberExistenceQueryService(loadMemberPort);

        assertThat(sut.existsById(null)).isFalse();
        verifyNoInteractions(loadMemberPort);
    }

    @Test
    @DisplayName("회원 저장소에서 회원 존재 여부를 확인한다")
    void checks_member_existence() {
        MemberExistenceQueryService sut = new MemberExistenceQueryService(loadMemberPort);
        given(loadMemberPort.existsById(10L)).willReturn(true);

        assertThat(sut.existsById(10L)).isTrue();
    }
}
