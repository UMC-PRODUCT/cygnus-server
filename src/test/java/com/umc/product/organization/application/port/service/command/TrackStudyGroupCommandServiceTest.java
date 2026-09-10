package com.umc.product.organization.application.port.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerBasicInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.organization.application.port.in.command.dto.AddStudyMemberCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateStudyGroupCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateStudyGroupCommand;
import com.umc.product.organization.application.port.out.command.SaveStudyGroupPort;
import com.umc.product.organization.application.port.out.query.LoadGisuPort;
import com.umc.product.organization.application.port.out.query.LoadStudyGroupPort;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.StudyGroup;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

@ExtendWith(MockitoExtension.class)
class TrackStudyGroupCommandServiceTest {

    @Mock
    private LoadStudyGroupPort loadStudyGroupPort;
    @Mock
    private LoadGisuPort loadGisuPort;
    @Mock
    private SaveStudyGroupPort saveStudyGroupPort;
    @Mock
    private GetChallengerUseCase getChallengerUseCase;
    @InjectMocks
    private StudyGroupCommandService service;

    @Test
    void 잠금전_컬렉션에_없어도_DB에_추가된_멤버는_중복추가할_수_없다() {
        // given: 다른 요청이 잠금 대기 중 먼저 같은 그룹에 가입한 상황
        StudyGroup staleGroup = group();
        assertThat(staleGroup.hasMember(1L)).isFalse();
        given(loadStudyGroupPort.getEntityById(100L)).willReturn(staleGroup);
        given(getChallengerUseCase.listBasicByMemberIdsAndGisuId(Set.of(1L), 10L))
            .willReturn(List.of(challenger(ChallengerStatus.ACTIVE, List.of(ChallengerTrack.PLAN))));
        given(loadStudyGroupPort.findMemberIdsByStudyGroupIds(Set.of(100L)))
            .willReturn(Map.of(100L, List.of(3L, 1L)));

        // when & then
        assertThatThrownBy(() -> service.addMember(AddStudyMemberCommand.of(100L, 1L)))
            .extracting("baseCode").isEqualTo(OrganizationErrorCode.STUDY_GROUP_MEMBER_DUPLICATED);
        assertThat(staleGroup.hasMember(1L)).isFalse();
        verify(saveStudyGroupPort, never()).save(any());
        var order = inOrder(loadGisuPort, loadStudyGroupPort);
        order.verify(loadStudyGroupPort).getEntityById(100L);
        order.verify(loadGisuPort).getByIdForUpdate(10L);
        order.verify(loadStudyGroupPort).findConflictedTrackMemberIds(10L, ChallengerTrack.PLAN, Set.of(1L), 100L);
        order.verify(loadStudyGroupPort).findMemberIdsByStudyGroupIds(Set.of(100L));
    }

    @Test
    void 최신_DB에_아직_소속되지_않은_수강자는_멤버추가할_수_있다() {
        // given
        StudyGroup group = group();
        given(loadStudyGroupPort.getEntityById(100L)).willReturn(group);
        given(getChallengerUseCase.listBasicByMemberIdsAndGisuId(Set.of(1L), 10L))
            .willReturn(List.of(challenger(ChallengerStatus.ACTIVE, List.of(ChallengerTrack.PLAN))));
        given(loadStudyGroupPort.findMemberIdsByStudyGroupIds(Set.of(100L)))
            .willReturn(Map.of(100L, List.of(3L)));

        // when
        service.addMember(AddStudyMemberCommand.of(100L, 1L));

        // then
        assertThat(group.hasMember(1L)).isTrue();
        verify(saveStudyGroupPort).save(group);
    }

