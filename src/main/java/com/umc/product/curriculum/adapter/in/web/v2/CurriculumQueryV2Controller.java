package com.umc.product.curriculum.adapter.in.web.v2;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.curriculum.adapter.in.web.v2.dto.response.CurriculumOverviewResponse;
import com.umc.product.curriculum.adapter.in.web.v2.dto.response.MyCurriculumResponse;
import com.umc.product.curriculum.application.port.in.query.GetCurriculumUseCase;
import com.umc.product.curriculum.application.port.in.query.dto.CurriculumOverviewInfo;
import com.umc.product.curriculum.application.port.in.query.dto.MyCurriculumInfo;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.global.security.annotation.Public;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/curriculums")
@RequiredArgsConstructor
@Tag(name = "Curriculum V2 | 커리큘럼 Query", description = "커리큘럼과 내 진행 상황을 조회합니다.")
public class CurriculumQueryV2Controller {

    private final GetCurriculumUseCase getCurriculumUseCase;

    @Operation(
        operationId = "CURRICULUM-101",
        summary = "특정 기수의 파트 또는 트랙 커리큘럼 조회",
        description = """
            요청을 보낸 사람과 관계없이, 해당 기수의 커리큘럼 목록을 조회하기 위해서 사용합니다.

            예상되는 사용처는 아래와 같습니다.
            - UMC WEB Landing Page
            - UMC APP (10th) 커리큘럼 목록 조회

            PART 기수는 part, TRACK 기수는 track 하나를 지정합니다. 기본 트랙만 지원합니다.
            선택한 커리큘럼에 대한 정보를 반환하며, 세부 내용은 아래와 같습니다.
            - 상위 단위, Curriculum의 제목
            - 주차별 커리큘럼, WeeklyCurriculum의 제목, N주차, 부록 여부, 시작/종료일

            `weekNo` 파라미터를 지정하면 해당 주차의 워크북만 반환합니다.
            """
    )
    @Public
    @GetMapping("/overview")
    public CurriculumOverviewResponse getCurriculum(
        @RequestParam Long gisuId,
        @RequestParam(required = false) ChallengerPart part,
        @RequestParam(required = false) ChallengerTrack track,
        @RequestParam(required = false) Long weekNo
    ) {
        CurriculumOverviewInfo info = getCurriculumUseCase.getCurriculumOverview(gisuId, part, track, weekNo);
        return CurriculumOverviewResponse.from(info);
    }

    @Operation(
        operationId = "CURRICULUM-102",
        summary = "내 커리큘럼 진행 상황 조회",
        description = """
            PART 기수는 본인의 파트에 해당하는 커리큘럼을 반환합니다.
            TRACK 기수는 수강 중인 기본 track을 지정합니다. 기본 트랙이 정확히 하나면 생략할 수 있습니다.
            여러 기본 트랙을 수강하면 track을 반드시 지정해야 하며, 응답은 선택한 커리큘럼 한 개입니다.
            각 주차별 워크북의 상태(기본/진행중/제출완료/통과/실패)를 반환합니다.
            """
    )
    @GetMapping("/progress/me")
    public MyCurriculumResponse getMyProgress(
        @RequestParam Long gisuId,
        @RequestParam(required = false) ChallengerTrack track,
        @CurrentMember MemberPrincipal memberPrincipal
    ) {
        MyCurriculumInfo info = getCurriculumUseCase.getMyProgress(memberPrincipal.getMemberId(), gisuId, track);
        return MyCurriculumResponse.from(info);
    }
}
