package com.umc.product.challenger.adapter.in.web.assembler;

import org.springframework.stereotype.Component;

import com.umc.product.challenger.adapter.in.web.dto.response.ChallengerRecordResponse;
import com.umc.product.challenger.application.port.in.query.GetChallengerRecordUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerRecordInfo;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class ChallengerRecordResponseAssembler {

    private final GetGisuUseCase getGisuUseCase;
    private final GetSchoolUseCase getSchoolUseCase;
    private final GetChapterUseCase getChapterUseCase;
    private final GetChallengerRecordUseCase getChallengerRecordUseCase;

    public ChallengerRecordResponse from(String code) {

        ChallengerRecordInfo recordInfo = getChallengerRecordUseCase.getByCode(code);

        return infoToResponse(recordInfo);
    }

    public ChallengerRecordResponse from(Long id) {

        ChallengerRecordInfo recordInfo = getChallengerRecordUseCase.getById(id);

        return infoToResponse(recordInfo);
    }

    private ChallengerRecordResponse infoToResponse(ChallengerRecordInfo recordInfo) {
        GisuInfo gisuInfo = getGisuUseCase.getById(recordInfo.gisuId());
        SchoolDetailInfo schoolInfo = getSchoolUseCase.getSchoolDetail(recordInfo.schoolId());
        ChapterInfo chapterInfo = getChapterUseCase.getChapterById(recordInfo.chapterId());

        return ChallengerRecordResponse.builder()
            .code(recordInfo.code())
            .part(recordInfo.part())
            .track(recordInfo.track())
            .gisuId(gisuInfo.gisuId())
            .gisu(gisuInfo.generation())
            .schoolId(schoolInfo.schoolId())
            .schoolName(schoolInfo.schoolName())
            .chapterId(chapterInfo.id())
            .chapterName(chapterInfo.name())
            .memberName(recordInfo.memberName())
            .challengerRoleType(recordInfo.challengerRoleType())
            .organizationId(recordInfo.organizationId())
            .build();
    }
}
