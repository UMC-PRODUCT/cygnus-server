package com.umc.product.challenger.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.challenger.adapter.in.web.assembler.ChallengerRecordResponseAssembler;
import com.umc.product.challenger.adapter.in.web.assembler.ChallengerResponseAssembler;
import com.umc.product.challenger.adapter.in.web.dto.request.AddChallengerRecordToMemberRequest;
import com.umc.product.challenger.adapter.in.web.dto.request.CreateChallengerInfoRequest;
import com.umc.product.challenger.adapter.in.web.dto.request.CreateChallengerRecordRequest;
import com.umc.product.challenger.adapter.in.web.dto.request.DeactivateChallengerRequest;
import com.umc.product.challenger.adapter.in.web.dto.request.EditChallengerPartRequest;
import com.umc.product.challenger.adapter.in.web.dto.request.GlobalSearchChallengerRequest;
import com.umc.product.challenger.adapter.in.web.dto.response.ChallengerInfoResponse;
import com.umc.product.challenger.adapter.in.web.dto.response.ChallengerRecordResponse;
import com.umc.product.challenger.application.port.in.command.ManageChallengerRecordUseCase;
import com.umc.product.challenger.application.port.in.command.ManageChallengerUseCase;
import com.umc.product.challenger.application.port.in.command.dto.ChallengerDeactivationType;
import com.umc.product.challenger.application.port.in.command.dto.ConsumeChallengerRecordCommand;
import com.umc.product.challenger.application.port.in.command.dto.CreateChallengerRecordCommand;
import com.umc.product.challenger.application.port.in.command.dto.DeleteChallengerCommand;
import com.umc.product.challenger.application.port.in.query.SearchChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.GlobalSearchChallengerCursorResult;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.security.MemberPrincipal;