    @Test
    void 활성_챌린저는_수강하는_여러_기본트랙의_스터디에_참여할_수_있다() {
        // given
        given(loadGisuPort.getById(10L)).willReturn(gisu(GisuLearningType.TRACK));
        given(getChallengerUseCase.listBasicByMemberIdsAndGisuId(Set.of(1L), 10L))
            .willReturn(List.of(challenger(ChallengerStatus.ACTIVE,
                List.of(ChallengerTrack.PLAN, ChallengerTrack.WEB_PRODUCT_ENGINEER))));

        // when
        service.create(command(ChallengerTrack.PLAN));
        service.create(command(ChallengerTrack.WEB_PRODUCT_ENGINEER));

        // then
        ArgumentCaptor<StudyGroup> captor = ArgumentCaptor.forClass(StudyGroup.class);
        verify(saveStudyGroupPort, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(StudyGroup::getTrack)
            .containsExactly(ChallengerTrack.PLAN, ChallengerTrack.WEB_PRODUCT_ENGINEER);
        assertThat(captor.getAllValues()).allSatisfy(group -> assertThat(group.getPart()).isNull());
        var order = inOrder(loadGisuPort, getChallengerUseCase, loadStudyGroupPort, saveStudyGroupPort);
        order.verify(loadGisuPort).getById(10L);
        order.verify(loadGisuPort).getByIdForUpdate(10L);
        order.verify(getChallengerUseCase).listBasicByMemberIdsAndGisuId(Set.of(1L), 10L);
        order.verify(loadStudyGroupPort).findConflictedTrackMemberIds(10L, ChallengerTrack.PLAN, Set.of(1L), null);
        order.verify(saveStudyGroupPort).save(any());
    }

    @Test
    void 트랙기수에서_파트로_스터디를_생성할_수_없다() {
        // given
        given(loadGisuPort.getById(10L)).willReturn(gisu(GisuLearningType.TRACK));

        // when & then
        assertThatThrownBy(() -> service.create(new CreateStudyGroupCommand(
            "파트 스터디", 10L, ChallengerPart.WEB, Set.of(2L), Set.of(1L))))
            .isInstanceOf(OrganizationDomainException.class)
            .extracting("baseCode").isEqualTo(OrganizationErrorCode.STUDY_GROUP_LEARNING_TYPE_INVALID);
        verify(saveStudyGroupPort, never()).save(any());
    }

    @Test
    void 파트기수에서_트랙으로_스터디를_생성할_수_없다() {
        // given
        given(loadGisuPort.getById(10L)).willReturn(gisu(GisuLearningType.PART));

        // when & then
        assertThatThrownBy(() -> service.create(command(ChallengerTrack.PLAN)))
            .extracting("baseCode").isEqualTo(OrganizationErrorCode.STUDY_GROUP_LEARNING_TYPE_INVALID);
    }

    @Test
    void 플러스트랙은_스터디를_생성할_수_없다() {
        // given
        given(loadGisuPort.getById(10L)).willReturn(gisu(GisuLearningType.TRACK));

        // when & then
        assertThatThrownBy(() -> service.create(command(ChallengerTrack.INFRA_PLUS)))
            .extracting("baseCode").isEqualTo(OrganizationErrorCode.STUDY_GROUP_LEARNING_TYPE_INVALID);
    }

    @Test
    void 해당트랙을_수강하지_않는_챌린저는_참여할_수_없다() {
        // given
        given(loadGisuPort.getById(10L)).willReturn(gisu(GisuLearningType.TRACK));
        given(getChallengerUseCase.listBasicByMemberIdsAndGisuId(Set.of(1L), 10L))
            .willReturn(List.of(challenger(ChallengerStatus.ACTIVE, List.of(ChallengerTrack.DESIGN))));

        // when & then
        assertThatThrownBy(() -> service.create(command(ChallengerTrack.PLAN)))
            .extracting("baseCode").isEqualTo(OrganizationErrorCode.STUDY_GROUP_TRACK_MEMBER_INVALID);
    }

    @Test
    void 해당기수_챌린저가_아니면_참여할_수_없다() {
        // given
        given(loadGisuPort.getById(10L)).willReturn(gisu(GisuLearningType.TRACK));
        given(getChallengerUseCase.listBasicByMemberIdsAndGisuId(Set.of(1L), 10L)).willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> service.create(command(ChallengerTrack.PLAN)))
            .extracting("baseCode").isEqualTo(OrganizationErrorCode.STUDY_GROUP_TRACK_MEMBER_INVALID);
    }

