package com.umc.product.challenger.adapter.in.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.umc.product.challenger.adapter.in.web.dto.request.DeactivateChallengerRequest;
import com.umc.product.challenger.adapter.in.web.dto.request.EditChallengerPartRequest;
import com.umc.product.challenger.adapter.in.web.dto.request.GlobalSearchChallengerRequest;
import com.umc.product.challenger.adapter.in.web.dto.request.SearchChallengerCursorRequest;
import com.umc.product.challenger.adapter.in.web.dto.response.GlobalSearchChallengerResponse;
import com.umc.product.challenger.application.port.in.command.dto.ChallengerDeactivationType;
import com.umc.product.challenger.application.port.in.command.dto.CreateChallengerRecordCommand;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerWorkbookSummary;
import com.umc.product.challenger.application.port.in.query.dto.GlobalSearchChallengerCursorResult;
import com.umc.product.challenger.application.port.in.query.dto.GlobalSearchChallengerItemInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.curriculum.domain.enums.WorkbookStatus;

@DisplayName("Challenger Web DTO 잔여 계약")
class ChallengerWebDtoResidualTest {

    @Nested
    @DisplayName("검색 요청")
    class SearchRequest {

        @Test
        @DisplayName("커서 검색은 유효하지 않은 크기를 기본값으로 보정하고 ACTIVE 상태만 조회한다")
        void 커서_검색은_유효하지_않은_크기를_기본값으로_보정한다() {
            SearchChallengerCursorRequest request = new SearchChallengerCursorRequest(
                10L, 0, 20L, "이름", "닉네임", "통합", 30L, 40L,
                ChallengerPart.SPRINGBOOT, 50L
            );

            var query = request.toQuery();

            assertThat(request.getSize()).isEqualTo(20);
            assertThat(query.challengerId()).isEqualTo(20L);
            assertThat(query.name()).isEqualTo("이름");
            assertThat(query.nickname()).isEqualTo("닉네임");
            assertThat(query.keyword()).isEqualTo("통합");
            assertThat(query.schoolId()).isEqualTo(30L);
            assertThat(query.chapterId()).isEqualTo(40L);
            assertThat(query.part()).isEqualTo(ChallengerPart.SPRINGBOOT);
            assertThat(query.gisuId()).isEqualTo(50L);
            assertThat(query.statuses()).containsExactly(ChallengerStatus.ACTIVE);
        }

        @Test
        @DisplayName("커서 검색은 null 크기를 기본값으로, 큰 크기를 최대값으로 보정한다")
        void 커서_검색의_크기_경계값을_보정한다() {
            assertThat(cursorRequest(null).getSize()).isEqualTo(20);
            assertThat(cursorRequest(51).getSize()).isEqualTo(50);
            assertThat(cursorRequest(1).getSize()).isEqualTo(1);
        }

        @Test
        @DisplayName("전역 검색은 ACTIVE와 GRADUATED를 포함하고 크기 경계값을 보정한다")
        void 전역_검색은_상태와_크기_경계값을_보정한다() {
            GlobalSearchChallengerRequest request = new GlobalSearchChallengerRequest(10L, -1, "홍", "길동");

            var query = request.toQuery();

            assertThat(request.getSize()).isEqualTo(20);
            assertThat(query.name()).isEqualTo("홍");
            assertThat(query.nickname()).isEqualTo("길동");
            assertThat(query.statuses()).containsExactly(ChallengerStatus.ACTIVE, ChallengerStatus.GRADUATED);
            assertThat(new GlobalSearchChallengerRequest(null, null, null, null).getSize()).isEqualTo(20);
            assertThat(new GlobalSearchChallengerRequest(null, 51, null, null).getSize()).isEqualTo(50);
            assertThat(new GlobalSearchChallengerRequest(null, 1, null, null).getSize()).isEqualTo(1);
        }

        private SearchChallengerCursorRequest cursorRequest(Integer size) {
            return new SearchChallengerCursorRequest(null, size, null, null, null, null,
                null, null, null, null);
        }
    }

