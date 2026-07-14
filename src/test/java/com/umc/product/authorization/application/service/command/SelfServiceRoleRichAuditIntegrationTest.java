package com.umc.product.authorization.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.umc.product.challenger.application.port.in.command.ManageChallengerRecordUseCase;
import com.umc.product.challenger.application.port.in.command.dto.ConsumeChallengerRecordCommand;
import com.umc.product.challenger.application.port.out.SaveChallengerRecordPort;
import com.umc.product.challenger.domain.ChallengerRecord;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.member.application.port.out.SaveMemberPort;
import com.umc.product.member.domain.Member;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.School;
import com.umc.product.support.fixture.ChallengerFixture;
import com.umc.product.support.fixture.ChapterFixture;
import com.umc.product.support.fixture.GisuFixture;
import com.umc.product.support.fixture.SchoolFixture;

@DisplayName("self-service 역할 rich audit PostgreSQL 통합 계약")
class SelfServiceRoleRichAuditIntegrationTest extends MemberRoleAuditDatabaseSupport {

    private static final String SENSITIVE_EMAIL = "self-service-sensitive@audit.test";

    @Autowired
    private ManageChallengerRecordUseCase manageChallengerRecordUseCase;

    @Autowired
    private SaveChallengerRecordPort saveChallengerRecordPort;

    @Autowired
    private SaveMemberPort saveMemberPort;

    @Autowired
    private GisuFixture gisuFixture;

    @Autowired
    private ChapterFixture chapterFixture;

    @Autowired
    private SchoolFixture schoolFixture;

    @Autowired
    private ChallengerFixture challengerFixture;

    @Test
    @DisplayName("인증 회원이 운영진 코드를 사용하면 CREATE audit에 본인 actor snapshot을 저장한다")
    void storesAuthenticatedMemberAsCreateAuditActor() throws Exception {
        // given
        Gisu gisu = gisuFixture.비활성_기수(9701L);
        Chapter chapter = chapterFixture.지부(gisu, "self-service 지부");
        School school = schoolFixture.지부에_소속된_학교("self-service 대학교", chapter);
        Member actor = saveMemberPort.save(Member.create(
            "self-service 사용자",
            "self-service 닉네임",
            SENSITIVE_EMAIL,
            school.getId(),
            null
        ));
        Member issuer = saveMemberPort.save(Member.create(
            "self-service 발급자",
            "self-service 발급닉",
            "self-service-issuer@audit.test",
            school.getId(),
            null
        ));
        challengerFixture.챌린저(actor.getId(), ChallengerPart.SPRINGBOOT, gisu.getId());
        ChallengerRecord record = saveChallengerRecordPort.save(ChallengerRecord.createAdmin(
            issuer.getId(),
            gisu.getId(),
            chapter.getId(),
            school.getId(),
            ChallengerPart.SPRINGBOOT,
            actor.getName(),
            ChallengerRoleType.SCHOOL_PRESIDENT,
            school.getId()
        ));

        // when
        manageChallengerRecordUseCase.consumeCode(ConsumeChallengerRecordCommand.builder()
            .targetMemberId(actor.getId())
            .code(record.getCode())
            .build());

        // then
        awaitExplicitRowCount("ChallengerRole", 1);
        AuditRow row = explicitRows("ChallengerRole").getFirst();
        String detailsJson = row.detailsJson();
        JsonNode details = objectMapper.readTree(detailsJson);
        assertThat(row.action()).isEqualTo("CREATE");
        assertThat(row.actorMemberId()).isEqualTo(actor.getId());
        assertThat(details.path("actor").path("memberId").asLong()).isEqualTo(actor.getId());
        assertThat(details.path("actor").path("name").asText()).isEqualTo(actor.getName());
        assertThat(details.path("actor").path("nickname").asText()).isEqualTo(actor.getNickname());
        assertThat(details.path("actor").path("schoolName").asText()).isEqualTo(school.getName());
        assertThat(details.path("target").path("memberId").asLong()).isEqualTo(actor.getId());
        assertThat(details.path("target").path("roleName").asText()).isEqualTo("SCHOOL_PRESIDENT");
        assertThat(detailsJson)
            .doesNotContain(SENSITIVE_EMAIL, "email", "providerId", "password", "token");
    }
}
