package com.umc.product.challenger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.command.EvictAuthoritySnapshotCacheUseCase;
import com.umc.product.authorization.application.port.in.command.ManageChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.command.dto.CreateChallengerRoleCommand;
import com.umc.product.challenger.application.port.in.command.dto.ConsumeChallengerRecordCommand;
import com.umc.product.challenger.application.port.in.command.dto.CreateChallengerRecordCommand;
import com.umc.product.challenger.application.port.out.LoadChallengerPort;
import com.umc.product.challenger.application.port.out.LoadChallengerRecordPort;
import com.umc.product.challenger.application.port.out.SaveChallengerPort;
import com.umc.product.challenger.application.port.out.SaveChallengerRecordPort;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.challenger.domain.ChallengerRecord;
import com.umc.product.challenger.domain.exception.ChallengerDomainException;
import com.umc.product.challenger.domain.exception.ChallengerErrorCode;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.notification.application.port.in.SendWebhookAlarmUseCase;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengerRecordCommandService")
class ChallengerRecordCommandServiceTest {

    @Mock
    SaveChallengerRecordPort saveChallengerRecordPort;

    @Mock
    LoadChallengerRecordPort loadChallengerRecordPort;

    @Mock
    SaveChallengerPort saveChallengerPort;

    @Mock
    LoadChallengerPort loadChallengerPort;

    @Mock
    GetChapterUseCase getChapterUseCase;

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Mock
    ManageChallengerRoleUseCase manageChallengerRoleUseCase;

    @Mock
    EvictAuthoritySnapshotCacheUseCase evictAuthoritySnapshotCacheUseCase;

    @Mock
    SendWebhookAlarmUseCase sendWebhookAlarmUseCase;

    @Mock
    GetGisuUseCase getGisuUseCase;

    @BeforeEach
    void 기본_기수는_파트_학습을_사용한다() {
        GisuInfo gisu = mock(GisuInfo.class);
        lenient().when(gisu.learningType()).thenReturn(GisuLearningType.PART);
        lenient().when(getGisuUseCase.getById(9L)).thenReturn(gisu);
    }

    @InjectMocks
    ChallengerRecordCommandService sut;

    @Test
    @DisplayName("학교가 요청 지부에 속하면 기록 코드를 생성한다")
    void 학교가_요청_지부에_속하면_기록_코드를_생성한다() {
        CreateChallengerRecordCommand command = recordCommand();
        given(getChapterUseCase.byGisuAndSchool(9L, 3L)).willReturn(new ChapterInfo(2L, "서울"));
        given(saveChallengerRecordPort.save(any(ChallengerRecord.class))).willAnswer(invocation -> {
            ChallengerRecord record = invocation.getArgument(0);
            ReflectionTestUtils.setField(record, "id", 10L);
            return record;
        });

        Long result = sut.create(command);

        assertThat(result).isEqualTo(10L);
        then(saveChallengerRecordPort).should().save(any(ChallengerRecord.class));
    }

    @Test
    @DisplayName("학교가 요청 지부에 속하지 않으면 기록 코드를 생성하지 않는다")
    void 학교가_요청_지부에_속하지_않으면_기록_코드를_생성하지_않는다() {
        given(getChapterUseCase.byGisuAndSchool(9L, 3L)).willReturn(new ChapterInfo(99L, "다른 지부"));

        assertThatThrownBy(() -> sut.create(recordCommand()))
            .isInstanceOf(ChallengerDomainException.class)
            .extracting("baseCode")
            .isEqualTo(ChallengerErrorCode.INVALID_CHALLENGER_RECORD_CREATE_REQUEST);

        then(saveChallengerRecordPort).should(never()).save(any());
    }

    @Test
    @DisplayName("일반 기록 코드를 소비하면 챌린저를 생성하고 코드를 사용 처리한다")
    void 일반_기록_코드를_소비하면_챌린저를_생성하고_코드를_사용_처리한다() {
        ChallengerRecord record = normalRecord();
        given(loadChallengerRecordPort.getByCodeForUpdate("ABC123")).willReturn(record);
        given(getMemberUseCase.getById(100L)).willReturn(member("홍길동", 3L));
        given(loadChallengerPort.findByMemberIdAndGisuId(100L, 9L)).willReturn(Optional.empty());

        sut.consumeCode(consumeCommand());

        assertThat(record.isUsed()).isTrue();
        assertThat(record.getUsedMemberId()).isEqualTo(100L);
        then(saveChallengerPort).should().save(any(Challenger.class));
    }

