package com.umc.product.inhouse.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.global.exception.BusinessException;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberAccountPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberPort;
import com.umc.product.inhouse.domain.UmcProductMember;
import com.umc.product.inhouse.domain.UmcProductMemberAccount;
import com.umc.product.inhouse.domain.enums.UmcProductMemberAccountType;
import com.umc.product.inhouse.exception.InhouseErrorCode;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;

@ExtendWith(MockitoExtension.class)
class UmcProductMemberAccountQueryServiceTest {

    @Mock
    LoadUmcProductMemberPort loadUmcProductMemberPort;
    @Mock
    LoadUmcProductMemberAccountPort loadUmcProductMemberAccountPort;
    @Mock
    GetMemberUseCase getMemberUseCase;
    @Mock
    UmcProductAccessPolicy umcProductAccessPolicy;

    @InjectMocks
    UmcProductMemberAccountQueryService sut;

    @Test
    void 연동_계정_목록은_계정_유형과_실제_이메일을_반환한다() {
        UmcProductMember member = member(30L);
        UmcProductMemberAccount account = UmcProductMemberAccount.create(
            member, 500L, UmcProductMemberAccountType.PROVISIONED
        );
        MemberInfo memberInfo = memberInfo(500L, "jeong@university.neordinary.com");
        given(umcProductAccessPolicy.canManageUmcProduct(1L)).willReturn(true);
        given(loadUmcProductMemberPort.getById(30L)).willReturn(member);
        given(loadUmcProductMemberAccountPort.listByUmcProductMemberId(30L)).willReturn(List.of(account));
        given(getMemberUseCase.findAllByIds(Set.of(500L))).willReturn(Map.of(500L, memberInfo));

        assertThat(sut.listAccounts(1L, 30L)).singleElement().satisfies(info -> {
            assertThat(info.email()).isEqualTo("jeong@university.neordinary.com");
            assertThat(info.accountType()).isEqualTo(UmcProductMemberAccountType.PROVISIONED);
        });
    }

    @Test
    void 후보_이메일은_trim과_소문자_정규화_후_정확히_조회한다() {
        MemberInfo memberInfo = memberInfo(200L, "linked@example.com");
        given(umcProductAccessPolicy.canManageUmcProduct(1L)).willReturn(true);
        given(getMemberUseCase.findByEmail("linked@example.com")).willReturn(Optional.of(memberInfo));
        given(loadUmcProductMemberAccountPort.existsByMemberId(200L)).willReturn(true);

        assertThat(sut.findCandidateByEmail(1L, " Linked@Example.com "))
            .get()
            .satisfies(candidate -> assertThat(candidate.alreadyLinked()).isTrue());
    }

    @Test
    void 관리_권한이_없으면_계정_정보를_조회할_수_없다() {
        given(umcProductAccessPolicy.canManageUmcProduct(1L)).willReturn(false);

        assertThatThrownBy(() -> sut.listAccounts(1L, 30L))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(InhouseErrorCode.UMC_PRODUCT_ACCESS_DENIED);
    }

    private UmcProductMember member(Long id) {
        UmcProductMember member = UmcProductMember.create("정의찬", "제옹", null, null, null);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private MemberInfo memberInfo(Long id, String email) {
        return new MemberInfo(
            id,
            "정의찬",
            "제옹",
            email,
            null,
            null,
            null,
            null,
            MemberStatus.ACTIVE,
            List.of()
        );
    }
}
