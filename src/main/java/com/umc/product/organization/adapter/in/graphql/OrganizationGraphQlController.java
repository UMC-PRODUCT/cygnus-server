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

import com.umc.product.global.graphql.relay.ConnectionArguments;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.graphql.relay.RelayConnection;
import com.umc.product.organization.adapter.in.graphql.dto.ChapterGraphQlResponse;
import com.umc.product.organization.adapter.in.graphql.dto.ChapterSchoolGraphQlResponse;
import com.umc.product.organization.adapter.in.graphql.dto.GisuChapterGraphQlResponse;
import com.umc.product.organization.adapter.in.graphql.dto.GisuFilterGraphQlRequest;
import com.umc.product.organization.adapter.in.graphql.dto.GisuGraphQlResponse;
import com.umc.product.organization.adapter.in.graphql.dto.SchoolGraphQlResponse;
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
    public RelayConnection<GisuGraphQlResponse> gisus(
        @Argument GisuFilterGraphQlRequest filter,
        @Argument Integer first,
        @Argument String after,
        @Argument Integer last,
        @Argument String before
    ) {
        ConnectionArguments arguments = ConnectionArguments.of(first, after, last, before);
        GisuFilterGraphQlRequest effectiveFilter = filter == null ? GisuFilterGraphQlRequest.empty() : filter;
        return RelayConnection.fromList(
            getGisuOrganizationUseCase.get(effectiveFilter.toQuery()),
            arguments,
            GisuGraphQlResponse::from
        );
    }

    @QueryMapping
    public GisuGraphQlResponse gisu(@Argument String id) {
        return GisuGraphQlResponse.from(getGisuUseCase.getById(GlobalId.decodeLong(id, GlobalIdTypes.GISU)));
    }

    @QueryMapping
    public GisuGraphQlResponse activeGisu() {
        return GisuGraphQlResponse.from(getGisuUseCase.getActiveGisu());
    }

    @QueryMapping
    public RelayConnection<ChapterGraphQlResponse> chapters(
        @Argument Integer first,
        @Argument String after,
        @Argument Integer last,
        @Argument String before
    ) {
        ConnectionArguments arguments = ConnectionArguments.of(first, after, last, before);
        return RelayConnection.fromList(
            getChapterUseCase.getAllChapters(),
            arguments,
            ChapterGraphQlResponse::from
        );
    }

    @QueryMapping
    public ChapterGraphQlResponse chapter(@Argument String id) {
        return ChapterGraphQlResponse.from(
            getChapterUseCase.getChapterById(GlobalId.decodeLong(id, GlobalIdTypes.CHAPTER))
        );
    }

    @QueryMapping
    public RelayConnection<SchoolNameGraphQlResponse> schools(
        @Argument Integer first,
        @Argument String after,
        @Argument Integer last,
        @Argument String before
    ) {
        ConnectionArguments arguments = ConnectionArguments.of(first, after, last, before);
        return RelayConnection.fromList(
            getSchoolUseCase.getAllSchoolNames(),
            arguments,
            SchoolNameGraphQlResponse::from
        );
    }

    @QueryMapping
    public SchoolGraphQlResponse school(@Argument String id) {
        return SchoolGraphQlResponse.from(
            getSchoolUseCase.getSchoolDetail(GlobalId.decodeLong(id, GlobalIdTypes.SCHOOL))
        );
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
    public Map<GisuGraphQlResponse, List<SchoolGraphQlResponse>> schoolsByGisu(
        List<GisuGraphQlResponse> gisus
    ) {
        Set<Long> gisuIds = gisuIds(gisus);
        Map<Long, List<SchoolDetailInfo>> schoolsByGisuId = getSchoolUseCase.getSchoolListByGisuIds(gisuIds);

        return gisus.stream()
            .collect(Collectors.toMap(
                Function.identity(),
                gisu -> schoolsByGisuId.getOrDefault(gisu.gisuId(), List.of()).stream()
                    .map(SchoolGraphQlResponse::from)
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
        ChapterWithSchoolsInfo chapterWithSchools = chapterById.get(chapter.rawChapterId());
        return chapterWithSchools == null ? List.of() : chapterWithSchools.schools();
    }
}
