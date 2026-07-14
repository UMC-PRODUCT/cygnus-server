package com.umc.product.organization.application.port.service.query;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolGisuChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolLinkInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolNameInfo;
import com.umc.product.organization.application.port.in.query.dto.school.UnassignedSchoolInfo;
import com.umc.product.organization.application.port.out.query.LoadSchoolPort;
import com.umc.product.organization.domain.School;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;
import com.umc.product.storage.application.port.in.query.dto.FileInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SchoolQueryService implements GetSchoolUseCase {


    private final LoadSchoolPort loadSchoolPort;
    private final GetFileUseCase getFileUseCase;

    @Override
    public List<SchoolNameInfo> getAllSchoolNames() {
        return loadSchoolPort.findAllNames();
    }

    @Override
    public SchoolDetailInfo getSchoolDetail(Long schoolId) {

        SchoolChapterInfo schoolInfoWithoutSchoolLinkItem = loadSchoolPort.findSchoolDetailByIdWithActiveChapter(
            schoolId);

        String logoImageUrl = null;
        if (schoolInfoWithoutSchoolLinkItem.logoImageId() != null) {
            FileInfo fileInfo = getFileUseCase.getById(schoolInfoWithoutSchoolLinkItem.logoImageId());
            logoImageUrl = fileInfo.fileLink();
        }

        List<SchoolDetailInfo.SchoolLinkItem> links = loadSchoolPort.findLinksBySchoolId(schoolId);

        return toSchoolDetailInfo(schoolInfoWithoutSchoolLinkItem, logoImageUrl, links);

    }

    @Override
    public SchoolLinkInfo getSchoolLink(Long schoolId) {

        School school = loadSchoolPort.findById(schoolId);

        return SchoolLinkInfo.from(school);

    }

    @Override
    public List<SchoolDetailInfo> listDetailsByIds(Set<Long> schoolIds) {
        if (schoolIds.isEmpty()) {
            return List.of();
        }

        List<SchoolChapterInfo> schools = loadSchoolPort.findSchoolDetailsByIds(schoolIds);
        if (schools.isEmpty()) {
            return List.of();
        }

        List<Long> orderedSchoolIds = schools.stream()
            .map(SchoolChapterInfo::schoolId)
            .toList();
        Map<Long, List<SchoolDetailInfo.SchoolLinkItem>> linksMap =
            loadSchoolPort.findLinksBySchoolIds(orderedSchoolIds);

        List<String> logoImageIds = schools.stream()
            .map(SchoolChapterInfo::logoImageId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        Map<String, String> logoImageUrls = logoImageIds.isEmpty()
            ? Map.of()
            : getFileUseCase.getFileLinks(logoImageIds);

        return schools.stream()
            .map(school -> toSchoolDetailInfo(
                school,
                logoImageUrl(logoImageUrls, school.logoImageId()),
                linksMap.getOrDefault(school.schoolId(), List.of())
            ))
            .toList();
    }

    @Override
    public List<UnassignedSchoolInfo> getUnassignedSchools(Long gisuId) {
        return loadSchoolPort.findUnassignedByGisuId(gisuId).stream()
            .map(UnassignedSchoolInfo::from)
            .toList();
    }

    @Override
    public List<SchoolDetailInfo> getSchoolListByGisuId(Long gisuId) {

        List<SchoolChapterInfo> schools = loadSchoolPort.findSchoolDetailsByGisuId(gisuId);
        if (schools.isEmpty()) {
            return List.of();
        }

        List<Long> schoolIds = schools.stream().map(SchoolChapterInfo::schoolId).toList();

        Map<Long, List<SchoolDetailInfo.SchoolLinkItem>> linksMap = loadSchoolPort.findLinksBySchoolIds(schoolIds);

        List<String> logoImageIds = schools.stream()
            .map(SchoolChapterInfo::logoImageId)
            .filter(Objects::nonNull)
            .toList();

        Map<String, String> logoImageUrls = logoImageIds.isEmpty()
            ? Map.of()
            : getFileUseCase.getFileLinks(logoImageIds);

        return schools.stream()
            .map(school -> toSchoolDetailInfo(
                school,
                logoImageUrl(logoImageUrls, school.logoImageId()),
                linksMap.getOrDefault(school.schoolId(), List.of())
            ))
            .toList();
    }

    @Override
    public Map<Long, List<SchoolDetailInfo>> getSchoolListByGisuIds(Set<Long> gisuIds) {
        if (gisuIds.isEmpty()) {
            return Map.of();
        }

        List<SchoolGisuChapterInfo> schools = loadSchoolPort.findSchoolDetailsByGisuIds(gisuIds);
        if (schools.isEmpty()) {
            return Map.of();
        }

        List<Long> schoolIds = schools.stream()
            .map(SchoolGisuChapterInfo::schoolId)
            .distinct()
            .toList();

        Map<Long, List<SchoolDetailInfo.SchoolLinkItem>> linksMap = loadSchoolPort.findLinksBySchoolIds(schoolIds);

        List<String> logoImageIds = schools.stream()
            .map(SchoolGisuChapterInfo::logoImageId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        Map<String, String> logoImageUrls = logoImageIds.isEmpty()
            ? Map.of()
            : getFileUseCase.getFileLinks(logoImageIds);

        return schools.stream()
            .collect(Collectors.groupingBy(
                SchoolGisuChapterInfo::gisuId,
                Collectors.mapping(
                    school -> toSchoolDetailInfo(
                        school,
                        logoImageUrl(logoImageUrls, school.logoImageId()),
                        linksMap.getOrDefault(school.schoolId(), List.of())
                    ),
                    Collectors.toList()
                )
            ));
    }

    @Override
    public List<SchoolNameInfo> batchGetNamesByIds(Set<Long> schoolIds) {
        if (schoolIds == null || schoolIds.isEmpty()) {
            return List.of();
        }
        List<School> schools = loadSchoolPort.findAllByIds(List.copyOf(schoolIds));
        if (schools.size() != schoolIds.size()) {
            throw new IllegalStateException("요청한 학교 이름을 모두 조회하지 못했습니다.");
        }
        return schools.stream().map(SchoolNameInfo::from).toList();
    }

    private SchoolDetailInfo toSchoolDetailInfo(SchoolChapterInfo info, String logoImageUrl,
                                                List<SchoolDetailInfo.SchoolLinkItem> links) {
        return new SchoolDetailInfo(
            info.chapterId(),
            info.chapterName(),
            info.schoolName(),
            info.schoolId(),
            info.remark(),
            logoImageUrl,
            links,
            info.isActive(),
            info.createdAt(),
            info.updatedAt()
        );
    }

    private String logoImageUrl(Map<String, String> logoImageUrls, String logoImageId) {
        if (logoImageId == null) {
            return null;
        }
        return logoImageUrls.get(logoImageId);
    }

    private SchoolDetailInfo toSchoolDetailInfo(SchoolGisuChapterInfo info, String logoImageUrl,
                                                List<SchoolDetailInfo.SchoolLinkItem> links) {
        return new SchoolDetailInfo(
            info.chapterId(),
            info.chapterName(),
            info.schoolName(),
            info.schoolId(),
            info.remark(),
            logoImageUrl,
            links,
            info.isActive(),
            info.createdAt(),
            info.updatedAt()
        );
    }
}
