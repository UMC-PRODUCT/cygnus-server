package com.umc.product.challenger.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.challenger.domain.exception.ChallengerDomainException;
import com.umc.product.challenger.domain.exception.ChallengerErrorCode;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;

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
    @DisplayName("미사용 코드는 삭제 가능하다")
    void 미사용_코드는_삭제_가능하다() {
        ChallengerRecord record = ChallengerRecord.create(1L, 9L, 2L, 3L, ChallengerPart.WEB, "홍길동");

        // 예외 없이 정상 통과해야 한다
        record.validateDeletable();

        assertThat(record.isUsed()).isFalse();
    }

    @Test
    @DisplayName("사용된 코드는 삭제할 수 없다")
    void 사용된_코드는_삭제할_수_없다() {
        ChallengerRecord record = ChallengerRecord.create(1L, 9L, 2L, 3L, ChallengerPart.WEB, "홍길동");
        record.markAsUsed(100L);

        assertThatThrownBy(record::validateDeletable)
            .isInstanceOf(ChallengerDomainException.class)
            .extracting("baseCode")
            .isEqualTo(ChallengerErrorCode.CHALLENGER_RECORD_ALREADY_USED);
    }
}
