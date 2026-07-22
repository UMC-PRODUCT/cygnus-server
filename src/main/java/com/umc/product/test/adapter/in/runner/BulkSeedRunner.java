package com.umc.product.test.adapter.in.runner;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.test.application.port.in.command.SeedBulkDataUseCase;
import com.umc.product.test.application.port.in.command.dto.SeedBulkDataCommand;
import com.umc.product.test.application.port.in.command.dto.SeedBulkDataResult;
import com.umc.product.test.application.service.BulkSeedProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * seeder 프로파일 전용 벌크 시딩 러너 (SEED_STRATEGY=bulk).
 * <p>
 * prepare-data.sh 가 앱 이미지를 다음처럼 1회 실행한다:
 * <pre>
 * docker run --rm --env-file &lt;SUT env&gt; \
 *   -e SPRING_PROFILES_ACTIVE=&lt;base&gt;,seeder \
 *   &lt;app_image&gt; \
 *   --spring.main.web-application-type=none \
 *   --app.bulk-seed.member-count=100000 --app.bulk-seed.seed-json-path=/seed-out/seed.json
 * </pre>
 * 시딩 후 k6 계약(prepare-data.sh 산출물과 동일 스키마)의 seed.json 을 쓰고 프로세스가 종료된다.
 */
@Slf4j
@Component
@Profile("seeder")
@RequiredArgsConstructor
public class BulkSeedRunner implements ApplicationRunner {

    private final SeedBulkDataUseCase seedBulkDataUseCase;
    private final BulkSeedProperties properties;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            throw new IllegalStateException("seeder 프로파일은 prod 와 함께 활성화할 수 없습니다");
        }

        long startedAt = System.currentTimeMillis();
        SeedBulkDataResult result = seedBulkDataUseCase.seed(new SeedBulkDataCommand(
            properties.memberCount(),
            properties.pointsPerChallenger(),
            properties.schedulesPerMember(),
            properties.noticeGlobalCount(),
            properties.randomSeed(),
            properties.sampleMemberIdCount()
        ));

        writeSeedJson(result);
        log.info(
            "bulk seed done in {}ms: members={}, sampledIds={}, seedJson={}",
            System.currentTimeMillis() - startedAt,
            result.memberCount(), result.memberIds().size(), properties.seedJsonPath()
        );
    }

    /** prepare-data.sh(api 전략) 산출물과 동일한 스키마로 쓴다 — k6 lib/data.js 계약. */
    private void writeSeedJson(SeedBulkDataResult result) throws Exception {
        File out = new File(properties.seedJsonPath());
        File parent = out.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("seed.json 출력 디렉터리 생성 실패: " + parent);
        }
        objectMapper.writerWithDefaultPrettyPrinter()
            .writeValue(out, new SeedJsonPayload(
                String.valueOf(result.gisuId()), "", "", result.memberIds(), List.of()));
    }

    private record SeedJsonPayload(
        String gisuId,
        String chapterId,
        String matchingRoundId,
        List<Long> memberIds,
        List<Object> targets
    ) {
    }
}
