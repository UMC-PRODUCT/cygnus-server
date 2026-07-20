package com.umc.product.challenger.adapter.in.web.assembler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.challenger.application.port.in.query.GetChallengerRecordUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerRecordInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengerRecordResponseAssembler")
class ChallengerRecordResponseAssemblerTest {

    @Mock
    GetGisuUseCase getGisuUseCase;

    @Mock
    GetSchoolUseCase getSchoolUseCase;

    @Mock
    GetChallengerRecordUseCase getChallengerRecordUseCase;

    ChallengerRecordResponseAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new ChallengerRecordResponseAssembler(
            getGisuUseCase, getSchoolUseCase, getChallengerRecordUseCase
        );
    }

    @Test
    @DisplayName("코드 조회 결과에 기수와 학교·지부 정보를 조립한다")
    void 코드_조회_결과를_조립한다() {
        given(getChallengerRecordUseCase.getByCode("ABC123")).willReturn(recordInfo());
        givenOrganizationInfo();

        var response = assembler.from("ABC123");

        assertResponse(response);
    }

    @Test
    @DisplayName("ID 조회 결과에 기수와 학교·지부 정보를 조립한다")
    void 아이디_조회_결과를_조립한다() {
        given(getChallengerRecordUseCase.getById(1L)).willReturn(recordInfo());
        givenOrganizationInfo();

        var response = assembler.from(1L);

        assertResponse(response);
    }

    private void givenOrganizationInfo() {
        given(getGisuUseCase.getById(2L)).willReturn(new GisuInfo(2L, 9L, null, null, true));
        given(getSchoolUseCase.getSchoolDetail(4L)).willReturn(new SchoolDetailInfo(
            3L, "지부", "학교", 4L, null, null, List.of(), true, null, null
        ));
    }

    private ChallengerRecordInfo recordInfo() {
        return ChallengerRecordInfo.builder()
            .id(1L)
            .code("ABC123")
            .memberName("홍길동")
            .challengerRoleType(ChallengerRoleType.SCHOOL_PRESIDENT)
            .organizationId(4L)
            .createdMemberId(10L)
            .gisuId(2L)
            .chapterId(3L)
            .schoolId(4L)
            .part(ChallengerPart.SPRINGBOOT)
            .build();
    }

    private void assertResponse(com.umc.product.challenger.adapter.in.web.dto.response.ChallengerRecordResponse response) {
        assertThat(response.code()).isEqualTo("ABC123");
        assertThat(response.part()).isEqualTo(ChallengerPart.SPRINGBOOT);
        assertThat(response.gisuId()).isEqualTo(2L);
        assertThat(response.gisu()).isEqualTo(9L);
        assertThat(response.schoolId()).isEqualTo(4L);
        assertThat(response.schoolName()).isEqualTo("학교");
        assertThat(response.chapterId()).isEqualTo(3L);
        assertThat(response.chapterName()).isEqualTo("지부");
        assertThat(response.memberName()).isEqualTo("홍길동");
        assertThat(response.challengerRoleType()).isEqualTo(ChallengerRoleType.SCHOOL_PRESIDENT);
        assertThat(response.organizationId()).isEqualTo(4L);
    }
}
