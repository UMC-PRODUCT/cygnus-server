package com.umc.product.test.adapter.in.runner;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
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
 * seeder 프로파일 전용 벌크 시딩 러너.
 * <p>
 * 앱 이미지를 다음처럼 1회 실행한다:
 * <pre>
 * docker run --rm --env-file &lt;SUT env&gt; \
 *   -e SPRING_PROFILES_ACTIVE=&lt;base&gt;,seeder \
 *   -e APP_BULK_SEED_MEMBER_COUNT=100000 -e APP_BULK_SEED_SEED_JSON_PATH=/seed-out/seed.json \
 *   &lt;app_image&gt;
 * </pre>
 * 웹서버는 끄지 않는다 — SecurityConfig 가 MVC 빈(RequestMappingHandlerMapping)을 생성자로 요구해
 * web-application-type=none 으로는 컨텍스트가 뜨지 않는다. 컨테이너가 포트를 publish 하지 않으므로
 * SUT 앱과 충돌은 없고, 시딩 후 k6 계약의 seed.json 을 쓴 뒤 러너가 스스로 프로세스를 종료한다.
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
    private final ConfigurableApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 컨텍스트 완성(DataSource·Flyway·JPA 초기화) 직후 1회 호출된다.
        // 웹서버가 없으므로(web-application-type=none) 이 메서드가 리턴하면 JVM 이 자연 종료된다
        // — "부팅 → 시딩 → 종료"의 일회성 배치가 별도 스크립트 없이 완성되는 이유.
        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            // @Profile("seeder") 만으로도 평소엔 빈이 안 뜨지만, 실수로 prod 와 같이 켜는 경우까지 차단한다.
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
        // 웹서버가 떠 있어 JVM 이 스스로 안 죽는다 — 시딩 성공 시 여기서 명시적으로 종료한다.
        // (실패 시에는 예외가 부팅을 중단시켜 exit 1 로 끝난다)
        System.exit(SpringApplication.exit(applicationContext, () -> 0));
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