    @Test
    @DisplayName("일반 기록 코드를 소비하면 대상 회원의 권한 snapshot 캐시를 제거한다")
    void 일반_기록_코드를_소비하면_대상_회원의_권한_snapshot_캐시를_제거한다() {
        ChallengerRecord record = normalRecord();
        given(loadChallengerRecordPort.getByCodeForUpdate("ABC123")).willReturn(record);
        given(getMemberUseCase.getById(100L)).willReturn(member("홍길동", 3L));
        given(loadChallengerPort.findByMemberIdAndGisuId(100L, 9L)).willReturn(Optional.empty());

        sut.consumeCode(consumeCommand());

        then(evictAuthoritySnapshotCacheUseCase).should().evictByMemberId(100L);
    }

    @Test
    @DisplayName("이미 사용된 코드는 소비할 수 없다")
    void 이미_사용된_코드는_소비할_수_없다() {
        ChallengerRecord record = normalRecord();
        record.markAsUsed(100L);
        given(loadChallengerRecordPort.getByCodeForUpdate("ABC123")).willReturn(record);

        assertThatThrownBy(() -> sut.consumeCode(consumeCommand()))
            .isInstanceOf(ChallengerDomainException.class)
            .extracting("baseCode")
            .isEqualTo(ChallengerErrorCode.USED_CHALLENGER_RECORD_CODE);

        then(getMemberUseCase).should(never()).getById(any());
        then(saveChallengerPort).should(never()).save(any());
    }

    @Test
    @DisplayName("일반 기록의 이름이 회원 정보와 다르면 챌린저를 생성하지 않는다")
    void 일반_기록의_이름이_회원_정보와_다르면_챌린저를_생성하지_않는다() {
        ChallengerRecord record = normalRecord();
        given(loadChallengerRecordPort.getByCodeForUpdate("ABC123")).willReturn(record);
        given(getMemberUseCase.getById(100L)).willReturn(member("김철수", 3L));

        assertThatThrownBy(() -> sut.consumeCode(consumeCommand()))
            .isInstanceOf(ChallengerDomainException.class)
            .extracting("baseCode")
            .isEqualTo(ChallengerErrorCode.INVALID_MEMBER_NAME_FOR_RECORD);

        assertThat(record.isUsed()).isFalse();
        then(saveChallengerPort).should(never()).save(any());
    }

    @Test
    @DisplayName("일반 기록의 학교가 회원 정보와 다르면 챌린저를 생성하지 않는다")
    void 일반_기록의_학교가_회원_정보와_다르면_챌린저를_생성하지_않는다() {
        ChallengerRecord record = normalRecord();
        given(loadChallengerRecordPort.getByCodeForUpdate("ABC123")).willReturn(record);
        given(getMemberUseCase.getById(100L)).willReturn(member("홍길동", 4L));

        assertThatThrownBy(() -> sut.consumeCode(consumeCommand()))
            .isInstanceOf(ChallengerDomainException.class)
            .extracting("baseCode")
            .isEqualTo(ChallengerErrorCode.INVALID_SCHOOL_FOR_RECORD);

        assertThat(record.isUsed()).isFalse();
        then(saveChallengerPort).should(never()).save(any());
    }

    @Test
    @DisplayName("같은 기수의 챌린저가 이미 있으면 일반 기록 코드를 소비하지 않는다")
    void 같은_기수의_챌린저가_이미_있으면_일반_기록_코드를_소비하지_않는다() {
        ChallengerRecord record = normalRecord();
        given(loadChallengerRecordPort.getByCodeForUpdate("ABC123")).willReturn(record);
        given(getMemberUseCase.getById(100L)).willReturn(member("홍길동", 3L));
        given(loadChallengerPort.findByMemberIdAndGisuId(100L, 9L))
            .willReturn(Optional.of(challenger(1L)));

        assertThatThrownBy(() -> sut.consumeCode(consumeCommand()))
            .isInstanceOf(ChallengerDomainException.class)
            .extracting("baseCode")
            .isEqualTo(ChallengerErrorCode.CHALLENGER_ALREADY_EXISTS);

        assertThat(record.isUsed()).isFalse();
        then(saveChallengerPort).should(never()).save(any());
    }

    @Test
    @DisplayName("운영진 기록 코드는 기존 챌린저에 역할을 부여하고 코드를 사용 처리한다")
    void 운영진_기록_코드는_기존_챌린저에_역할을_부여하고_코드를_사용_처리한다() {
        ChallengerRecord record = ChallengerRecord.createAdmin(
            1L, 9L, 2L, 3L, ChallengerPart.SPRINGBOOT, "홍길동",
            ChallengerRoleType.SCHOOL_PRESIDENT, 3L
        );
        given(loadChallengerRecordPort.getByCodeForUpdate("ABC123")).willReturn(record);
        given(loadChallengerPort.findByMemberIdAndGisuId(100L, 9L))
            .willReturn(Optional.of(challenger(50L)));
        given(getMemberUseCase.getById(100L)).willReturn(member("홍길동", 3L));

        sut.consumeCode(consumeCommand());

        assertThat(record.isUsed()).isTrue();
        then(manageChallengerRoleUseCase).should().createChallengerRole(any(CreateChallengerRoleCommand.class));
        then(saveChallengerPort).should(never()).save(any());
    }

