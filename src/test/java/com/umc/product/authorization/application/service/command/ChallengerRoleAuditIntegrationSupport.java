package com.umc.product.authorization.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.umc.product.authorization.application.port.in.command.ManageChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.command.dto.CreateChallengerRoleCommand;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.member.application.port.out.SaveMemberPort;
import com.umc.product.member.domain.Member;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.School;
import com.umc.product.support.fixture.ChallengerFixture;
import com.umc.product.support.fixture.GisuFixture;
import com.umc.product.support.fixture.SchoolFixture;

abstract class ChallengerRoleAuditIntegrationSupport extends MemberRoleAuditDatabaseSupport {

    @Autowired
    protected ManageChallengerRoleUseCase manageChallengerRoleUseCase;

    @Autowired
    private SaveMemberPort saveMemberPort;

    @Autowired
    private SchoolFixture schoolFixture;

    @Autowired
    private GisuFixture gisuFixture;

    @Autowired
    private ChallengerFixture challengerFixture;

    protected RoleContext roleContext(String prefix) {
        School school = schoolFixture.학교(prefix + "대학교");
        Gisu gisu = gisuFixture.비활성_기수((long) Math.abs(prefix.hashCode()));
        Member actor = member(prefix + "관리자", prefix + "관리닉", school.getId());
        Member target = member(prefix + "대상자", prefix + "대상닉", school.getId());
        Challenger challenger = challengerFixture.챌린저(
            target.getId(),
            ChallengerPart.WEB,
            gisu.getId()
        );
        return new RoleContext(school, gisu, actor, target, challenger);
    }

    protected CreateChallengerRoleCommand createRoleCommand(
        RoleContext context,
        ChallengerRoleType roleType
    ) {
        return CreateChallengerRoleCommand.builder()
            .challengerId(context.challenger().getId())
            .roleType(roleType)
            .organizationId(context.school().getId())
            .gisuId(context.gisu().getId())
            .actorMemberId(context.actor().getId())
            .build();
    }

    protected void assertRoleSnapshot(
        AuditRow row,
        RoleContext context,
        RoleExpectation expectation
    ) throws JsonProcessingException {
        JsonNode details = details(row);
        assertThat(row.actorMemberId()).isEqualTo(context.actor().getId());
        assertThat(details.path("schemaVersion").asInt()).isOne();
        assertThat(details.path("actor").path("memberId").asLong()).isEqualTo(context.actor().getId());
        assertThat(details.path("actor").path("name").asText()).isEqualTo(context.actor().getName());
        assertThat(details.path("target").path("id").asLong()).isEqualTo(expectation.roleId());
        assertThat(details.path("target").path("memberId").asLong()).isEqualTo(context.target().getId());
        assertThat(details.path("target").path("name").asText()).isEqualTo(context.target().getName());
        assertThat(details.path("target").path("nickname").asText())
            .isEqualTo(context.target().getNickname());
        assertThat(details.path("target").path("schoolName").asText()).isEqualTo(context.school().getName());
        assertThat(details.path("target").path("status").asText()).isEqualTo("ACTIVE");
        assertThat(details.path("target").path("roleName").asText()).isEqualTo(expectation.roleName());
        assertThat(row.detailsJson()).doesNotContain(
            context.actor().getEmail(),
            context.target().getEmail(),
            "email",
            "providerId",
            "oauthSubject",
            "password",
            "token"
        );
    }

    private Member member(String name, String nickname, Long schoolId) {
        return saveMemberPort.save(Member.create(
            name,
            nickname,
            name + "-" + nickname + "@role-audit.test",
            schoolId,
            null
        ));
    }

    protected record RoleContext(
        School school,
        Gisu gisu,
        Member actor,
        Member target,
        Challenger challenger
    ) {
    }

    protected record RoleExpectation(Long roleId, String roleName) {
    }
}
