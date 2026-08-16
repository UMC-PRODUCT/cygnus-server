package com.umc.product.organization.application.port.in.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.member.domain.Member;
import com.umc.product.organization.application.port.in.command.dto.UpdateStudyGroupCommand;
import com.umc.product.organization.application.port.out.query.LoadStudyGroupPort;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.StudyGroup;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;
import com.umc.product.support.UseCaseTestSupport;
import com.umc.product.support.fixture.ChallengerFixture;
import com.umc.product.support.fixture.GisuFixture;
import com.umc.product.support.fixture.MemberFixture;
import com.umc.product.support.fixture.StudyGroupFixture;

class ManageStudyGroupUseCaseTest extends UseCaseTestSupport {

    @Autowired
    private ManageStudyGroupUseCase manageStudyGroupUseCase;

    @Autowired
    private GisuFixture gisuFixture;

    @Autowired
    private MemberFixture memberFixture;

    @Autowired
    private ChallengerFixture challengerFixture;

    @Autowired
    private StudyGroupFixture studyGroupFixture;

    @Autowired
    private LoadStudyGroupPort loadStudyGroupPort;

    @Test
    void 파트를_수정하면_그룹의_파트가_변경된다() {
        // given
        Gisu gisu = gisuFixture.비활성_기수();
        Member member = memberFixture.일반();
        StudyGroup group = studyGroupFixture.스터디그룹(
            "web-study", gisu, ChallengerPart.WEB, null, Set.of(member.getId())
        );

        // when
        manageStudyGroupUseCase.update(
            new UpdateStudyGroupCommand(group.getId(), null, ChallengerPart.SPRINGBOOT)
        );

        // then
        assertThat(loadStudyGroupPort.getEntityById(group.getId()).getPart())
            .isEqualTo(ChallengerPart.SPRINGBOOT);
    }

    @Test
    void 파트_수정시_변경할_파트의_다른_스터디에_이미_속한_멤버가_있으면_예외가_발생한다() {
        // given
        Gisu gisu = gisuFixture.비활성_기수();
        Member member = memberFixture.일반();
        studyGroupFixture.스터디그룹(
            "web-study", gisu, ChallengerPart.WEB, null, Set.of(member.getId())
        );
        StudyGroup planGroup = studyGroupFixture.스터디그룹(
            "plan-study", gisu, ChallengerPart.PLAN, null, Set.of(member.getId())
        );

        // when & then
        assertThatThrownBy(() -> manageStudyGroupUseCase.update(
            new UpdateStudyGroupCommand(planGroup.getId(), null, ChallengerPart.WEB)
        ))
            .isInstanceOf(OrganizationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.STUDY_GROUP_MEMBER_ALREADY_IN_PART_STUDY);
    }
}
