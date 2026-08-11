package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.graphql.execution.GraphQlSource;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.instrumentation.Instrumentation;

class GraphQlExecutionConfigTest {

    @Test
    @DisplayName("members는 기본 first 20 비용을 포함한 설정 한도에서 실행된다")
    void members는_기본_first_20_비용을_포함한_설정_한도에서_실행된다() throws IOException {
        AtomicInteger dataFetcherInvocations = new AtomicInteger();
        ExecutionResult result = actualGraphQl(dataFetcherInvocations, 22).execute("""
            query {
              members(filter: { keyword: "kim" }) { totalCount }
            }
            """);

        assertThat(result.getErrors()).isEmpty();
        Map<String, Object> data = result.getData();
        assertThat(data).isEqualTo(Map.of("members", Map.of("totalCount", 0L)));
        assertThat(dataFetcherInvocations.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("members alias 두 개와 큰 first는 data fetcher 전에 거부된다")
    void members_alias_두_개와_큰_first는_data_fetcher_전에_거부된다() throws IOException {
        AtomicInteger dataFetcherInvocations = new AtomicInteger();
        ExecutionResult result = actualGraphQl(dataFetcherInvocations, 203).execute("""
            query {
              firstResult: members(filter: { keyword: "kim" }, first: 100) { totalCount }
              secondResult: members(filter: { keyword: "lee" }, first: 100) { totalCount }
            }
            """);

        assertRejected(result);
        assertThat(dataFetcherInvocations.get()).isZero();
    }

    @Test
    @DisplayName("first 1은 정확한 복잡도 한도에서 실행된다")
    void first_1은_정확한_복잡도_한도에서_실행된다() throws IOException {
        AtomicInteger dataFetcherInvocations = new AtomicInteger();
        ExecutionResult result = actualGraphQl(dataFetcherInvocations, 3).execute("""
            query {
              members(first: 1) { totalCount }
            }
            """);

        assertThat(result.getErrors()).isEmpty();
        assertThat(dataFetcherInvocations.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("last 100은 first 1의 정확한 복잡도 한도를 초과한다")
    void last_100은_first_1의_정확한_복잡도_한도를_초과한다() throws IOException {
        AtomicInteger dataFetcherInvocations = new AtomicInteger();
        ExecutionResult result = actualGraphQl(dataFetcherInvocations, 3).execute("""
            query {
              members(last: 100) { totalCount }
            }
            """);

        assertRejected(result);
        assertThat(dataFetcherInvocations.get()).isZero();
    }

    @Test
    @DisplayName("범위를 벗어난 first는 최대 크기 비용으로 계산되어 우회할 수 없다")
    void 범위를_벗어난_first는_최대_크기_비용으로_계산되어_우회할_수_없다() throws IOException {
        AtomicInteger dataFetcherInvocations = new AtomicInteger();
        ExecutionResult result = actualGraphQl(dataFetcherInvocations, 22).execute("""
            query {
              members(first: 101) { totalCount }
            }
            """);

        assertRejected(result);
        assertThat(dataFetcherInvocations.get()).isZero();
    }

    @Test
    @DisplayName("first와 last를 함께 주면 실제 최대 edge 수로 비용을 계산한다")
    void first와_last를_함께_주면_실제_최대_edge_수로_비용을_계산한다() throws IOException {
        AtomicInteger dataFetcherInvocations = new AtomicInteger();
        ExecutionResult result = actualGraphQl(dataFetcherInvocations, 4).execute("""
            query {
              members(first: 5, last: 2) { totalCount }
            }
            """);

        assertThat(result.getErrors()).isEmpty();
        assertThat(dataFetcherInvocations.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Connection이 아닌 필드는 기본 child complexity + 1 비용을 유지한다")
    void Connection이_아닌_필드는_기본_child_complexity_1_비용을_유지한다() throws IOException {
        AtomicInteger dataFetcherInvocations = new AtomicInteger();
        ExecutionResult result = actualGraphQl(dataFetcherInvocations, 2).execute("""
            query {
              me { id }
            }
            """);

        assertThat(result.getErrors()).isEmpty();
        Map<String, Object> data = result.getData();
        assertThat(data).isEqualTo(Map.of(
            "me",
            Map.of("id", GlobalId.encode(GlobalIdTypes.MEMBER, 1L))
        ));
        assertThat(dataFetcherInvocations.get()).isEqualTo(1);
    }

    private static GraphQL actualGraphQl(AtomicInteger dataFetcherInvocations, int maxComplexity) throws IOException {
        Resource[] schemaResources = new PathMatchingResourcePatternResolver()
            .getResources("classpath*:graphql/**/*.graphqls");

        GraphQlSource graphQlSource = GraphQlSource.schemaResourceBuilder()
            .schemaResources(schemaResources)
            .configureRuntimeWiring(new GraphQlRuntimeWiringConfig().graphQlRuntimeWiringConfigurer())
            .configureRuntimeWiring(builder -> builder.type("Query", type -> {
                type.dataFetcher("members", environment -> {
                    dataFetcherInvocations.incrementAndGet();
                    return Map.of(
                        "edges", List.of(),
                        "pageInfo", Map.of(
                            "hasNextPage", false,
                            "hasPreviousPage", false
                        ),
                        "totalCount", 0L
                    );
                });
                type.dataFetcher("me", environment -> {
                    dataFetcherInvocations.incrementAndGet();
                    return Map.of("id", GlobalId.encode(GlobalIdTypes.MEMBER, 1L));
                });
                return type;
            }))
            .configureGraphQl(graphQl -> graphQl.instrumentation(complexityInstrumentation(maxComplexity)))
            .build();

        return graphQlSource.graphQl();
    }

    private static Instrumentation complexityInstrumentation(int maxComplexity) {
        GraphQlExecutionProperties properties = new GraphQlExecutionProperties(
            Duration.ofSeconds(5), 10, maxComplexity
        );
        return new GraphQlExecutionConfig().graphQlMaxQueryComplexityInstrumentation(properties);
    }

    private static void assertRejected(ExecutionResult result) {
        assertThat(result.getErrors())
            .anySatisfy(error -> assertThat(error.getMessage()).contains("maximum query complexity exceeded"));
        Object data = result.getData();
        assertThat(data).isNull();
    }
}
