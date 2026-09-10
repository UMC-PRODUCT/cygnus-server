package com.umc.product.challenger.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.challenger.domain.exception.ChallengerDomainException;
import com.umc.product.challenger.domain.exception.ChallengerErrorCode;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.common.domain.enums.GisuLearningType;

@DisplayName("ChallengerRecord 도메인")
class ChallengerRecordTest {

    @Test
    @DisplayName("일반 챌린저 기록 코드를 생성한다")
    void 일반_챌린저_기록_코드를_생성한다() {
        ChallengerRecord record = ChallengerRecord.create(1L, 9L, 2L, 3L, ChallengerPart.WEB, "홍길동");

        assertThat(record.getCode()).hasSize(6);
        assertThat(record.isUsed()).isFalse();
        assertThat(record.isAdminRecord()).isFalse();
        assertThat(record.getMemberName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("운영진 기록은 역할 타입과 조직 ID를 가진다")
    void 운영진_기록은_역할_타입과_조직_ID를_가진다() {
        ChallengerRecord record = ChallengerRecord.createAdmin(
            1L, 9L, 2L, 3L, ChallengerPart.PLAN, "홍길동",
            ChallengerRoleType.SCHOOL_PRESIDENT, 3L
        );

        assertThat(record.isAdminRecord()).isTrue();
        assertThat(record.getChallengerRoleType()).isEqualTo(ChallengerRoleType.SCHOOL_PRESIDENT);
        assertThat(record.getOrganizationId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("사용 처리 시 사용 회원과 시각을 기록한다")
    void 사용_처리_시_사용_회원과_시각을_기록한다() {
        ChallengerRecord record = ChallengerRecord.create(1L, 9L, 2L, 3L, ChallengerPart.WEB, "홍길동");

        record.markAsUsed(100L);

        assertThat(record.isUsed()).isTrue();
        assertThat(record.getUsedMemberId()).isEqualTo(100L);
        assertThat(record.getUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 사용된 코드는 다시 사용할 수 없다")
    void 이미_사용된_코드는_다시_사용할_수_없다() {
        ChallengerRecord record = ChallengerRecord.create(1L, 9L, 2L, 3L, ChallengerPart.WEB, "홍길동");
        record.markAsUsed(100L);

        assertThatThrownBy(() -> record.markAsUsed(101L))
            .isInstanceOf(ChallengerDomainException.class)
            .extracting("baseCode")
            .isEqualTo(ChallengerErrorCode.USED_CHALLENGER_RECORD_CODE);
    }

    @Test
    @DisplayName("기록의 회원 이름과 학교가 요청자 정보와 일치해야 한다")
    void 기록의_회원_이름과_학교가_요청자_정보와_일치해야_한다() {
        ChallengerRecord record = ChallengerRecord.create(1L, 9L, 2L, 3L, ChallengerPart.WEB, "홍길동");

        record.validateMember("홍길동", 3L);

        assertThatThrownBy(() -> record.validateMember("김철수", 3L))
            .isInstanceOf(ChallengerDomainException.class)
            .extracting("baseCode")
            .isEqualTo(ChallengerErrorCode.INVALID_MEMBER_NAME_FOR_RECORD);

        assertThatThrownBy(() -> record.validateMember("홍길동", 4L))
            .isInstanceOf(ChallengerDomainException.class)
            .extracting("baseCode")
            .isEqualTo(ChallengerErrorCode.INVALID_SCHOOL_FOR_RECORD);
    }

    @Test
    @DisplayName("복수 수강 트랙과 담당 파트 역할을 한 코드에 보존하고 PART 기수에서는 거부한다")
    void 복수_트랙과_역할을_보존하고_PART_기수에서는_거부한다() {
        // Given
        ChallengerRecord record = ChallengerRecord.createAdminWithTracks(
            1L, 11L, 2L, 3L, ChallengerPart.SPRINGBOOT,
            List.of(ChallengerTrack.DESIGN, ChallengerTrack.WEB_PRODUCT_ENGINEER), "홍길동",
            ChallengerRoleType.SCHOOL_PART_LEADER, 3L);

        // When
        record.validateLearningType(GisuLearningType.TRACK);

        // Then
        assertThat(record.getPart()).isEqualTo(ChallengerPart.SPRINGBOOT);
        assertThat(record.getTracks()).containsExactly(ChallengerTrack.DESIGN, ChallengerTrack.WEB_PRODUCT_ENGINEER);
        assertThat(record.getTrack()).isNull();
        assertThatThrownBy(() -> record.validateLearningType(GisuLearningType.PART))
            .isInstanceOf(ChallengerDomainException.class);
    }

    @Test
    @DisplayName("기존 운영진의 담당 파트는 수강으로 변환하지 않고 비수강 중앙 운영진은 지부를 생략한다")
    void 담당_파트는_수강이_아니며_비수강_중앙_운영진은_지부를_생략한다() {
        // Given / When
        ChallengerRecord record = ChallengerRecord.createAdmin(
            1L, 11L, null, 3L, ChallengerPart.DESIGN, "홍길동",
            ChallengerRoleType.CENTRAL_EDUCATION_TEAM_MEMBER, null);
        record.validateLearningType(GisuLearningType.TRACK);
        record.validateLearningType(GisuLearningType.PART);

        // Then
        assertThat(record.getTracks()).isEmpty();
        assertThat(record.canOmitChapter()).isTrue();
        assertThat(record.getPart()).isEqualTo(ChallengerPart.DESIGN);
    }

    @Test
    @DisplayName("수강 중인 중앙 운영진과 학교 운영진은 지부를 생략할 수 없다")
    void 수강_중앙_운영진과_학교_운영진은_지부를_생략할_수_없다() {
        // Given / When / Then
        assertThatThrownBy(() -> ChallengerRecord.createAdminWithTracks(
            1L, 11L, null, 3L, null, List.of(ChallengerTrack.PLAN), "홍길동",
            ChallengerRoleType.CENTRAL_PRESIDENT, null)).isInstanceOf(ChallengerDomainException.class);
        assertThatThrownBy(() -> ChallengerRecord.createAdminWithTracks(
            1L, 11L, null, 3L, null, List.of(), "홍길동",
            ChallengerRoleType.SCHOOL_PRESIDENT, 3L)).isInstanceOf(ChallengerDomainException.class);
    }

    @Test
    @DisplayName("TRACK 기수에서 기존 ADMIN 비수강 코드는 허용하고 역할 없는 빈 코드와 일반 학습 파트는 거부한다")
    void TRACK_기수의_ADMIN은_허용하고_역할없는_빈코드와_학습_파트는_거부한다() {
        // Given
        ChallengerRecord staff = ChallengerRecord.createWithTracks(1L, 11L, 2L, 3L, null, List.of(), "홍길동");
        ChallengerRecord legacyStaff = ChallengerRecord.create(1L, 11L, 2L, 3L, ChallengerPart.ADMIN, "홍길동");
        ChallengerRecord learner = ChallengerRecord.create(1L, 11L, 2L, 3L, ChallengerPart.WEB, "홍길동");

        // When
        legacyStaff.validateLearningType(GisuLearningType.TRACK);

        // Then
        assertThat(staff.getTracks()).isEmpty();
        assertThat(legacyStaff.getTracks()).isEmpty();
        assertThatThrownBy(() -> staff.validateLearningType(GisuLearningType.TRACK))
            .isInstanceOf(ChallengerDomainException.class);
        assertThatThrownBy(() -> staff.validateLearningType(GisuLearningType.PART))
            .isInstanceOf(ChallengerDomainException.class);
        assertThatThrownBy(() -> learner.validateLearningType(GisuLearningType.TRACK))
            .isInstanceOf(ChallengerDomainException.class);
    }

    @Test
    @DisplayName("PLUS 트랙은 운영진 역할이 있어도 발급할 수 없다")
    void PLUS_트랙은_운영진_역할이_있어도_발급할_수_없다() {
        // Given / When / Then
        assertThatThrownBy(() -> ChallengerRecord.createAdminWithTracks(
            1L, 11L, 2L, 3L, null, List.of(ChallengerTrack.INFRA_PLUS), "홍길동",
            ChallengerRoleType.SCHOOL_PRESIDENT, 3L)).isInstanceOf(ChallengerDomainException.class);
    }
}