    @Test
    @DisplayName("수정 요청은 모든 식별자와 변경 사유를 명령으로 전달한다")
    void 수정_요청은_명령으로_변환된다() {
        var partCommand = new EditChallengerPartRequest(ChallengerPart.WEB).toCommand(1L, 2L);
        var deactivateCommand = new DeactivateChallengerRequest(
            ChallengerDeactivationType.EXPEL, 3L, "운영 정책 위반"
        ).toCommand(1L);

        assertThat(partCommand.challengerId()).isEqualTo(1L);
        assertThat(partCommand.newPart()).isEqualTo(ChallengerPart.WEB);
        assertThat(partCommand.modifiedBy()).isEqualTo(2L);
        assertThat(deactivateCommand.challengerId()).isEqualTo(1L);
        assertThat(deactivateCommand.deactivationType()).isEqualTo(ChallengerDeactivationType.EXPEL);
        assertThat(deactivateCommand.modifiedBy()).isEqualTo(3L);
        assertThat(deactivateCommand.reason()).isEqualTo("운영 정책 위반");
    }

    @Test
    @DisplayName("전역 검색 결과는 항목과 커서 메타데이터를 손실 없이 변환한다")
    void 전역_검색_결과를_응답으로_변환한다() {
        var item = new GlobalSearchChallengerItemInfo(
            1L, "닉네임", "이름", "학교", 9L, ChallengerPart.SPRINGBOOT, "profile"
        );

        var response = GlobalSearchChallengerResponse.from(
            new GlobalSearchChallengerCursorResult(List.of(item), 99L, true)
        );

        assertThat(response.cursor().nextCursor()).isEqualTo(99L);
        assertThat(response.cursor().hasNext()).isTrue();
        assertThat(response.cursor().content()).singleElement().satisfies(converted -> {
            assertThat(converted.memberId()).isEqualTo(1L);
            assertThat(converted.nickname()).isEqualTo("닉네임");
            assertThat(converted.name()).isEqualTo("이름");
            assertThat(converted.schoolName()).isEqualTo("학교");
            assertThat(converted.gisu()).isEqualTo(9L);
            assertThat(converted.part()).isEqualTo(ChallengerPart.SPRINGBOOT);
            assertThat(converted.profileImageLink()).isEqualTo("profile");
        });
    }

    @Test
    @DisplayName("기록 생성 명령은 일반·중앙·지부·학교 조직 규칙으로 엔티티를 만든다")
    void 기록_생성_명령은_조직_단계별_엔티티를_만든다() {
        var normal = recordCommand(null).toEntity();
        var central = recordCommand(ChallengerRoleType.CENTRAL_PRESIDENT).toEntity();
        var chapter = recordCommand(ChallengerRoleType.CHAPTER_PRESIDENT).toEntity();
        var school = recordCommand(ChallengerRoleType.SCHOOL_PRESIDENT).toEntity();

        assertThat(normal.getChallengerRoleType()).isNull();
        assertThat(central.getOrganizationId()).isNull();
        assertThat(chapter.getOrganizationId()).isEqualTo(3L);
        assertThat(school.getOrganizationId()).isEqualTo(4L);
        assertThat(recordCommand(null).toString())
            .contains("creatorMemberId=1", "gisuId=2", "chapterId=3", "schoolId=4")
            .doesNotContain("민감한 이름");
    }

    @Test
    @DisplayName("워크북 요약 레코드는 모든 조회 값을 보존한다")
    void 워크북_요약_레코드는_조회_값을_보존한다() {
        var summary = new ChallengerWorkbookSummary(
            1L, 2L, "챌린저", "학교", "SPRINGBOOT", "워크북", "제출", WorkbookStatus.SUBMITTED, true
        );

        assertThat(summary.challengerWorkbookId()).isEqualTo(1L);
        assertThat(summary.challengerId()).isEqualTo(2L);
        assertThat(summary.isBest()).isTrue();
    }

    private CreateChallengerRecordCommand recordCommand(ChallengerRoleType roleType) {
        return CreateChallengerRecordCommand.builder()
            .creatorMemberId(1L)
            .gisuId(2L)
            .chapterId(3L)
            .schoolId(4L)
            .part(ChallengerPart.SPRINGBOOT)
            .memberName("민감한 이름")
            .challengerRoleType(roleType)
            .build();
    }
}
