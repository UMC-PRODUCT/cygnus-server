package com.umc.product.member.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Member 초대 검색 아키텍처")
class MemberInvitationArchitectureTest {

    private static final Path SERVICE_SOURCE = Path.of(
        "src/main/java/com/umc/product/member/application/service/MemberInvitationQueryService.java"
    );
    private static final Path MEMBER_ADAPTER_SOURCE = Path.of(
        "src/main/java/com/umc/product/member/adapter/out/persistence/MemberPersistenceAdapter.java"
    );
    private static final Path REMOVED_REPOSITORY_SOURCE = Path.of(
        "src/main/java/com/umc/product/member/adapter/out/persistence/ActiveChallengerInvitationQueryRepository.java"
    );
    private static final Path REMOVED_PORT_SOURCE = Path.of(
        "src/main/java/com/umc/product/member/application/port/out/SearchActiveChallengerInvitationPort.java"
    );

    @Test
    @DisplayName("초대 검색은 Member 출력 Port와 외부 도메인 Query UseCase 경계를 지킨다")
    void 초대_검색은_공개_query_usecase_경계를_지킨다() throws IOException {
        String serviceSource = Files.readString(SERVICE_SOURCE);
        String memberAdapterSource = Files.readString(MEMBER_ADAPTER_SOURCE);

        List<String> forbiddenImports = serviceSource.lines()
            .filter(line -> line.trim().startsWith("import "))
            .filter(line -> line.contains("com.umc.product.challenger.domain.")
                || line.contains("com.umc.product.organization.domain.")
                || line.contains(".adapter.out.persistence."))
            .toList();

        assertThat(serviceSource)
            .contains(
                "GetGisuUseCase",
                "GetChallengerUseCase",
                "SearchMemberInvitationPort",
                "ChallengerBasicInfo",
                "getAllBasicByMemberIds"
            )
            .doesNotContain(
                "SearchActiveChallengerInvitationPort",
                "getAllByGisuIdWithoutChallengerPoints",
                "findActiveGisu",
                "getActiveGisu",
                "listBasicByGisuId",
                "QChallenger",
                "com.umc.product.challenger.domain.",
                "com.umc.product.organization.domain.",
                ".adapter.out.persistence."
            );
        assertThat(forbiddenImports).isEmpty();
        assertThat(memberAdapterSource)
            .contains("SearchMemberInvitationPort")
            .doesNotContain("ActiveChallengerInvitation", "SearchActiveChallengerInvitationPort");
        assertThat(Files.exists(REMOVED_REPOSITORY_SOURCE)).isFalse();
        assertThat(Files.exists(REMOVED_PORT_SOURCE)).isFalse();
    }
}