    @Test
    void 활동상태가_아니면_멤버추가도_거부한다() {
        // given
        StudyGroup group = group();
        given(loadStudyGroupPort.getEntityById(100L)).willReturn(group);
        given(getChallengerUseCase.listBasicByMemberIdsAndGisuId(Set.of(1L), 10L))
            .willReturn(List.of(challenger(ChallengerStatus.WITHDRAWN, List.of(ChallengerTrack.PLAN))));

        // when & then
        assertThatThrownBy(() -> service.addMember(AddStudyMemberCommand.of(100L, 1L)))
            .extracting("baseCode").isEqualTo(OrganizationErrorCode.STUDY_GROUP_TRACK_MEMBER_INVALID);
        assertThat(group.hasMember(1L)).isFalse();
    }

    @Test
    void 같은_기수와_트랙의_다른_스터디에_속하면_생성을_거부한다() {
        // given
        given(loadGisuPort.getById(10L)).willReturn(gisu(GisuLearningType.TRACK));
        given(getChallengerUseCase.listBasicByMemberIdsAndGisuId(Set.of(1L), 10L))
            .willReturn(List.of(challenger(ChallengerStatus.ACTIVE, List.of(ChallengerTrack.PLAN))));
        given(loadStudyGroupPort.findConflictedTrackMemberIds(10L, ChallengerTrack.PLAN, Set.of(1L), null))
            .willReturn(Set.of(1L));

        // when & then
        assertThatThrownBy(() -> service.create(command(ChallengerTrack.PLAN)))
            .extracting("baseCode").isEqualTo(OrganizationErrorCode.STUDY_GROUP_MEMBER_ALREADY_IN_TRACK_STUDY);
    }

    @Test
    void 같은_기수와_트랙의_다른_스터디에_속하면_멤버추가를_거부한다() {
        // given
        given(loadStudyGroupPort.getEntityById(100L)).willReturn(group());
        given(getChallengerUseCase.listBasicByMemberIdsAndGisuId(Set.of(1L), 10L))
            .willReturn(List.of(challenger(ChallengerStatus.ACTIVE, List.of(ChallengerTrack.PLAN))));
        given(loadStudyGroupPort.findConflictedTrackMemberIds(10L, ChallengerTrack.PLAN, Set.of(1L), 100L))
            .willReturn(Set.of(1L));

        // when & then
        assertThatThrownBy(() -> service.addMember(AddStudyMemberCommand.of(100L, 1L)))
            .extracting("baseCode").isEqualTo(OrganizationErrorCode.STUDY_GROUP_MEMBER_ALREADY_IN_TRACK_STUDY);
    }

    @Test
    void 생성한_스터디의_트랙은_변경할_수_없다() {
        // given
        given(loadStudyGroupPort.getEntityById(100L)).willReturn(group());

        // when & then
        assertThatThrownBy(() -> service.update(
            new UpdateStudyGroupCommand(100L, null, null, ChallengerTrack.DESIGN)))
            .extracting("baseCode").isEqualTo(OrganizationErrorCode.STUDY_GROUP_TRACK_IMMUTABLE);
    }

    private CreateStudyGroupCommand command(ChallengerTrack track) {
        return new CreateStudyGroupCommand("트랙 스터디", 10L, null, Set.of(2L), Set.of(1L), track);
    }

    private ChallengerBasicInfo challenger(ChallengerStatus status, List<ChallengerTrack> tracks) {
        return new ChallengerBasicInfo(11L, 1L, 10L, null, tracks, status);
    }

    private Gisu gisu(GisuLearningType learningType) {
        Gisu gisu = Gisu.create(11L, Instant.parse("2026-09-01T00:00:00Z"),
            Instant.parse("2027-03-01T00:00:00Z"), false, learningType);
        ReflectionTestUtils.setField(gisu, "id", 10L);
        return gisu;
    }

    private StudyGroup group() {
        StudyGroup group = StudyGroup.create("기획 스터디", 10L, null, ChallengerTrack.PLAN, Set.of(3L), Set.of(2L));
        ReflectionTestUtils.setField(group, "id", 100L);
        return group;
    }
}
