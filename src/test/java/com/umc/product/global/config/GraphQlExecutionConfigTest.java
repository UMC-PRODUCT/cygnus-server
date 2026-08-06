package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.graphql.execution.GraphQlSource;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.execution.instrumentation.Instrumentation;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

class GraphQlExecutionConfigTest {

    @Test
    @DisplayName("memberSearch는 기본 page.size 비용을 포함한 설정 한도에서 실행된다")
    void memberSearch는_기본_page_size_비용을_포함한_설정_한도에서_실행된다() throws IOException {
        AtomicInteger dataFetcherInvocations = new AtomicInteger();
        ExecutionResult result = actualGraphQl(dataFetcherInvocations, 22).execute("""
            query {
              memberSearch(input: { keyword: "kim" }) { page }
            }
            """);

        assertThat(result.getErrors()).isEmpty();
        Map<String, Object> data = result.getData();
        assertThat(data).isEqualTo(Map.of("memberSearch", Map.of("page", 0)));
        assertThat(dataFetcherInvocations.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("page에서 size를 생략하면 기본 page.size 비용으로 실행된다")
    void page에서_size를_생략하면_기본_page_size_비용으로_실행된다() {
        AtomicInteger dataFetcherInvocations = new AtomicInteger();
        ExecutionResult result = pageWithoutSizeDefaultGraphQl(dataFetcherInvocations, 22).execute("""
            query {
              memberSearch(input: { keyword: "kim" }, page: { page: 1 }) { page }
            }
            """);

        assertThat(result.getErrors()).isEmpty();
        assertThat(dataFetcherInvocations.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("memberSearch alias 두 개와 큰 page.size는 data fetcher 전에 거부된다")
    void memberSearch_alias_두_개와_큰_page_size는_data_fetcher_전에_거부된다() throws IOException {
        AtomicInteger dataFetcherInvocations = new AtomicInteger();
        ExecutionResult result = actualGraphQl(dataFetcherInvocations, 203).execute("""
            query {
              first: memberSearch(input: { keyword: "kim" }, page: { size: 100 }) { page }
              second: memberSearch(input: { keyword: "lee" }, page: { size: 100 }) { page }
            }
            """);

        assertRejected(result);
        assertThat(dataFetcherInvocations.get()).isZero();
    }

    @Test
    @DisplayName("size 1은 정확한 복잡도 한도에서 실행된다")
    void size_1은_정확한_복잡도_한도에서_실행된다() throws IOException {
        AtomicInteger dataFetcherInvocations = new AtomicInteger();
        ExecutionResult result = actualGraphQl(dataFetcherInvocations, 3).execute("""
            query {
              memberSearch(input: { keyword: "kim" }, page: { size: 1 }) { page }
            }
            """);

        assertThat(result.getErrors()).isEmpty();
        assertThat(dataFetcherInvocations.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("size 100은 size 1의 정확한 복잡도 한도를 초과한다")
    void size_100은_size_1의_정확한_복잡도_한도를_초과한다() throws IOException {
        AtomicInteger dataFetcherInvocations = new AtomicInteger();
        ExecutionResult result = actualGraphQl(dataFetcherInvocations, 3).execute("""
            query {
              memberSearch(input: { keyword: "kim" }, page: { size: 100 }) { page }
            }
            """);

        assertRejected(result);
        assertThat(dataFetcherInvocations.get()).isZero();
    }

    @Test
    @DisplayName("범위를 벗어난 size는 최대 크기 비용으로 계산되어 우회할 수 없다")
    void 범위를_벗어난_size는_최대_크기_비용으로_계산되어_우회할_수_없다() throws IOException {
        AtomicInteger dataFetcherInvocations = new AtomicInteger();
        ExecutionResult result = actualGraphQl(dataFetcherInvocations, 22).execute("""
            query {
              memberSearch(input: { keyword: "kim" }, page: { size: 101 }) { page }
            }
            """);

        assertRejected(result);
        assertThat(dataFetcherInvocations.get()).isZero();
    }

    @Test
    @DisplayName("page가 맵이 아니면 최대 크기 비용으로 계산되어 우회할 수 없다")
    void page가_맵이_아니면_최대_크기_비용으로_계산되어_우회할_수_없다() {
        AtomicInteger dataFetcherInvocations = new AtomicInteger();
        ExecutionResult result = malformedPageGraphQl(dataFetcherInvocations, 2).execute("""
            query {
              memberSearch(input: "kim", page: "not-a-map") { page }
            }
            """);

        assertRejected(result);
        assertThat(dataFetcherInvocations.get()).isZero();
    }

    @Test
    @DisplayName("memberSearch가 아닌 필드는 기본 child complexity + 1 비용을 유지한다")
    void memberSearch가_아닌_필드는_기본_child_complexity_1_비용을_유지한다() throws IOException {
        AtomicInteger dataFetcherInvocations = new AtomicInteger();
        ExecutionResult result = actualGraphQl(dataFetcherInvocations, 2).execute("""
            query {
              me { memberId }
            }
            """);

        assertThat(result.getErrors()).isEmpty();
        Map<String, Object> data = result.getData();
        assertThat(data).isEqualTo(Map.of("me", Map.of("memberId", "1")));
        assertThat(dataFetcherInvocations.get()).isEqualTo(1);
    }

    private static GraphQL actualGraphQl(AtomicInteger dataFetcherInvocations, int maxComplexity) throws IOException {
        Resource[] schemaResources = new PathMatchingResourcePatternResolver()
            .getResources("classpath*:graphql/**/*.graphqls");

        GraphQlSource graphQlSource = GraphQlSource.schemaResourceBuilder()
            .schemaResources(schemaResources)
            .configureRuntimeWiring(new GraphQlRuntimeWiringConfig().graphQlRuntimeWiringConfigurer())
            .configureRuntimeWiring(builder -> builder.type("Query", type -> {
                type.dataFetcher("memberSearch", environment -> {
                    dataFetcherInvocations.incrementAndGet();
                    return Map.of("page", 0);
                });
                type.dataFetcher("me", environment -> {
                    dataFetcherInvocations.incrementAndGet();
                    return Map.of("memberId", "1");
                });
                return type;
            }))
            .configureGraphQl(graphQl -> graphQl.instrumentation(complexityInstrumentation(maxComplexity)))
            .build();

        return graphQlSource.graphQl();
    }

    private static GraphQL malformedPageGraphQl(AtomicInteger dataFetcherInvocations, int maxComplexity) {
        GraphQLObjectType pageType = GraphQLObjectType.newObject()
            .name("MemberPage")
            .field(field -> field.name("page").type(Scalars.GraphQLInt))
            .build();
        GraphQLObjectType queryType = GraphQLObjectType.newObject()
            .name("Query")
            .field(field -> field
                .name("memberSearch")
                .type(pageType)
                .argument(argument -> argument.name("input").type(Scalars.GraphQLString))
                .argument(argument -> argument.name("page").type(Scalars.GraphQLString))
                .dataFetcher(environment -> {
                    dataFetcherInvocations.incrementAndGet();
                    return Map.of("page", 0);
                }))
            .build();
        GraphQLSchema schema = GraphQLSchema.newSchema().query(queryType).build();

        return GraphQL.newGraphQL(schema)
            .instrumentation(complexityInstrumentation(maxComplexity))
            .build();
    }

    private static GraphQL pageWithoutSizeDefaultGraphQl(
        AtomicInteger dataFetcherInvocations,
        int maxComplexity
    ) {
        GraphQLInputObjectType searchInputType = GraphQLInputObjectType.newInputObject()
            .name("MemberSearchInput")
            .field(field -> field.name("keyword").type(Scalars.GraphQLString))
            .build();
        GraphQLInputObjectType pageInputType = GraphQLInputObjectType.newInputObject()
            .name("MemberPageInput")
            .field(field -> field.name("page").type(Scalars.GraphQLInt))
            .field(field -> field.name("size").type(Scalars.GraphQLInt))
            .build();
        GraphQLObjectType pageType = GraphQLObjectType.newObject()
            .name("MemberPage")
            .field(field -> field.name("page").type(Scalars.GraphQLInt))
            .build();
        GraphQLObjectType queryType = GraphQLObjectType.newObject()
            .name("Query")
            .field(field -> field
                .name("memberSearch")
                .type(pageType)
                .argument(argument -> argument.name("input").type(searchInputType))
                .argument(argument -> argument.name("page").type(pageInputType))
                .dataFetcher(environment -> {
                    dataFetcherInvocations.incrementAndGet();
                    return Map.of("page", 1);
                }))
            .build();
        GraphQLSchema schema = GraphQLSchema.newSchema().query(queryType).build();

        return GraphQL.newGraphQL(schema)
            .instrumentation(complexityInstrumentation(maxComplexity))
            .build();
    }

    private static Instrumentation complexityInstrumentation(int maxComplexity) {
        GraphQlExecutionProperties properties = new GraphQlExecutionProperties(Duration.ofSeconds(5), 10, maxComplexity);
        return new GraphQlExecutionConfig().graphQlMaxQueryComplexityInstrumentation(properties);
    }

    private static void assertRejected(ExecutionResult result) {
        assertThat(result.getErrors())
            .anySatisfy(error -> assertThat(error.getMessage()).contains("maximum query complexity exceeded"));
        Object data = result.getData();
        assertThat(data).isNull();
    }
}
