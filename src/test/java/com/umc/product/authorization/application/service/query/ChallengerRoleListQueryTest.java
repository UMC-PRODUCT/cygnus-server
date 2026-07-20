package com.umc.product.authorization.application.service.query;

import static com.umc.product.support.fixture.AuthorizationFixture.중앙_역할;
import static com.umc.product.support.fixture.AuthorizationFixture.학교_역할;
import static com.umc.product.support.fixture.AuthorizationFixture.학교_파트장;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleBasicInfo;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.authorization.application.port.out.LoadChallengerRolePort;
import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.member.application.port.in.query.ListMemberSystemRoleUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengerRoleQueryService 역할 조회")
class ChallengerRoleListQueryTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long GISU_ID = 9L;
    private static final Long SCHOOL_ID = 30L;

    @Mock
    LoadChallengerRolePort loadChallengerRolePort;

    @Mock
    GetGisuUseCase getGisuUseCase;

    @Mock
    ListMemberSystemRoleUseCase listMemberSystemRoleUseCase;

    @Nested
    @DisplayName("상세와 경량 조회")
    class DetailAndBasicQuery {

        @Test
        @DisplayName("역할 ID 조회는 기수 정보까지 결합한다")
        void 역할_ID_조회는_기수_정보까지_결합한다() {
            ChallengerRole role = 학교_역할(ChallengerRoleType.SCHOOL_PRESIDENT, SCHOOL_ID, GISU_ID);
            given(loadChallengerRolePort.getById(1L)).willReturn(role);
            given(getGisuUseCase.getById(GISU_ID)).willReturn(gisu());

            ChallengerRoleInfo result = sut().getById(1L);

            assertThat(result.roleType()).isEqualTo(ChallengerRoleType.SCHOOL_PRESIDENT);
            assertThat(result.gisu()).isEqualTo(10L);
            assertThat(result.organizationId()).isEqualTo(SCHOOL_ID);
        }

        @Test
        @DisplayName("회원 역할 상세 목록은 각 역할의 기수를 결합한다")
        void 회원_역할_상세_목록은_각_기수를_결합한다() {
            ChallengerRole central = 중앙_역할(ChallengerRoleType.CENTRAL_PRESIDENT, GISU_ID);
            ChallengerRole school = 학교_역할(ChallengerRoleType.SCHOOL_PART_LEADER, SCHOOL_ID, GISU_ID);
            given(loadChallengerRolePort.findByMemberId(MEMBER_ID)).willReturn(List.of(central, school));
            given(getGisuUseCase.getById(GISU_ID)).willReturn(gisu());

            List<ChallengerRoleInfo> result = sut().listByMemberId(MEMBER_ID);

            assertThat(result)
                .extracting(ChallengerRoleInfo::roleType)
                .containsExactly(
                    ChallengerRoleType.CENTRAL_PRESIDENT,
                    ChallengerRoleType.SCHOOL_PART_LEADER
                );
            then(getGisuUseCase).should(org.mockito.Mockito.times(2)).getById(GISU_ID);
        }

        @Test
        @DisplayName("특정 기수 역할 목록은 저장소 필터 결과를 상세 정보로 변환한다")
        void 특정_기수_역할_목록을_상세_정보로_변환한다() {
            ChallengerRole role = 중앙_역할(ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER, GISU_ID);
            given(loadChallengerRolePort.findRolesByMemberIdAndGisuId(MEMBER_ID, GISU_ID))
                .willReturn(List.of(role));
            given(getGisuUseCase.getById(GISU_ID)).willReturn(gisu());

            List<ChallengerRoleInfo> result = sut().listByMemberIdAndGisuId(MEMBER_ID, GISU_ID);

            assertThat(result).singleElement()
                .extracting(ChallengerRoleInfo::roleType)
                .isEqualTo(ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER);
        }

        @Test
        @DisplayName("경량 목록은 기수 조회 없이 역할 범위만 반환한다")
        void 경량_목록은_기수_조회가_없다() {
            ChallengerRole role = 학교_역할(ChallengerRoleType.SCHOOL_VICE_PRESIDENT, SCHOOL_ID, GISU_ID);
            given(loadChallengerRolePort.findByMemberId(MEMBER_ID)).willReturn(List.of(role));

            List<ChallengerRoleBasicInfo> result = sut().listBasicByMemberId(MEMBER_ID);

            assertThat(result).containsExactly(new ChallengerRoleBasicInfo(
                ChallengerRoleType.SCHOOL_VICE_PRESIDENT,
                OrganizationType.SCHOOL,
                SCHOOL_ID
            ));
            then(getGisuUseCase).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("역할 타입과 담당 파트")
    class RoleTypeAndPartQuery {

        @Test
        @DisplayName("기수 역할 타입은 중복을 제거하고 최초 순서를 유지한다")
        void 기수_역할_타입은_중복을_제거한다() {
            given(loadChallengerRolePort.findRolesByMemberIdAndGisuId(MEMBER_ID, GISU_ID)).willReturn(List.of(
                중앙_역할(ChallengerRoleType.CENTRAL_PRESIDENT, GISU_ID),
                중앙_역할(ChallengerRoleType.CENTRAL_PRESIDENT, GISU_ID),
                학교_역할(ChallengerRoleType.SCHOOL_PART_LEADER, SCHOOL_ID, GISU_ID)
            ));

            List<ChallengerRoleType> result = sut().getAllRoleTypesByMemberIdAndGisuId(MEMBER_ID, GISU_ID);

            assertThat(result).containsExactly(
                ChallengerRoleType.CENTRAL_PRESIDENT,
                ChallengerRoleType.SCHOOL_PART_LEADER
            );
        }

        @Test
        @DisplayName("학교 역할 타입은 중앙과 다른 학교 역할을 제외하고 중복을 제거한다")
        void 학교_역할_타입은_학교_범위로_필터한다() {
            given(loadChallengerRolePort.findByMemberId(MEMBER_ID)).willReturn(List.of(
                중앙_역할(ChallengerRoleType.CENTRAL_PRESIDENT, GISU_ID),
                학교_역할(ChallengerRoleType.SCHOOL_PRESIDENT, SCHOOL_ID + 1, GISU_ID),
                학교_역할(ChallengerRoleType.SCHOOL_PART_LEADER, SCHOOL_ID, GISU_ID),
                학교_역할(ChallengerRoleType.SCHOOL_PART_LEADER, SCHOOL_ID, GISU_ID)
            ));

            List<ChallengerRoleType> result = sut().getAllRoleTypesByMemberIdAndSchoolId(MEMBER_ID, SCHOOL_ID);

            assertThat(result).containsExactly(ChallengerRoleType.SCHOOL_PART_LEADER);
        }

        @Test
        @DisplayName("담당 파트는 null을 제거하고 중복 없이 반환한다")
        void 담당_파트는_null과_중복을_제거한다() {
            given(loadChallengerRolePort.findRolesByMemberIdAndGisuId(MEMBER_ID, GISU_ID)).willReturn(List.of(
                중앙_역할(ChallengerRoleType.CENTRAL_PRESIDENT, GISU_ID),
                학교_파트장(SCHOOL_ID, GISU_ID, ChallengerPart.SPRINGBOOT),
                학교_파트장(SCHOOL_ID, GISU_ID, ChallengerPart.SPRINGBOOT),
                학교_파트장(SCHOOL_ID, GISU_ID, ChallengerPart.WEB)
            ));

            Set<ChallengerPart> result = sut().listResponsiblePartsByMemberIdAndGisuId(MEMBER_ID, GISU_ID);

            assertThat(result).containsExactlyInAnyOrder(ChallengerPart.SPRINGBOOT, ChallengerPart.WEB);
        }
    }

    @Nested
    @DisplayName("챌린저별 역할 묶음")
    class ChallengerRoleMapQuery {

        @Test
        @DisplayName("null 또는 빈 챌린저 ID 집합은 저장소 조회 없이 빈 Map을 반환한다")
        void null과_빈_ID는_빈_Map이다() {
            ChallengerRoleQueryService sut = sut();

            assertThat(sut.mapRoleTypesByChallengerIds(null)).isEmpty();
            assertThat(sut.mapRoleTypesByChallengerIds(Set.of())).isEmpty();
            then(loadChallengerRolePort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("챌린저별 역할은 챌린저 ID로 그룹화하고 역할 순서를 유지한다")
        void 역할은_챌린저_ID로_그룹화한다() {
            ChallengerRole first = 중앙_역할(ChallengerRoleType.CENTRAL_PRESIDENT, GISU_ID);
            ChallengerRole second = 학교_역할(ChallengerRoleType.SCHOOL_PART_LEADER, SCHOOL_ID, GISU_ID);
            ChallengerRole third = 학교_역할(ChallengerRoleType.SCHOOL_PRESIDENT, SCHOOL_ID, GISU_ID);
            ReflectionTestUtils.setField(first, "challengerId", 10L);
            ReflectionTestUtils.setField(second, "challengerId", 10L);
            ReflectionTestUtils.setField(third, "challengerId", 11L);
            given(loadChallengerRolePort.findByChallengerIdIn(Set.of(10L, 11L)))
                .willReturn(List.of(first, second, third));

            Map<Long, List<ChallengerRoleType>> result = sut()
                .mapRoleTypesByChallengerIds(Set.of(10L, 11L));

            assertThat(result).containsEntry(10L, List.of(
                ChallengerRoleType.CENTRAL_PRESIDENT,
                ChallengerRoleType.SCHOOL_PART_LEADER
            ));
            assertThat(result).containsEntry(11L, List.of(ChallengerRoleType.SCHOOL_PRESIDENT));
        }
    }

    private ChallengerRoleQueryService sut() {
        return new ChallengerRoleQueryService(
            loadChallengerRolePort,
            getGisuUseCase,
            listMemberSystemRoleUseCase,
            memberId -> true
        );
    }

    private GisuInfo gisu() {
        return new GisuInfo(GISU_ID, 10L, null, null, true);
    }
}
