package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.graphql.server.TimeoutWebGraphQlInterceptor;

import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;

class GraphQlRuntimeWiringConfigTest {

    @Test
    @DisplayName("Project GraphQL schema는 Long scalar wiring과 함께 로드된다")
    void projectGraphQlSchemaLoadsWithLongScalar() throws IOException {
        Resource[] schemaResources = new PathMatchingResourcePatternResolver()
            .getResources("classpath*:graphql/**/*.graphqls");

        assertThat(Arrays.stream(schemaResources).map(Resource::getFilename))
            .contains("organization.graphqls", "project.graphqls");

        GraphQlSource graphQlSource = GraphQlSource.schemaResourceBuilder()
            .schemaResources(schemaResources)
            .configureRuntimeWiring(new GraphQlRuntimeWiringConfig().graphQlRuntimeWiringConfigurer())
            .build();

        assertThat(graphQlSource.schema().getType("Project")).isNotNull();
        assertThat(graphQlSource.schema().getType("Long")).isNotNull();
    }

    @Test
    @DisplayName("공통 GraphQL schema는 common과 form 계약을 모두 로드한다")
    void sharedGraphQlSchemaLoadsCommonAndFormContracts() throws IOException {
        Resource[] schemaResources = new PathMatchingResourcePatternResolver()
            .getResources("classpath*:graphql/**/*.graphqls");

        assertThat(Arrays.stream(schemaResources).map(Resource::getFilename))
            .contains("common.graphqls", "form.graphqls");

        GraphQlSource graphQlSource = GraphQlSource.schemaResourceBuilder()
            .schemaResources(schemaResources)
            .configureRuntimeWiring(new GraphQlRuntimeWiringConfig().graphQlRuntimeWiringConfigurer())
            .build();

        assertThat(graphQlSource.schema().getType("Long")).isInstanceOf(graphql.schema.GraphQLScalarType.class);
        assertThat(graphQlSource.schema().getType("ChallengerPart"))
            .isInstanceOf(GraphQLEnumType.class);
        assertThat(graphQlSource.schema().getType("Form"))
            .isInstanceOf(GraphQLInterfaceType.class);
        assertThat(graphQlSource.schema().getType("FormSection"))
            .isInstanceOf(GraphQLInterfaceType.class);
        assertThat(graphQlSource.schema().getType("FormQuestion"))
            .isInstanceOf(GraphQLObjectType.class);
        assertThat(graphQlSource.schema().getType("FormOption"))
            .isInstanceOf(GraphQLObjectType.class);

        assertFieldNames((GraphQLInterfaceType) graphQlSource.schema().getType("Form"),
            "title", "description", "sections");
        assertFieldNames((GraphQLInterfaceType) graphQlSource.schema().getType("FormSection"),
            "sectionId", "title", "description", "orderNo", "questions");
        assertFieldNames((GraphQLObjectType) graphQlSource.schema().getType("FormQuestion"),
            "questionId", "type", "title", "description", "required", "orderNo", "options");
        assertFieldNames((GraphQLObjectType) graphQlSource.schema().getType("FormOption"),
            "optionId", "content", "orderNo", "other");

        assertEnumValues((GraphQLEnumType) graphQlSource.schema().getType("ChallengerPart"),
            "PLAN", "DESIGN", "WEB", "ANDROID", "IOS", "NODEJS", "SPRINGBOOT", "ADMIN");
        assertEnumValues((GraphQLEnumType) graphQlSource.schema().getType("FormStatus"),
            "DRAFT", "PUBLISHED");
        assertEnumValues((GraphQLEnumType) graphQlSource.schema().getType("FormResponseStatus"),
            "DRAFT", "SUBMITTED");
        assertEnumValues((GraphQLEnumType) graphQlSource.schema().getType("QuestionType"),
            "SHORT_TEXT", "LONG_TEXT", "RADIO", "CHECKBOX", "DROPDOWN", "SCHEDULE", "FILE",
            "PORTFOLIO");
    }

