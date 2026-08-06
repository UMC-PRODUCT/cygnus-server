package com.umc.product.organization.application.port.service.command;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.command.EvictAuthoritySnapshotCacheUseCase;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.organization.application.port.in.command.ManageChapterUseCase;
import com.umc.product.organization.application.port.in.command.dto.CreateChapterCommand;
import com.umc.product.organization.application.port.out.command.SaveChapterPort;
import com.umc.product.organization.application.port.out.command.SaveChapterSchoolPort;
import com.umc.product.organization.application.port.out.query.LoadChapterPort;
import com.umc.product.organization.application.port.out.query.LoadChapterSchoolPort;
import com.umc.product.organization.application.port.out.query.LoadGisuPort;
import com.umc.product.organization.application.port.out.query.LoadSchoolPort;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.ChapterSchool;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.School;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ChapterService implements ManageChapterUseCase {

    private final LoadGisuPort loadGisuPort;
    private final LoadChapterPort loadChapterPort;
    private final LoadSchoolPort loadSchoolPort;
    private final LoadChapterSchoolPort loadChapterSchoolPort;
    private final SaveChapterPort saveChapterPort;
    private final SaveChapterSchoolPort saveChapterSchoolPort;
    private final GetMemberUseCase getMemberUseCase;
    private final EvictAuthoritySnapshotCacheUseCase evictAuthoritySnapshotCacheUseCase;

    @Override
    public Long create(CreateChapterCommand command) {
        Gisu gisu = loadGisuPort.getById(command.gisuId());
        validateChapterNameNotDuplicated(command.gisuId(), command.name());

        Chapter chapter = Chapter.create(gisu, command.name());

        Chapter savedChapter = saveChapterPort.save(chapter);

        if (!command.schoolIds().isEmpty()) {
            List<School> schools = loadSchoolPort.findAllByIds(command.schoolIds());
            validateAllSchoolsExist(command.schoolIds(), schools);
            validateSchoolsNotAssignedInGisu(command.schoolIds(), command.gisuId());

            for (School school : schools) {
                ChapterSchool chapterSchool = ChapterSchool.create(savedChapter, school);
                saveChapterSchoolPort.save(chapterSchool);
            }
            evictAuthoritySnapshotsBySchoolIds(new HashSet<>(command.schoolIds()));
        }

        return savedChapter.getId();
    }

    @Override
    public void delete(Long chapterId) {
        Chapter chapter = loadChapterPort.findById(chapterId);
        Set<Long> schoolIds = loadChapterSchoolPort.findByGisuId(chapter.getGisu().getId()).stream()
            .filter(chapterSchool -> Objects.equals(chapterSchool.getChapter().getId(), chapterId))
            .map(chapterSchool -> chapterSchool.getSchool().getId())
            .collect(Collectors.toSet());

        saveChapterSchoolPort.deleteAllByChapterId(chapterId);
        saveChapterPort.delete(chapter);
        evictAuthoritySnapshotsBySchoolIds(schoolIds);
    }

    private void validateAllSchoolsExist(List<Long> requestedIds, List<School> foundSchools) {
        Set<Long> requestedSet = new HashSet<>(requestedIds);
        Set<Long> foundSet = foundSchools.stream()
            .map(School::getId)
            .collect(Collectors.toSet());

        if (!foundSet.containsAll(requestedSet)) {
            throw new OrganizationDomainException(OrganizationErrorCode.SCHOOL_NOT_FOUND);
        }
    }

    private void validateChapterNameNotDuplicated(Long gisuId, String name) {
        boolean duplicated = loadChapterPort.findByGisuId(gisuId).stream()
            .anyMatch(chapter -> chapter.getName().equals(name));

        if (duplicated) {
            throw new OrganizationDomainException(OrganizationErrorCode.CHAPTER_NAME_DUPLICATED);
        }
    }

    private void validateSchoolsNotAssignedInGisu(List<Long> schoolIds, Long gisuId) {
        Set<Long> requestedSet = new HashSet<>(schoolIds);

        Set<Long> alreadyAssignedSchoolIds = loadChapterSchoolPort.findByGisuId(gisuId).stream()
            .map(cs -> cs.getSchool().getId())
            .collect(Collectors.toSet());

        requestedSet.retainAll(alreadyAssignedSchoolIds);

        if (!requestedSet.isEmpty()) {
            throw new OrganizationDomainException(OrganizationErrorCode.SCHOOL_ALREADY_ASSIGNED_TO_CHAPTER);
        }
    }

    private void evictAuthoritySnapshotsBySchoolIds(Set<Long> schoolIds) {
        if (schoolIds.isEmpty()) {
            return;
        }

        Set<Long> memberIds = getMemberUseCase.listIdsBySchoolIds(schoolIds).values().stream()
            .flatMap(Set::stream)
            .collect(Collectors.toSet());

        evictAuthoritySnapshotCacheUseCase.evictByMemberIds(memberIds);
    }
}
