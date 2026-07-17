package com.umc.product.organization.application.port.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.command.EvictAuthoritySnapshotCacheUseCase;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.organization.application.port.in.command.dto.CreateSchoolCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateSchoolCommand;
import com.umc.product.organization.application.port.out.command.SaveChapterSchoolPort;
import com.umc.product.organization.application.port.out.command.SaveSchoolPort;
import com.umc.product.organization.application.port.out.query.LoadChapterPort;
import com.umc.product.organization.application.port.out.query.LoadSchoolPort;
import com.umc.product.organization.domain.School;
import com.umc.product.storage.application.port.in.command.ManageFileUsageUseCase;
import com.umc.product.storage.application.port.in.command.dto.BulkRemoveFileUsagesCommand;
import com.umc.product.storage.application.port.in.command.dto.ReplaceFileUsagesCommand;
import com.umc.product.storage.domain.FileUsageCoordinate;

@ExtendWith(MockitoExtension.class)
class SchoolFileUsageSynchronizationTest {

    @Mock
    LoadChapterPort loadChapterPort;
    @Mock
    LoadSchoolPort loadSchoolPort;
    @Mock
    SaveSchoolPort saveSchoolPort;
    @Mock
    SaveChapterSchoolPort saveChapterSchoolPort;
    @Mock
    GetMemberUseCase getMemberUseCase;
    @Mock
    EvictAuthoritySnapshotCacheUseCase evictAuthoritySnapshotCacheUseCase;
    @Mock
    ManageFileUsageUseCase manageFileUsageUseCase;

    @Test
    void 학교_생성은_저장된_로고_snapshot을_정확한_owner에_등록한다() {
        SchoolFileUsageSynchronizationTestFixture fixture = fixture();
        School persisted = school(7L, "logo-file");
        given(saveSchoolPort.save(any(School.class))).willReturn(persisted);

        fixture.service().create(CreateSchoolCommand.of(
            55L,
            "한성대",
            "비고",
            "logo-file",
            List.of()
        ));

        then(manageFileUsageUseCase).should().replaceUsages(new ReplaceFileUsagesCommand(
            FileUsageCoordinate.of("organization.school", "7", "logo"),
            java.util.Set.of("logo-file"),
            55L
        ));
    }

    @Test
    void 학교_수정의_null은_logo를_유지하고_빈_문자열은_clear한다() {
        SchoolFileUsageSynchronizationTestFixture fixture = fixture();
        School school = school(7L, "old-logo");
        given(loadSchoolPort.findById(7L)).willReturn(school);
        given(saveSchoolPort.save(school)).willReturn(school);

        fixture.service().updateSchool(7L, new UpdateSchoolCommand(55L, "학교", null, "비고", null, null));
        then(manageFileUsageUseCase).should().replaceUsages(new ReplaceFileUsagesCommand(
            FileUsageCoordinate.of("organization.school", "7", "logo"),
            java.util.Set.of("old-logo"),
            55L
        ));

        fixture.service().updateSchool(7L, new UpdateSchoolCommand(55L, "학교", null, "비고", "", null));
        assertThat(school.getLogoImageId()).isNull();
        then(manageFileUsageUseCase).should().replaceUsages(new ReplaceFileUsagesCommand(
            FileUsageCoordinate.of("organization.school", "7", "logo"),
            java.util.Set.of(),
            55L
        ));
    }

    @Test
    void 학교_삭제는_aggregate_delete보다_먼저_usage를_비운다() {
        SchoolFileUsageSynchronizationTestFixture fixture = fixture();
        given(getMemberUseCase.listIdsBySchoolIds(Set.of(7L, 8L))).willReturn(Map.of());
        fixture.service().deleteSchools(List.of(7L, 8L));

        InOrder order = inOrder(manageFileUsageUseCase, saveChapterSchoolPort, saveSchoolPort);
        order.verify(manageFileUsageUseCase).removeAll(any(BulkRemoveFileUsagesCommand.class));
        order.verify(saveChapterSchoolPort).deleteAllBySchoolIds(List.of(7L, 8L));
        order.verify(saveSchoolPort).deleteAllLinksBySchoolIds(List.of(7L, 8L));
        order.verify(saveSchoolPort).deleteAllByIds(List.of(7L, 8L));

        ArgumentCaptor<BulkRemoveFileUsagesCommand> captor =
            ArgumentCaptor.forClass(BulkRemoveFileUsagesCommand.class);
        then(manageFileUsageUseCase).should().removeAll(captor.capture());
        assertThat(captor.getValue().owners()).containsExactly(
            FileUsageCoordinate.of("organization.school", "7", "logo"),
            FileUsageCoordinate.of("organization.school", "8", "logo")
        );
    }

    private SchoolFileUsageSynchronizationTestFixture fixture() {
        return new SchoolFileUsageSynchronizationTestFixture(new SchoolService(
            loadChapterPort,
            loadSchoolPort,
            saveSchoolPort,
            saveChapterSchoolPort,
            getMemberUseCase,
            evictAuthoritySnapshotCacheUseCase,
            manageFileUsageUseCase
        ));
    }

    private School school(Long id, String logoImageId) {
        School school = School.create("학교", "비고");
        school.updateLogoImageId(logoImageId);
        ReflectionTestUtils.setField(school, "id", id);
        return school;
    }

    private record SchoolFileUsageSynchronizationTestFixture(SchoolService service) {
    }
}
