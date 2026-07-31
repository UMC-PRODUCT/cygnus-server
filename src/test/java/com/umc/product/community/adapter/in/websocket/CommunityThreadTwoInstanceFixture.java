package com.umc.product.community.adapter.in.websocket;

import java.util.List;

import org.springframework.context.ApplicationContext;

import com.umc.product.challenger.application.port.out.SaveChallengerPort;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.community.application.port.in.command.thread.CreateCommunityThreadUseCase;
import com.umc.product.community.application.port.in.command.thread.dto.CommunityThreadLifecycleInfo;
import com.umc.product.community.application.port.in.command.thread.dto.CreateCommunityThreadCommand;
import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.member.application.port.out.SaveMemberPort;
import com.umc.product.member.domain.Member;
import com.umc.product.organization.application.port.out.command.SaveChapterPort;
import com.umc.product.organization.application.port.out.command.SaveSchoolPort;
import com.umc.product.organization.application.port.out.query.LoadGisuPort;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.School;

final class CommunityThreadTwoInstanceFixture {

    private CommunityThreadTwoInstanceFixture() {
    }

    static Participants seedParticipants(ApplicationContext context) {
        Gisu gisu = context.getBean(LoadGisuPort.class).getActiveGisu();
        Chapter chapter = context.getBean(SaveChapterPort.class).save(
            Chapter.create(gisu, "community-relay-e2e")
        );
        School school = School.create("community-relay-e2e-school", null, null);
        school.assignToChapter(chapter);
        school = context.getBean(SaveSchoolPort.class).save(school);

        SaveMemberPort saveMemberPort = context.getBean(SaveMemberPort.class);
        Member owner = saveMemberPort.save(member("owner", school.getId()));
        Member member = saveMemberPort.save(member("member", school.getId()));
        Member attacker = saveMemberPort.save(member("attacker", school.getId()));

        SaveChallengerPort saveChallengerPort = context.getBean(SaveChallengerPort.class);
        saveChallengerPort.save(new Challenger(owner.getId(), ChallengerPart.SPRINGBOOT, gisu.getId()));
        saveChallengerPort.save(new Challenger(member.getId(), ChallengerPart.WEB, gisu.getId()));
        saveChallengerPort.save(new Challenger(attacker.getId(), ChallengerPart.IOS, gisu.getId()));

        JwtTokenProvider tokenProvider = context.getBean(JwtTokenProvider.class);
        return new Participants(
            actor(owner, tokenProvider),
            actor(member, tokenProvider),
            actor(attacker, tokenProvider)
        );
    }

    static Scenario createThread(ApplicationContext context, Participants participants, int sequence) {
        CommunityThreadLifecycleInfo thread = context.getBean(CreateCommunityThreadUseCase.class).create(
            new CreateCommunityThreadCommand(
                participants.owner().memberId(),
                "두 인스턴스 relay 검증 " + sequence,
                "공유 DB와 외부 STOMP broker E2E",
                CommunityThreadCategory.PROJECT,
                "💬",
                List.of(participants.member().memberId())
            )
        );
        return new Scenario(
            thread.threadId(),
            participants.owner(),
            participants.member(),
            participants.attacker()
        );
    }

    private static Member member(String key, Long schoolId) {
        return Member.create(
            "relay-" + key,
            "relay-" + key,
            "relay-" + key + "@test.umc.local",
            schoolId,
            null
        );
    }

    private static Actor actor(Member member, JwtTokenProvider tokenProvider) {
        return new Actor(member.getId(), tokenProvider.createAccessToken(member.getId(), List.of()));
    }

    record Scenario(Long threadId, Actor owner, Actor member, Actor attacker) {
    }

    record Participants(Actor owner, Actor member, Actor attacker) {
    }

    record Actor(Long memberId, String accessToken) {
    }
}
