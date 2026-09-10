package com.umc.product.curriculum.adapter.in.web.v2.dto.response;

import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.curriculum.application.port.in.query.dto.MyCurriculumInfo;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;

import lombok.Builder;

/**
 * 커리큘럼, 주차별 커리큘럼, 원본 워크북, 챌린저 워크북, 미션 제출물, 피드백 정보, 베스트 워크북 정보를 나타내는 DTO
 */
@Builder
public record MyCurriculumResponse(
    Long curriculumId,
    String title,
    List<MyWeeklyCurriculumResponse> weeks,
    ChallengerPart part,
    ChallengerTrack track
) {

    public MyCurriculumResponse(Long curriculumId, String title, List<MyWeeklyCurriculumResponse> weeks) {
        this(curriculumId, title, weeks, null, null);
    }

    public static MyCurriculumResponse from(MyCurriculumInfo info) {
        return MyCurriculumResponse.builder()
            .curriculumId(info.curriculumId())
            .title(info.title())
            .part(info.part())
            .track(info.track())
            .weeks(info.weeks().stream()
                .map(MyWeeklyCurriculumResponse::from)
                .toList())
            .build();
    }

    /**
     * 사용자에 따른 각 주차별 커리큘럼과 관련된 내용
     * <p>
     * 각 주차별 기간은 미션 제출이 가능한 기긴과는 무관합니다.
     *
     * @param weeklyCurriculumId      PK
     * @param weekNo                  몇 주차인지
     * @param title                   주차별 커리큘럼 대제목
     * @param isExtra                 부록 여부
     * @param startsAt                주차 시작 기간
     * @param endsAt                  주차 종료 기간
     * @param isRequesterBestWorkbook 주차별 워크북이 베스트 워크북으로 선정되었는지 여부
     * @param originalWorkbooks       해당 주차에 대해서 배포 완료된 원본 워크북 목록
     */
    @Builder
    public record MyWeeklyCurriculumResponse(
        Long weeklyCurriculumId,
        Long weekNo,
        String title,
        boolean isExtra,
        Instant startsAt,
        Instant endsAt,
        WeeklyCurriculumStatus status,
        List<MyOriginalWorkbookResponse> originalWorkbooks
    ) {

        static MyWeeklyCurriculumResponse from(MyCurriculumInfo.MyWeeklyCurriculumInfo w) {
            return MyWeeklyCurriculumResponse.builder()
                .weeklyCurriculumId(w.weeklyCurriculumId())
                .weekNo(w.weekNo())
                .title(w.title())
                .isExtra(w.isExtra())
                .startsAt(w.startsAt())
                .endsAt(w.endsAt())
                .status(WeeklyCurriculumStatus.from(w))
                .originalWorkbooks(w.releasedOriginalWorkbooks().stream()
                    .map(MyOriginalWorkbookResponse::from)
                    .toList())
                .build();
        }
    }

    /**
     * 요청자에 따라서 OriginalWorkbook의 정보와 미션, 해당 원본 워크북을 배포 받은 ChallengerWorkbook에 대한 정보
     * <p>
     * OriginalWorkbook 자체에 대한 정보 조회는 별도의 DTO 사용 예정
     * <p>
     * OriginalWorkbook 및 ChallengerWorkbook의 content는 제외하고 제공됩니다.
     *
     * @param originalWorkbookId PK
     * @param title              원본 워크북 제목
     * @param description        (nullable) 원본 워크북 설명
     * @param url                (nullable) 원본 워크북 URL
     * @param type               워크북 유형 (메인/부록 여부)
     * @param missions           워크북 미션과 관련된 정보
     * @param isDeployedToMember 원본 워크북이 요청자에게 배포된 상태인지 여부 (ChallengerWorkbook이 존재하는지)
     * @param challengerWorkbook 원본 워크북을 배포받은 경우 그 챌린저 워크북과 관련된 정보.
     */
    @Builder
    public record MyOriginalWorkbookResponse(
        Long originalWorkbookId,
        String title,
        String description,
        String url,
        OriginalWorkbookType type,
        List<MyOriginalWorkbookMissionResponse> missions,
        boolean isDeployedToMember,
        Long challengerWorkbookId
    ) {

        static MyOriginalWorkbookResponse from(MyCurriculumInfo.MyOriginalWorkbookInfo wb) {
            return MyOriginalWorkbookResponse.builder()
                .originalWorkbookId(wb.originalWorkbookId())
                .title(wb.title())
                .description(wb.description())
                .url(wb.url())
                .type(wb.type())
                .missions(wb.missions().stream()
                    .map(MyOriginalWorkbookMissionResponse::from)
                    .toList())
                .isDeployedToMember(wb.isDeployedToMember())
                .challengerWorkbookId(wb.challengerWorkbookId().orElse(null))
                .build();
        }
    }

    /**
     * 요청자에 따른 원본 워크북 미션과 관련된 정보를 제공하는 DTO
     *
     * @param originalWorkbookMissionId PK
     * @param title                     미션 제목
     * @param description               미션 설명 (nullable)
     * @param missionType               미션 유형
     * @param isNecessary               미션 필수 수행 여부
     * @param hasSubmission             미션 제출물이 존재하는지 여부
     * @param submission                요청자가 제출한 미션에 대한 정보, 미션 당 제출물은 한 개로 제한됨
     */
    @Builder
    public record MyOriginalWorkbookMissionResponse(
        Long originalWorkbookMissionId,
        String title,
        String description,
        MissionType missionType,
        boolean isNecessary,
        boolean hasSubmission,
        MissionSubmissionResponse submission
    ) {

        static MyOriginalWorkbookMissionResponse from(MyCurriculumInfo.MyOriginalWorkbookMissionInfo m) {
            return MyOriginalWorkbookMissionResponse.builder()
                .originalWorkbookMissionId(m.originalWorkbookMissionId())
                .title(m.title())
                .description(m.description())
                .missionType(m.missionType())
                .isNecessary(m.isNecessary())
                .hasSubmission(m.hasSubmission())
                .submission(m.submission()
                    .map(s -> MissionSubmissionResponse.from(m.originalWorkbookMissionId(), s))
                    .orElse(null))
                .build();
        }
    }
}