    @Test
    @DisplayName("Project GraphQL은 MemberSummary와 shared form 구현 계약을 노출한다")
    void projectGraphQlSchemaUsesSummaryAndFormImplementations() throws IOException {
        Resource[] schemaResources = new PathMatchingResourcePatternResolver()
            .getResources("classpath*:graphql/**/*.graphqls");

        GraphQlSource graphQlSource = GraphQlSource.schemaResourceBuilder()
            .schemaResources(schemaResources)
            .configureRuntimeWiring(new GraphQlRuntimeWiringConfig().graphQlRuntimeWiringConfigurer())
            .build();

        assertThat(graphQlSource.schema().getType("MemberSummary")).isInstanceOf(GraphQLObjectType.class);
        assertFieldNames((GraphQLObjectType) graphQlSource.schema().getType("MemberSummary"),
            "memberId", "nickname", "name", "schoolName");

        GraphQLObjectType project = (GraphQLObjectType) graphQlSource.schema().getType("Project");
        assertThat(typeName(project.getFieldDefinition("productOwner").getType())).isEqualTo("MemberSummary");
        assertThat(typeName(project.getFieldDefinition("coProductOwners").getType()))
            .isEqualTo("[MemberSummary!]!");

        GraphQLObjectType projectApplicationForm =
            (GraphQLObjectType) graphQlSource.schema().getType("ProjectApplicationForm");
        assertThat(projectApplicationForm.getInterfaces())
            .extracting(interfaceType -> interfaceType.getName())
            .containsExactly("Form");
        assertFieldNames(projectApplicationForm,
            "projectId", "applicationFormId", "title", "description", "sections");
        assertThat(typeName(projectApplicationForm.getFieldDefinition("sections").getType()))
            .isEqualTo("[ApplicationFormSection!]!");

        GraphQLObjectType applicationFormSection =
            (GraphQLObjectType) graphQlSource.schema().getType("ApplicationFormSection");
        assertThat(applicationFormSection.getInterfaces())
            .extracting(interfaceType -> interfaceType.getName())
            .containsExactly("FormSection");
        assertFieldNames(applicationFormSection,
            "sectionId", "type", "allowedParts", "title", "description", "orderNo", "questions");
        assertThat(typeName(applicationFormSection.getFieldDefinition("questions").getType()))
            .isEqualTo("[FormQuestion!]!");

        GraphQLObjectType responseQuestion =
            (GraphQLObjectType) graphQlSource.schema().getType("ProjectApplicationResponseQuestion");
        assertThat(typeName(responseQuestion.getFieldDefinition("options").getType()))
            .isEqualTo("[FormOption!]!");

        assertThat(graphQlSource.schema().getType("MemberBrief")).isNull();
        assertThat(graphQlSource.schema().getType("ApplicationFormQuestion")).isNull();
        assertThat(graphQlSource.schema().getType("ApplicationFormOption")).isNull();
    }

    @Test
    @DisplayName("Project GraphQL schema는 shared 선언을 소유하지 않는다")
    void projectGraphQlSchemaDoesNotOwnSharedDeclarations() throws IOException {
        Resource projectSchema = new PathMatchingResourcePatternResolver()
            .getResource("classpath:graphql/project.graphqls");
        String projectSchemaSource = new String(projectSchema.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(projectSchemaSource)
            .doesNotContain("scalar Long", "enum ChallengerPart", "enum FormResponseStatus", "enum QuestionType");
    }

    @Test
    @DisplayName("GraphQL 실행 제한은 timeout과 depth, complexity 빈으로 구성된다")
    void GraphQL_실행_제한은_timeout과_depth_complexity_빈으로_구성된다() {
        GraphQlExecutionProperties properties = new GraphQlExecutionProperties(Duration.ofSeconds(5), 10, 200);
        GraphQlExecutionConfig config = new GraphQlExecutionConfig();

        assertThat(config.graphQlTimeoutWebGraphQlInterceptor(properties))
            .isInstanceOf(TimeoutWebGraphQlInterceptor.class);
        assertThat(config.graphQlMaxQueryDepthInstrumentation(properties))
            .isInstanceOf(MaxQueryDepthInstrumentation.class);
        assertThat(config.graphQlMaxQueryComplexityInstrumentation(properties))
            .isInstanceOf(MaxQueryComplexityInstrumentation.class);
    }

    private void assertFieldNames(graphql.schema.GraphQLFieldsContainer type, String... fieldNames) {
        assertThat(type.getFieldDefinitions())
            .extracting(GraphQLFieldDefinition::getName)
            .containsExactly(fieldNames);
    }

    private void assertEnumValues(GraphQLEnumType type, String... enumValues) {
        assertThat(type.getValues())
            .extracting(value -> value.getName())
            .containsExactly(enumValues);
    }

    private String typeName(GraphQLType type) {
        if (type instanceof GraphQLNonNull nonNull) {
            return typeName(nonNull.getWrappedType()) + "!";
        }
        if (type instanceof GraphQLList list) {
            return "[" + typeName(list.getWrappedType()) + "]";
        }
        return ((GraphQLNamedType) type).getName();
    }
}
