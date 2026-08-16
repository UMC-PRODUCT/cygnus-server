package com.umc.product.challenger.application.port.in;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.challenger.adapter.in.web.dto.request.CreateChallengerInfoRequest;
import com.umc.product.challenger.application.port.in.command.dto.CreateChallengerCommand;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerBasicInfo;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;

@DisplayName("Challenger 트랙 DTO")
class ChallengerTrackDtoTest {

    @Test
    @DisplayName("생성 요청은 여러 트랙을 생성 Command에 노출한다")
    void 생성_요청은_여러_트랙을_생성_Command에_노출한다() {
        CreateChallengerInfoRequest request = new CreateChallengerInfoRequest(
            1L,
            null,
            List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER, ChallengerTrack.MOBILE_PRODUCT_ENGINEER),
            9L
        );

        CreateChallengerCommand command = request.toCommand();

        assertThat(command.tracks()).containsExactly(
            ChallengerTrack.WEB_PRODUCT_ENGINEER,
            ChallengerTrack.MOBILE_PRODUCT_ENGINEER
        );
    }

    @Test
    @DisplayName("조회 Info는 여러 유효 트랙을 노출한다")
    void 조회_Info는_여러_유효_트랙을_노출한다() {
        Challenger challenger = Challenger.builder()
            .memberId(1L)
            .tracks(List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER, ChallengerTrack.MOBILE_PRODUCT_ENGINEER))
            .gisuId(9L)
            .build();

        ChallengerInfo info = ChallengerInfo.from(challenger, List.of());
        ChallengerBasicInfo basicInfo = ChallengerBasicInfo.from(challenger);

        assertThat(info.tracks()).containsExactly(
            ChallengerTrack.WEB_PRODUCT_ENGINEER,
            ChallengerTrack.MOBILE_PRODUCT_ENGINEER
        );
        assertThat(basicInfo.tracks()).isEqualTo(info.tracks());
    }

    @Test
    @DisplayName("조회 Info는 기존 파트를 단일 유효 트랙으로 노출한다")
    void 조회_Info는_기존_파트를_단일_유효_트랙으로_노출한다() {
        Challenger challenger = Challenger.builder()
            .memberId(1L)
            .part(ChallengerPart.SPRINGBOOT)
            .gisuId(9L)
            .build();

        ChallengerInfo info = ChallengerInfo.from(challenger, List.of());

        assertThat(info.tracks()).containsExactly(ChallengerTrack.WEB_PRODUCT_ENGINEER);
    }
}
