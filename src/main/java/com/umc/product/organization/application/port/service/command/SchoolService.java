package com.umc.product.organization.application.port.service.command;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.command.EvictAuthoritySnapshotCacheUseCase;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.organization.application.port.in.command.ManageSchoolUseCase;
import com.umc.product.organization.application.port.in.command.dto.AssignSchoolCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateSchoolCommand;
import com.umc.product.organization.application.port.in.command.dto.UnassignSchoolCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateSchoolCommand;
import com.umc.product.organization.application.port.out.command.SaveChapterSchoolPort;
import com.umc.product.organization.application.port.out.command.SaveSchoolPort;
import com.umc.product.organization.application.port.out.query.LoadChapterPort;
import com.umc.product.organization.application.port.out.query.LoadSchoolPort;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.School;
import com.umc.product.organization.domain.SchoolLink;
import com.umc.product.storage.application.port.in.command.ManageFileUsageUseCase;
import com.umc.product.storage.application.port.in.command.dto.BulkRemoveFileUsagesCommand;
import com.umc.product.storage.application.port.in.command.dto.ReplaceFileUsagesCommand;
import com.umc.product.storage.domain.FileUsageCoordinate;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SchoolService implements ManageSchoolUseCase {

    private final LoadChapterPort loadChapterPort;
    private final LoadSchoolPort loadSchoolPort;
    private final SaveSchoolPort saveSchoolPort;
    private final SaveChapterSchoolPort saveChapterSchoolPort;
    private final GetMemberUseCase getMemberUseCase;
    private final EvictAuthoritySnapshotCacheUseCase evictAuthoritySnapshotCacheUseCase;
    private final ManageFileUsageUseCase manageFileUsageUseCase;

    @Override
    public Long create(CreateSchoolCommand command) {

        School newSchool = School.create(command.schoolName(), command.remark());
        newSchool.updateLogoImageId(command.logoImageId());

        List<SchoolLink> links = command.links().stream()
            .map(linkCommand -> linkCommand.toEntity(newSchool))
            .toList();

        newSchool.updateLinks(links);

        School savedSchool = saveSchoolPort.save(newSchool);
        replaceLogoUsage(savedSchool, command.requesterMemberId());

        return savedSchool.getId();
    }

    public void updateSchool(Long schoolId, UpdateSchoolCommand command) {

        School school = loadSchoolPort.findById(schoolId);

        school.updateName(command.schoolName());
        school.updateRemark(command.remark());
        school.updateLogoImageId(command.logoImageId());

        if (command.links() != null) {
            List<SchoolLink> newLinks = command.links().stream()
                .map(linkCommand -> linkCommand.toEntity(school))
                .toList();
            school.updateLinks(newLinks);
        }

        if (command.chapterId() != null) {
            Chapter chapter = loadChapterPort.findById(command.chapterId());

            school.updateChapterSchool(chapter);
            evictAuthoritySnapshotsBySchoolId(schoolId);
        }

        School savedSchool = saveSchoolPort.save(school);
        replaceLogoUsage(savedSchool, command.requesterMemberId());
    }

    @Override
    public void deleteSchools(List<Long> schoolIds) {

        if (schoolIds == null || schoolIds.isEmpty()) {
            return;
        }

        Set<Long> memberIds = listMemberIdsBySchoolIds(schoolIds);

        List<FileUsageCoordinate> owners = schoolIds.stream()
            .distinct()
            .map(this::schoolLogoCoordinate)
            .toList();
        manageFileUsageUseCase.removeAll(new BulkRemoveFileUsagesCommand(owners));
        saveChapterSchoolPort.deleteAllBySchoolIds(schoolIds);
        saveSchoolPort.deleteAllLinksBySchoolIds(schoolIds);
        saveSchoolPort.deleteAllByIds(schoolIds);
        evictAuthoritySnapshotCacheUseCase.evictByMemberIds(memberIds);
    }

    @Override
    public void assignToChapter(AssignSchoolCommand command) {
        School school = loadSchoolPort.findSchoolDetailById(command.schoolId());
        Chapter chapter = loadChapterPort.findById(command.chapterId());

        school.assignToChapter(chapter);
        evictAuthoritySnapshotsBySchoolId(command.schoolId());
    }

    @Override
    public void unassignFromChapter(UnassignSchoolCommand command) {
        School school = loadSchoolPort.findSchoolDetailById(command.schoolId());

        school.unassignFromGisu(command.gisuId());
        evictAuthoritySnapshotsBySchoolId(command.schoolId());
    }

    private void evictAuthoritySnapshotsBySchoolId(Long schoolId) {
        evictAuthoritySnapshotCacheUseCase.evictByMemberIds(getMemberUseCase.listIdsBySchoolId(schoolId));
    }

    private Set<Long> listMemberIdsBySchoolIds(List<Long> schoolIds) {
        Set<Long> schoolIdSet = schoolIds.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (schoolIdSet.isEmpty()) {
            return Set.of();
        }

        return getMemberUseCase.listIdsBySchoolIds(schoolIdSet).values().stream()
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
    }

    private void replaceLogoUsage(School school, Long requesterMemberId) {
        manageFileUsageUseCase.replaceUsages(new ReplaceFileUsagesCommand(
            schoolLogoCoordinate(school.getId()),
            school.getLogoImageId() == null ? Set.of() : Set.of(school.getLogoImageId()),
            requesterMemberId
        ));
    }

    private FileUsageCoordinate schoolLogoCoordinate(Long schoolId) {
        return FileUsageCoordinate.of("organization.school", String.valueOf(schoolId), "logo");
    }
}
