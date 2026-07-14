package com.umc.product.member.application.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.audit.application.port.in.command.RecordAuditLogUseCase;
import com.umc.product.authentication.application.port.in.command.OAuthAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.query.GetMemberOAuthUseCase;
import com.umc.product.authorization.application.port.in.command.EvictAuthoritySnapshotCacheUseCase;
import com.umc.product.member.application.port.in.command.dto.DeleteMemberCommand;
import com.umc.product.member.application.port.out.LoadMemberPort;
import com.umc.product.member.application.port.out.SaveMemberPort;
import com.umc.product.member.domain.Member;
import com.umc.product.notification.application.port.in.SendWebhookAlarmUseCase;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.term.application.port.in.command.ManageTermAgreementUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 권한 캐시")
class MemberServiceAuthorityCacheTest {

    private static final Long MEMBER_ID = 10L;
    private static final Long SCHOOL_ID = 20L;

    @Mock
    LoadMemberPort loadMemberPort;

    @Mock
    SaveMemberPort saveMemberPort;

    @Mock
    MemberRegistrationValidator registrationValidator;

    @Mock
    OAuthAuthenticationUseCase oAuthAuthenticationUseCase;

    @Mock
    GetMemberOAuthUseCase getMemberOAuthUseCase;

    @Mock
    ManageTermAgreementUseCase manageTermAgreementUseCase;

    @Mock
    GetSchoolUseCase getSchoolUseCase;

    @Mock
    RecordAuditLogUseCase recordAuditLogUseCase;

    @Mock
    SendWebhookAlarmUseCase sendWebhookAlarmUseCase;

    @Mock
    EvictAuthoritySnapshotCacheUseCase evictAuthoritySnapshotCacheUseCase;

    @InjectMocks
    MemberService sut;

    @Test
    @DisplayName("회원 삭제가 완료되면 해당 회원의 권한 snapshot 캐시를 제거한다")
    void evict_authority_snapshot_after_member_delete() {
        Member member = Member.builder()
            .name("홍길동")
            .nickname("길동")
            .email("member@example.com")
            .schoolId(SCHOOL_ID)
            .build();
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        given(loadMemberPort.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(getMemberOAuthUseCase.getOAuthList(MEMBER_ID)).willReturn(List.of());
        given(getSchoolUseCase.getSchoolDetail(SCHOOL_ID)).willReturn(new SchoolDetailInfo(
            null, null, "테스트대학교", SCHOOL_ID, null, null, List.of(), true, null, null
        ));

        sut.deleteMember(DeleteMemberCommand.builder().memberId(MEMBER_ID).build());

        verify(saveMemberPort).delete(member);
        verify(evictAuthoritySnapshotCacheUseCase).evictByMemberId(MEMBER_ID);
    }
}
