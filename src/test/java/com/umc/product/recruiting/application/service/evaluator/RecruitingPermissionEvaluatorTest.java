package com.umc.product.recruiting.application.service.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SystemRoleType;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonPort;
import com.umc.product.recruiting.domain.RecruitingSeason;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecruitingPermissionEvaluator")
class RecruitingPermissionEvaluatorTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long GISU_ID = 11L;
    private static final Long SCHOOL_ID = 22L;
    private static final Long OTHER_SCHOOL_ID = 33L;
    private static final Long SEASON_ID = 44L;

    @Mock
    LoadRecruitingSeasonPort loadRecruitingSeasonPort;

    @Mock
    GetSchoolUseCase getSchoolUseCase;

    RecruitingPermissionEvaluator sut;

    @BeforeEach
    void setUp() {
        sut = new RecruitingPermissionEvaluator(loadRecruitingSeasonPort, getSchoolUseCase);
    }

    @Test
    @DisplayName("supportedResourceType은 RECRUITMENT를 반환한다")
    void supportedResourceType은_RECRUITMENT를_반환한다() {
        assertThat(sut.supportedResourceType()).isEqualTo(ResourceType.RECRUITMENT);
    }

    @Test
    @DisplayName("학교 회장단은 자기 학교 모집 WRITE 권한을 통과한다")
    void 학교_회장단은_자기_학교_모집_WRITE_권한을_통과한다() {
        givenSeason();
        givenSchool(SCHOOL_ID, 100L);
        SubjectAttributes subject = subjectWithRoles(schoolPresidentRole(SCHOOL_ID));

        assertThat(sut.evaluate(subject, seasonPermission(PermissionType.WRITE))).isTrue();
    }

    @Test
    @DisplayName("학교 회장단은 다른 학교 모집 WRITE 권한을 거부한다")
    void 학교_회장단은_다른_학교_모집_WRITE_권한을_거부한다() {
        givenSeason();
        givenSchool(SCHOOL_ID, 100L);
        SubjectAttributes subject = subjectWithRoles(schoolPresidentRole(OTHER_SCHOOL_ID));

        assertThat(sut.evaluate(subject, seasonPermission(PermissionType.WRITE))).isFalse();
    }

    @Test
    @DisplayName("학교 회장단은 다른 학교 모집 READ 권한을 통과한다")
    void 학교_회장단은_다른_학교_모집_READ_권한을_통과한다() {
        givenSeason();
        givenSchool(SCHOOL_ID, 100L);
        SubjectAttributes subject = subjectWithRoles(schoolPresidentRole(OTHER_SCHOOL_ID));

        assertThat(sut.evaluate(subject, seasonPermission(PermissionType.READ))).isTrue();
    }

    @Test
    @DisplayName("지부장은 리소스 ID가 없는 경우 모집 READ 권한을 통과한다")
    void 지부장은_리소스_ID가_없는_경우_모집_READ_권한을_통과한다() {
        SubjectAttributes subject = subjectWithRoles(chapterPresidentRole(100L));

        assertThat(sut.evaluate(
            subject,
            ResourcePermission.ofType(ResourceType.RECRUITMENT, PermissionType.READ)
        )).isTrue();
    }

    @Test
    @DisplayName("지부장은 자기 지부 소속 학교 모집 WRITE 권한을 통과한다")
    void 지부장은_자기_지부_소속_학교_모집_WRITE_권한을_통과한다() {
        givenSeason();
        givenSchool(SCHOOL_ID, 100L);
        SubjectAttributes subject = subjectWithRoles(chapterPresidentRole(100L));

        assertThat(sut.evaluate(subject, seasonPermission(PermissionType.WRITE))).isTrue();
    }

    @Test
    @DisplayName("지부장은 다른 지부 소속 학교 모집 WRITE 권한을 거부한다")
    void 지부장은_다른_지부_소속_학교_모집_WRITE_권한을_거부한다() {
        givenSeason();
        givenSchool(SCHOOL_ID, 200L);
        SubjectAttributes subject = subjectWithRoles(chapterPresidentRole(100L));

        assertThat(sut.evaluate(subject, seasonPermission(PermissionType.WRITE))).isFalse();
    }

    @Test
    @DisplayName("지부장은 다른 지부 소속 학교 모집 READ 권한을 통과한다")
    void 지부장은_다른_지부_소속_학교_모집_READ_권한을_통과한다() {
        givenSeason();
        givenSchool(SCHOOL_ID, 200L);
        SubjectAttributes subject = subjectWithRoles(chapterPresidentRole(100L));

        assertThat(sut.evaluate(subject, seasonPermission(PermissionType.READ))).isTrue();
    }

    @Test
    @DisplayName("중앙운영사무국 총괄단은 모든 학교 모집 APPROVE 권한을 통과한다")
    void 중앙운영사무국_총괄단은_모든_학교_모집_APPROVE_권한을_통과한다() {
        givenSeason();
        SubjectAttributes subject = subjectWithRoles(centralPresidentRole());

        assertThat(sut.evaluate(subject, seasonPermission(PermissionType.APPROVE))).isTrue();
    }

    @Test
    @DisplayName("중앙운영사무국 총괄단은 모든 학교 모집 MANAGE 권한을 통과한다")
    void 중앙운영사무국_총괄단은_모든_학교_모집_MANAGE_권한을_통과한다() {
        givenSeason();
        SubjectAttributes subject = subjectWithRoles(centralPresidentRole());

        assertThat(sut.evaluate(subject, seasonPermission(PermissionType.MANAGE))).isTrue();
    }

    @Test
    @DisplayName("SUPER_ADMIN은 기수와 무관하게 특정 모집 MANAGE 권한을 통과한다")
    void SUPER_ADMIN은_기수와_무관하게_특정_모집_MANAGE_권한을_통과한다() {
        givenSeason();
        SubjectAttributes subject = superAdminSubject();

        assertThat(sut.evaluate(subject, seasonPermission(PermissionType.MANAGE))).isTrue();
    }

    @Test
    @DisplayName("SUPER_ADMIN은 모집 전체 MANAGE 권한을 통과한다")
    void SUPER_ADMIN은_모집_전체_MANAGE_권한을_통과한다() {
        SubjectAttributes subject = superAdminSubject();

        assertThat(sut.evaluate(
            subject,
            ResourcePermission.ofType(ResourceType.RECRUITMENT, PermissionType.MANAGE)
        )).isTrue();
    }

    @Test
    @DisplayName("학교 회장단은 자기 학교라도 MANAGE 권한을 거부한다")
    void 학교_회장단은_자기_학교라도_MANAGE_권한을_거부한다() {
        givenSeason();
        SubjectAttributes subject = subjectWithRoles(schoolPresidentRole(SCHOOL_ID));

        assertThat(sut.evaluate(subject, seasonPermission(PermissionType.MANAGE))).isFalse();
    }

    @Test
    @DisplayName("교내 파트장은 모집 WRITE 권한을 거부한다")
    void 교내_파트장은_모집_WRITE_권한을_거부한다() {
        givenSeason();
        givenSchool(SCHOOL_ID, 100L);
        SubjectAttributes subject = subjectWithRoles(schoolPartLeaderRole(SCHOOL_ID));

        assertThat(sut.evaluate(subject, seasonPermission(PermissionType.WRITE))).isFalse();
    }

    @Test
    @DisplayName("DELETE 권한은 evaluator에서 구현하지 않아 예외가 발생한다")
    void DELETE_권한은_evaluator에서_구현하지_않아_예외가_발생한다() {
        SubjectAttributes subject = subjectWithRoles(centralPresidentRole());

        assertThatThrownBy(() -> sut.evaluate(subject, seasonPermission(PermissionType.DELETE)))
            .isInstanceOf(AuthorizationDomainException.class);
    }

    private void givenSeason() {
        given(loadRecruitingSeasonPort.getById(SEASON_ID))
            .willReturn(season());
    }

    private void givenSchool(Long schoolId, Long chapterId) {
        given(getSchoolUseCase.getSchoolDetail(schoolId))
            .willReturn(schoolDetail(schoolId, chapterId));
    }

    private SchoolDetailInfo schoolDetail(Long schoolId, Long chapterId) {
        return new SchoolDetailInfo(
            chapterId,
            "테스트지부",
            "테스트학교",
            "테스트대",
            schoolId,
            null,
            null,
            List.of(),
            true,
            null,
            null
        );
    }

    private RecruitingSeason season() {
        RecruitingSeason season = RecruitingSeason.create(GISU_ID, SCHOOL_ID);
        ReflectionTestUtils.setField(season, "id", SEASON_ID);
        return season;
    }

    private ResourcePermission seasonPermission(PermissionType permissionType) {
        return ResourcePermission.of(ResourceType.RECRUITMENT, SEASON_ID, permissionType);
    }

    private SubjectAttributes subjectWithRoles(RoleAttribute... roles) {
        return SubjectAttributes.builder()
            .memberId(MEMBER_ID)
            .schoolId(SCHOOL_ID)
            .gisuChallengerInfos(List.of())
            .roleAttributes(List.of(roles))
            .build();
    }

    private SubjectAttributes superAdminSubject() {
        return SubjectAttributes.builder()
            .memberId(MEMBER_ID)
            .schoolId(SCHOOL_ID)
            .systemRoles(Set.of(SystemRoleType.SUPER_ADMIN))
            .build();
    }

    private RoleAttribute centralPresidentRole() {
        return new RoleAttribute(
            ChallengerRoleType.CENTRAL_PRESIDENT,
            OrganizationType.CENTRAL,
            null,
            null,
            GISU_ID
        );
    }

    private RoleAttribute schoolPresidentRole(Long schoolId) {
        return new RoleAttribute(
            ChallengerRoleType.SCHOOL_PRESIDENT,
            OrganizationType.SCHOOL,
            schoolId,
            null,
            GISU_ID
        );
    }

    private RoleAttribute schoolPartLeaderRole(Long schoolId) {
        return new RoleAttribute(
            ChallengerRoleType.SCHOOL_PART_LEADER,
            OrganizationType.SCHOOL,
            schoolId,
            null,
            GISU_ID
        );
    }

    private RoleAttribute chapterPresidentRole(Long chapterId) {
        return new RoleAttribute(
            ChallengerRoleType.CHAPTER_PRESIDENT,
            OrganizationType.CHAPTER,
            chapterId,
            null,
            GISU_ID
        );
    }
}