@DisplayName("Challenger Controller 정상 흐름")
class ChallengerControllerResidualTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("챌린저 명령")
    class CommandController {

        @Mock
        ManageChallengerUseCase manageChallengerUseCase;

        @Mock
        ChallengerResponseAssembler assembler;

        @InjectMocks
        ChallengerCommandController controller;

        @Test
        @DisplayName("일괄 생성은 입력 순서대로 생성 결과를 조립한다")
        void 일괄_생성한다() {
            var firstResponse = ChallengerInfoResponse.builder().challengerId(10L).build();
            var secondResponse = ChallengerInfoResponse.builder().challengerId(20L).build();
            given(manageChallengerUseCase.createChallenger(any())).willReturn(10L, 20L);
            given(assembler.fromChallengerId(10L)).willReturn(firstResponse);
            given(assembler.fromChallengerId(20L)).willReturn(secondResponse);

            var result = controller.bulkCreateChallenger(List.of(
                new CreateChallengerInfoRequest(
                    1L,
                    ChallengerPart.SPRINGBOOT,
                    List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER),
                    9L
                ),
                new CreateChallengerInfoRequest(
                    2L,
                    ChallengerPart.WEB,
                    List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER),
                    9L
                )
            ));

            assertThat(result).containsExactly(firstResponse, secondResponse);
        }

        @Test
        @DisplayName("비활성화 요청을 대상 ID와 함께 전달한다")
        void 비활성화한다() {
            controller.deactivateChallenger(10L, new DeactivateChallengerRequest(
                ChallengerDeactivationType.WITHDRAW, 20L, "본인 요청"
            ));

            then(manageChallengerUseCase).should().deactivateChallenger(any());
        }

        @Test
        @DisplayName("파트를 변경한 뒤 최신 챌린저 응답을 반환한다")
        void 파트를_변경한다() {
            var expected = ChallengerInfoResponse.builder().challengerId(10L).build();
            given(assembler.fromChallengerId(10L)).willReturn(expected);
            var principal = MemberPrincipal.builder().memberId(20L).build();

            var result = controller.editChallengerInfo(
                principal, 10L, new EditChallengerPartRequest(ChallengerPart.WEB)
            );

            assertThat(result).isSameAs(expected);
            then(manageChallengerUseCase).should().updateChallenger(any());
        }

        @Test
        @DisplayName("물리 삭제 명령에는 관리자 삭제 사유를 고정한다")
        void 물리_삭제한다() {
            controller.deleteChallenger(10L);

            var captor = ArgumentCaptor.forClass(DeleteChallengerCommand.class);
            then(manageChallengerUseCase).should().deleteChallenger(captor.capture());
            assertThat(captor.getValue().challengerId()).isEqualTo(10L);
            assertThat(captor.getValue().description()).isEqualTo("관리자에 의한 삭제");
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("챌린저 기록")
    class RecordController {

        @Mock
        ChallengerRecordResponseAssembler assembler;

        @Mock
        ManageChallengerRecordUseCase manageChallengerRecordUseCase;

        @InjectMocks
        ChallengerRecordController controller;

        @Test
        @DisplayName("ID로 기록 응답을 조회한다")
        void 아이디로_조회한다() {
            var expected = ChallengerRecordResponse.builder().code("ABC123").build();
            given(assembler.from(1L)).willReturn(expected);

            assertThat(controller.getChallengerRecordById(1L)).isSameAs(expected);
        }

        @Test
        @DisplayName("코드를 현재 회원의 기록으로 소비한다")
        void 코드를_소비한다() {
            var principal = MemberPrincipal.builder().memberId(9L).build();

            controller.addChallengerRecordToMember(
                principal, new AddChallengerRecordToMemberRequest("ABC123")
            );

            var captor = ArgumentCaptor.forClass(ConsumeChallengerRecordCommand.class);
            then(manageChallengerRecordUseCase).should().consumeCode(captor.capture());
            assertThat(captor.getValue().targetMemberId()).isEqualTo(9L);
            assertThat(captor.getValue().code()).isEqualTo("ABC123");
        }

        @Test
        @DisplayName("기록을 생성하고 조립된 응답을 반환한다")
        void 기록을_생성한다() {
            var request = recordRequest();
            var expected = ChallengerRecordResponse.builder().code("ABC123").build();
            given(manageChallengerRecordUseCase.create(any())).willReturn(1L);
            given(assembler.from(1L)).willReturn(expected);

            var result = controller.createChallengerRecord(
                MemberPrincipal.builder().memberId(9L).build(), request
            );

            assertThat(result).isSameAs(expected);
            var captor = ArgumentCaptor.forClass(CreateChallengerRecordCommand.class);
            then(manageChallengerRecordUseCase).should().create(captor.capture());
            assertThat(captor.getValue().creatorMemberId()).isEqualTo(9L);
            assertThat(captor.getValue().challengerRoleType()).isEqualTo(ChallengerRoleType.SCHOOL_PRESIDENT);
        }

        @Test
        @DisplayName("기록 일괄 생성은 모든 요청을 명령으로 변환하고 ID 목록을 반환한다")
        void 기록을_일괄_생성한다() {
            given(manageChallengerRecordUseCase.createBulk(any())).willReturn(List.of(1L, 2L));

            var result = controller.createChallengerRecordBulk(
                MemberPrincipal.builder().memberId(9L).build(), List.of(recordRequest(), recordRequest())
            );

            assertThat(result).containsExactly(1L, 2L);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<CreateChallengerRecordCommand>> captor = ArgumentCaptor.forClass(List.class);
            then(manageChallengerRecordUseCase).should().createBulk(captor.capture());
            assertThat(captor.getValue()).hasSize(2).allSatisfy(command -> {
                assertThat(command.creatorMemberId()).isEqualTo(9L);
                assertThat(command.gisuId()).isEqualTo(2L);
                assertThat(command.chapterId()).isEqualTo(3L);
                assertThat(command.schoolId()).isEqualTo(4L);
                assertThat(command.part()).isEqualTo(ChallengerPart.SPRINGBOOT);
                assertThat(command.memberName()).isEqualTo("홍길동");
            });
        }

        private CreateChallengerRecordRequest recordRequest() {
            return new CreateChallengerRecordRequest(
                2L, 3L, 4L, ChallengerPart.SPRINGBOOT, "홍길동", ChallengerRoleType.SCHOOL_PRESIDENT
            );
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("챌린저 검색")
    class SearchController {

        @Mock
        SearchChallengerUseCase searchChallengerUseCase;

        @InjectMocks
        ChallengerSearchController controller;

        @Test
        @DisplayName("전역 검색 요청과 커서 정보를 전달하고 응답으로 변환한다")
        void 전역_검색한다() {
            var searchResult = new GlobalSearchChallengerCursorResult(List.of(), null, false);
            given(searchChallengerUseCase.globalCursorSearch(any(), any(), any(Integer.class)))
                .willReturn(searchResult);

            var response = controller.globalSearchChallenger(
                new GlobalSearchChallengerRequest(10L, 5, "이름", "닉네임")
            );

            assertThat(response.cursor().content()).isEmpty();
            assertThat(response.cursor().hasNext()).isFalse();
            then(searchChallengerUseCase).should().globalCursorSearch(any(), any(), any(Integer.class));
        }
    }
}
