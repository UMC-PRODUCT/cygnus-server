package com.umc.product.authorization.application.service.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.analytics.application.authorization.AnalyticsPolicyBundleContributor;
import com.umc.product.audit.application.authorization.AuditPolicyBundleContributor;
import com.umc.product.authorization.application.authorization.AuthorizationPolicyBundleContributor;
import com.umc.product.authorization.application.port.out.policy.PolicyBundleContributor;
import com.umc.product.blog.application.authorization.BlogPolicyBundleContributor;
import com.umc.product.certificate.application.authorization.CertificatePolicyBundleContributor;
import com.umc.product.challenger.application.authorization.ChallengerPolicyBundleContributor;
import com.umc.product.chat.application.authorization.ChatPolicyBundleContributor;
import com.umc.product.community.application.authorization.CommunityPolicyBundleContributor;
import com.umc.product.curriculum.application.authorization.CurriculumPolicyBundleContributor;
import com.umc.product.feedback.application.authorization.FeedbackPolicyBundleContributor;
import com.umc.product.form.application.authorization.FormPolicyBundleContributor;
import com.umc.product.maintenance.application.authorization.MaintenancePolicyBundleContributor;
import com.umc.product.member.application.authorization.MemberPolicyBundleContributor;
import com.umc.product.notice.application.authorization.NoticePolicyBundleContributor;
import com.umc.product.notification.application.authorization.NotificationPolicyBundleContributor;
import com.umc.product.organization.application.authorization.OrganizationPolicyBundleContributor;
import com.umc.product.project.application.authorization.ProjectPolicyBundleContributor;
import com.umc.product.recruiting.application.authorization.RecruitingPolicyBundleContributor;
import com.umc.product.schedule.application.authorization.SchedulePolicyBundleContributor;
import com.umc.product.storage.application.authorization.StoragePolicyBundleContributor;
import com.umc.product.term.application.authorization.TermPolicyBundleContributor;

class CommonPolicyArtifactTest {

    @Test
    @DisplayName("공용 rollout namespace의 generated review artifact가 compiled policy와 일치한다")
    void verifiesCommonRolloutArtifacts() throws Exception {
        List<PolicyBundleContributor> contributors = List.of(
            new AnalyticsPolicyBundleContributor(),
            new AuditPolicyBundleContributor(),
            new AuthorizationPolicyBundleContributor(),
            new BlogPolicyBundleContributor(),
            new CertificatePolicyBundleContributor(),
            new ChallengerPolicyBundleContributor(),
            new ChatPolicyBundleContributor(),
            new CommunityPolicyBundleContributor(),
            new CurriculumPolicyBundleContributor(),
            new FeedbackPolicyBundleContributor(),
            new FormPolicyBundleContributor(),
            new MaintenancePolicyBundleContributor(),
            new MemberPolicyBundleContributor(),
            new NotificationPolicyBundleContributor(),
            new NoticePolicyBundleContributor(),
            new OrganizationPolicyBundleContributor(),
            new ProjectPolicyBundleContributor(),
            new RecruitingPolicyBundleContributor(),
            new SchedulePolicyBundleContributor(),
            new StoragePolicyBundleContributor(),
            new TermPolicyBundleContributor());
        CompiledPolicyRegistry registry =
            new CompiledPolicyRegistry(new PolicySemanticCompiler(), contributors);
        String printNamespace = System.getenv("POLICY_ARTIFACT_PRINT");

        for (String namespace : registry.commonRolloutNamespaces()) {
            String artifact = PolicyReviewArtifactRenderer.render(
                registry.require(namespace),
                registry.surfaces());
            if (namespace.equals(printNamespace)) {
                System.out.print(artifact);
                continue;
            }
            Path tracked = Path.of(
                "src/main/resources/policies",
                namespace,
                "generated",
                namespace + "-policy-artifacts.md");
            assertThat(Files.readString(tracked, StandardCharsets.UTF_8))
                .as(namespace + " policy artifact")
                .isEqualTo(artifact);
        }
    }
}