    @Test
    @DisplayName("운영진 기록 코드 소비 시 해당 기수 챌린저가 없으면 실패한다")
    void 운영진_기록_코드_소비_시_해당_기수_챌린저가_없으면_실패한다() {
        ChallengerRecord record = ChallengerRecord.createAdmin(
            1L, 9L, 2L, 3L, ChallengerPart.SPRINGBOOT, "홍길동",
            ChallengerRoleType.SCHOOL_PRESIDENT, 3L
        );
        given(loadChallengerRecordPort.getByCodeForUpdate("ABC123")).willReturn(record);
        given(loadChallengerPort.findByMemberIdAndGisuId(100L, 9L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.consumeCode(consumeCommand()))
            .isInstanceOf(ChallengerDomainException.class)
            .extracting("baseCode")
            .isEqualTo(ChallengerErrorCode.NO_CHALLENGER_IN_MEMBER_GISU);

        assertThat(record.isUsed()).isFalse();
        then(manageChallengerRoleUseCase).should(never()).createChallengerRole(any());
    }

    private CreateChallengerRecordCommand recordCommand() {
        return CreateChallengerRecordCommand.builder()
            .creatorMemberId(1L)
            .gisuId(9L)
            .chapterId(2L)
            .schoolId(3L)
            .part(ChallengerPart.SPRINGBOOT)
            .memberName("홍길동")
            .build();
    }

    @Test
    @DisplayName("트랙 기수의 기본 트랙 코드를 파트 없이 발급한다")
    void 트랙_기수의_기본_트랙_코드를_파트_없이_발급한다() {
        // given
        givenTrackGisu();
        given(getChapterUseCase.byGisuAndSchool(9L, 3L)).willReturn(new ChapterInfo(2L, "서울"));
        given(saveChallengerRecordPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        sut.create(trackCommand(ChallengerTrack.WEB_PRODUCT_ENGINEER));

        // then
        ArgumentCaptor<ChallengerRecord> captor = ArgumentCaptor.forClass(ChallengerRecord.class);
        then(saveChallengerRecordPort).should().save(captor.capture());
        assertThat(captor.getValue().getTrack()).isEqualTo(ChallengerTrack.WEB_PRODUCT_ENGINEER);
        assertThat(captor.getValue().getPart()).isNull();
    }

    @Test
    @DisplayName("파트 기수에는 트랙 코드를 발급할 수 없다")
    void 파트_기수에는_트랙_코드를_발급할_수_없다() {
        assertThatThrownBy(() -> sut.create(trackCommand(ChallengerTrack.DESIGN)))
            .isInstanceOf(ChallengerDomainException.class)
            .extracting("baseCode")
            .isEqualTo(ChallengerErrorCode.INVALID_CHALLENGER_RECORD_CREATE_REQUEST);
        then(saveChallengerRecordPort).should(never()).save(any());
    }

    @Test
    @DisplayName("트랙 기수에는 기존 파트 코드를 발급할 수 없다")
    void 트랙_기수에는_기존_파트_코드를_발급할_수_없다() {
        givenTrackGisu();

        assertThatThrownBy(() -> sut.create(recordCommand()))
            .isInstanceOf(ChallengerDomainException.class)
            .extracting("baseCode")
            .isEqualTo(ChallengerErrorCode.INVALID_CHALLENGER_RECORD_CREATE_REQUEST);
        then(saveChallengerRecordPort).should(never()).save(any());
    }

    @Test
    @DisplayName("PLUS 트랙은 일반 가입코드로 발급할 수 없다")
    void PLUS_트랙은_일반_가입코드로_발급할_수_없다() {
        assertThatThrownBy(() -> sut.create(trackCommand(ChallengerTrack.INFRA_PLUS)))
            .isInstanceOf(ChallengerDomainException.class)
            .extracting("baseCode")
            .isEqualTo(ChallengerErrorCode.INVALID_CHALLENGER_RECORD_CREATE_REQUEST);
        then(saveChallengerRecordPort).should(never()).save(any());
    }

    @Test
    @DisplayName("일괄 발급도 학교와 지부 관계가 다르면 전체 저장하지 않는다")
    void 일괄_발급도_학교와_지부_관계가_다르면_전체_저장하지_않는다() {
        // given
        given(getChapterUseCase.byGisuAndSchool(9L, 3L))
            .willReturn(new ChapterInfo(2L, "서울"), new ChapterInfo(99L, "다른 지부"));

        // when & then
        assertThatThrownBy(() -> sut.createBulk(List.of(recordCommand(), recordCommand())))
            .isInstanceOf(ChallengerDomainException.class)
            .extracting("baseCode")
            .isEqualTo(ChallengerErrorCode.INVALID_CHALLENGER_RECORD_CREATE_REQUEST);
        then(saveChallengerRecordPort).should(never()).saveAll(any());
    }

    @Test
    @DisplayName("일괄 발급도 기수와 학습 유형이 다르면 전체 저장하지 않는다")
    void 일괄_발급도_기수와_학습_유형이_다르면_전체_저장하지_않는다() {
        givenTrackGisu();
        given(getChapterUseCase.byGisuAndSchool(9L, 3L)).willReturn(new ChapterInfo(2L, "서울"));

        assertThatThrownBy(() -> sut.createBulk(List.of(trackCommand(ChallengerTrack.PLAN), recordCommand())))
            .isInstanceOf(ChallengerDomainException.class);
        then(saveChallengerRecordPort).should(never()).saveAll(any());
    }

    @Test
    @DisplayName("기본 트랙 코드를 등록하면 하나의 트랙을 가진 챌린저를 생성한다")
    void 기본_트랙_코드를_등록하면_하나의_트랙을_가진_챌린저를_생성한다() {
        // given
        givenTrackGisu();
        ChallengerRecord record = trackCommand(ChallengerTrack.MOBILE_PRODUCT_ENGINEER).toEntity();
        given(loadChallengerRecordPort.getByCodeForUpdate("ABC123")).willReturn(record);
        given(getMemberUseCase.getById(100L)).willReturn(member("홍길동", 3L));
        given(loadChallengerPort.findByMemberIdAndGisuId(100L, 9L)).willReturn(Optional.empty());

        // when
        sut.consumeCode(consumeCommand());

        // then
        ArgumentCaptor<Challenger> captor = ArgumentCaptor.forClass(Challenger.class);
        then(saveChallengerPort).should().save(captor.capture());
        assertThat(captor.getValue().getPart()).isNull();
        assertThat(captor.getValue().getTracks()).containsExactly(ChallengerTrack.MOBILE_PRODUCT_ENGINEER);
        assertThat(record.isUsed()).isTrue();
        then(evictAuthoritySnapshotCacheUseCase).should().evictByMemberId(100L);
        then(manageChallengerRoleUseCase).should(never()).createChallengerRole(any());
    }

    @Test
    @DisplayName("운영진 코드에는 수강 트랙을 함께 발급할 수 없다")
    void 운영진_코드에는_수강_트랙을_함께_발급할_수_없다() {
        CreateChallengerRecordCommand command = CreateChallengerRecordCommand.builder()
            .creatorMemberId(1L).gisuId(9L).chapterId(2L).schoolId(3L)
            .part(ChallengerPart.WEB).track(ChallengerTrack.WEB_PRODUCT_ENGINEER)
            .memberName("홍길동").challengerRoleType(ChallengerRoleType.SCHOOL_PART_LEADER).build();

        assertThatThrownBy(() -> sut.create(command)).isInstanceOf(ChallengerDomainException.class);
        then(saveChallengerRecordPort).should(never()).save(any());
    }

    private void givenTrackGisu() {
        GisuInfo gisu = mock(GisuInfo.class);
        given(gisu.learningType()).willReturn(GisuLearningType.TRACK);
        given(getGisuUseCase.getById(9L)).willReturn(gisu);
    }

    private CreateChallengerRecordCommand trackCommand(ChallengerTrack track) {
        return CreateChallengerRecordCommand.builder()
            .creatorMemberId(1L).gisuId(9L).chapterId(2L).schoolId(3L)
            .track(track).memberName("홍길동").build();
    }

    private ConsumeChallengerRecordCommand consumeCommand() {
        return ConsumeChallengerRecordCommand.builder()
            .targetMemberId(100L)
            .code("ABC123")
            .build();
    }

    private ChallengerRecord normalRecord() {
        return ChallengerRecord.create(1L, 9L, 2L, 3L, ChallengerPart.SPRINGBOOT, "홍길동");
    }

    private MemberInfo member(String name, Long schoolId) {
        return MemberInfo.builder()
            .id(100L)
            .name(name)
            .nickname("길동")
            .schoolId(schoolId)
            .schoolName("테스트대학교")
            .build();
    }

    private Challenger challenger(Long id) {
        Challenger challenger = Challenger.builder()
            .memberId(100L)
            .part(ChallengerPart.SPRINGBOOT)
            .gisuId(9L)
            .build();
        ReflectionTestUtils.setField(challenger, "id", id);
        return challenger;
    }
}
