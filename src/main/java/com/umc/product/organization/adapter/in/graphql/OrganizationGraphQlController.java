package com.umc.product.organization.adapter.in.graphql;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.umc.product.organization.adapter.in.graphql.dto.ChapterGraphQlResponse;
import com.umc.product.organization.adapter.in.graphql.dto.ChapterSchoolGraphQlResponse;
import com.umc.product.organization.adapter.in.graphql.dto.GisuChapterGraphQlResponse;
import com.umc.product.organization.adapter.in.graphql.dto.GisuGraphQlResponse;
import com.umc.product.organization.adapter.in.graphql.dto.GisuOrganizationGraphQlRequest;
import com.umc.product.organization.adapter.in.graphql.dto.GisuOrganizationPayloadGraphQlResponse;
import com.umc.product.organization.adapter.in.graphql.dto.SchoolDetailGraphQlResponse;
import com.umc.product.organization.adapter.in.graphql.dto.SchoolNameGraphQlResponse;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuOrganizationUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterWithSchoolsInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class OrganizationGraphQlController {

    private final GetGisuOrganizationUseCase getGisuOrganizationUseCase;
    private final GetGisuUseCase getGisuUseCase;
    private final GetChapterUseCase getChapterUseCase;
    private final GetSchoolUseCase getSchoolUseCase;

    @QueryMapping
    public GisuOrganizationPayloadGraphQlResponse gisuOrganizations(
        @Argument GisuOrganizationGraphQlRequest input
    ) {
        return GisuOrganizationPayloadGraphQlResponse.from(
            getGisuOrganizationUseCase.get(input.toQuery())
        );
    }

    @QueryMapping
    public GisuGraphQlResponse gisu(@Argument Long id) {
        return GisuGraphQlResponse.from(getGisuUseCase.getById(id));
    }

    @QueryMapping
    public GisuGraphQlResponse activeGisu() {
        return GisuGraphQlResponse.from(getGisuUseCase.getActiveGisu());
    }

    @QueryMapping
    public List<ChapterGraphQlResponse> chapters() {
        return getChapterUseCase.getAllChapters().stream()
            .map(ChapterGraphQlResponse::from)
            .toList();
    }

    @QueryMapping
    public ChapterGraphQlResponse chapter(@Argument Long id) {
        return ChapterGraphQlResponse.from(getChapterUseCase.getChapterById(id));
    }

    @QueryMapping
    public List<SchoolNameGraphQlResponse> schools() {
        return getSchoolUseCase.getAllSchoolNames().stream()
            .map(SchoolNameGraphQlResponse::from)
            .toList();
    }

    @QueryMapping
    public SchoolDetailGraphQlResponse school(@Argument Long id) {
        return SchoolDetailGraphQlResponse.from(getSchoolUseCase.getSchoolDetail(id));
    }

    @BatchMapping(typeName = "Gisu", field = "chapters")
    public Map<GisuGraphQlResponse, List<GisuChapterGraphQlResponse>> chaptersByGisu(
        List<GisuGraphQlResponse> gisus
    ) {
        Set<Long> gisuIds = gisuIds(gisus);
        Map<Long, List<ChapterInfo>> chaptersByGisuId = getChapterUseCase.listByGisuIds(gisuIds);

        return gisus.stream()
            .collect(Collectors.toMap(
                Function.identity(),
                gisu -> chaptersByGisuId.getOrDefault(gisu.gisuId(), List.of()).stream()
                    .map(chapter -> GisuChapterGraphQlResponse.from(gisu.gisuId(), chapter))
                    .toList(),
                (left, right) -> left,
                LinkedHashMap::new
            ));
    }

    @BatchMapping(typeName = "Gisu", field = "schools")
    public Map<GisuGraphQlResponse, List<SchoolDetailGraphQlResponse>> schoolsByGisu(
        List<GisuGraphQlResponse> gisus
    ) {
        Set<Long> gisuIds = gisuIds(gisus);
        Map<Long, List<SchoolDetailInfo>> schoolsByGisuId = getSchoolUseCase.getSchoolListByGisuIds(gisuIds);

        return gisus.stream()
            .collect(Collectors.toMap(
                Function.identity(),
                gisu -> schoolsByGisuId.getOrDefault(gisu.gisuId(), List.of()).stream()
                    .map(SchoolDetailGraphQlResponse::from)
                    .toList(),
                (left, right) -> left,
                LinkedHashMap::new
            ));
    }

    @BatchMapping(typeName = "GisuChapter", field = "schools")
    public Map<GisuChapterGraphQlResponse, List<ChapterSchoolGraphQlResponse>> schoolsByGisuChapter(
        List<GisuChapterGraphQlResponse> chapters
    ) {
        Set<Long> gisuIds = chapters.stream()
            .map(GisuChapterGraphQlResponse::gisuId)
            .collect(Collectors.toSet());
        Map<Long, List<ChapterWithSchoolsInfo>> chaptersByGisuId =
            getChapterUseCase.getChaptersWithSchoolsByGisuIds(gisuIds);
        Map<Long, ChapterWithSchoolsInfo> chapterById = chaptersByGisuId.values().stream()
            .flatMap(List::stream)
            .collect(Collectors.toMap(
                ChapterWithSchoolsInfo::chapterId,
                Function.identity(),
                (left, right) -> left
            ));

        return chapters.stream()
            .collect(Collectors.toMap(
                Function.identity(),
                chapter -> chapterSchools(chapterById, chapter).stream()
                    .map(ChapterSchoolGraphQlResponse::from)
                    .toList(),
                (left, right) -> left,
                LinkedHashMap::new
            ));
    }

    private Set<Long> gisuIds(List<GisuGraphQlResponse> gisus) {
        return gisus.stream()
            .map(GisuGraphQlResponse::gisuId)
            .collect(Collectors.toSet());
    }

    private List<ChapterWithSchoolsInfo.SchoolInfo> chapterSchools(
        Map<Long, ChapterWithSchoolsInfo> chapterById,
        GisuChapterGraphQlResponse chapter
    ) {
        ChapterWithSchoolsInfo chapterWithSchools = chapterById.get(chapter.chapterId());
        return chapterWithSchools == null ? List.of() : chapterWithSchools.schools();
    }
}
