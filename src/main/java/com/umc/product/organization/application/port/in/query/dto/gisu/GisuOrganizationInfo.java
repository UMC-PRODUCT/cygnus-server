package com.umc.product.organization.application.port.in.query.dto.gisu;

import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterWithSchoolsInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.organization.domain.enums.SchoolLinkType;

public record GisuOrganizationInfo(
    Long gisuId,
    Long generation,
    Instant startAt,
    Instant endAt,
    boolean isActive,
    List<ChapterOrganizationInfo> chapters,
    List<SchoolOrganizationInfo> schools,
    GisuLearningType learningType
) {

    public GisuOrganizationInfo(
        Long gisuId, Long generation, Instant startAt, Instant endAt, boolean isActive,
        List<ChapterOrganizationInfo> chapters, List<SchoolOrganizationInfo> schools
    ) {
        this(gisuId, generation, startAt, endAt, isActive, chapters, schools, GisuLearningType.PART);
    }

    public GisuOrganizationInfo {
        chapters = chapters == null ? List.of() : List.copyOf(chapters);
        schools = schools == null ? List.of() : List.copyOf(schools);
    }

    public static GisuOrganizationInfo of(
        GisuInfo gisuInfo,
        List<ChapterOrganizationInfo> chapters,
        List<SchoolOrganizationInfo> schools
    ) {
        return new GisuOrganizationInfo(
            gisuInfo.gisuId(),
            gisuInfo.generation(),
            gisuInfo.startAt(),
            gisuInfo.endAt(),
            gisuInfo.isActive(),
            chapters,
            schools,
            gisuInfo.learningType()
        );
    }

    public record ChapterOrganizationInfo(
        Long chapterId,
        String chapterName,
        List<ChapterSchoolInfo> schools
    ) {

        public ChapterOrganizationInfo {
            schools = schools == null ? List.of() : List.copyOf(schools);
        }

        public static ChapterOrganizationInfo from(ChapterInfo info) {
            return new ChapterOrganizationInfo(info.id(), info.name(), List.of());
        }

        public static ChapterOrganizationInfo from(ChapterWithSchoolsInfo info) {
            List<ChapterSchoolInfo> schools = info.schools().stream()
                .map(ChapterSchoolInfo::from)
                .toList();
            return new ChapterOrganizationInfo(info.chapterId(), info.chapterName(), schools);
        }
    }

    public record ChapterSchoolInfo(
        Long schoolId,
        String schoolName
    ) {

        public static ChapterSchoolInfo from(ChapterWithSchoolsInfo.SchoolInfo info) {
            return new ChapterSchoolInfo(info.schoolId(), info.schoolName());
        }
    }

    public record SchoolOrganizationInfo(
        Long chapterId,
        String chapterName,
        Long schoolId,
        String schoolName,
        String shortName,
        String remark,
        String logoImageUrl,
        List<SchoolLinkInfo> links,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
    ) {

        public SchoolOrganizationInfo {
            links = links == null ? List.of() : List.copyOf(links);
        }

        public static SchoolOrganizationInfo from(SchoolDetailInfo info) {
            List<SchoolDetailInfo.SchoolLinkItem> sourceLinks = info.links() == null ? List.of() : info.links();
            List<SchoolLinkInfo> links = sourceLinks.stream()
                .map(SchoolLinkInfo::from)
                .toList();

            return new SchoolOrganizationInfo(
                info.chapterId(),
                info.chapterName(),
                info.schoolId(),
                info.schoolName(),
                info.shortName(),
                info.remark(),
                info.logoImageUrl(),
                links,
                info.isActive(),
                info.createdAt(),
                info.updatedAt()
            );
        }
    }

    public record SchoolLinkInfo(
        String title,
        SchoolLinkType type,
        String url
    ) {

        public static SchoolLinkInfo from(SchoolDetailInfo.SchoolLinkItem info) {
            return new SchoolLinkInfo(info.title(), info.type(), info.url());
        }
    }
}
