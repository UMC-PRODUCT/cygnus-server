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
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLType;

class GraphQlRuntimeWiringConfigTest {

    @Test
    @DisplayName("중첩 디렉터리의 도메인 GraphQL IDL을 하나의 schema로 조립한다")
    void nestedDomainGraphQlContractsAreLoadedAsOneSchema() throws IOException {
        Resource[] schemaResources = schemaResources();

        assertThat(Arrays.stream(schemaResources).map(Resource::getDescription))
            .anyMatch(description -> description.contains("graphql/project/request.graphqls"))
            .anyMatch(description -> description.contains("graphql/project/response.graphqls"))
            .anyMatch(description -> description.contains("graphql/recruiting/request.graphqls"))
            .anyMatch(description -> description.contains("graphql/recruiting/response.graphqls"));

        GraphQlSource graphQlSource = graphQlSource(schemaResources);

        assertThat(graphQlSource.schema().getType("Project")).isNotNull();
        assertThat(graphQlSource.schema().getType("RecruitingApplicationForm")).isNotNull();
        assertThat(graphQlSource.schema().getType("Long")).isInstanceOf(GraphQLScalarType.class);
        assertThat(graphQlSource.schema().getType("Instant")).isInstanceOf(GraphQLScalarType.class);
        assertThat(graphQlSource.schema().getType("LocalDate")).isInstanceOf(GraphQLScalarType.class);
        assertThat(graphQlSource.schema().getType("LocalDateTime")).isInstanceOf(GraphQLScalarType.class);
        assertThat(graphQlSource.schema().getType("PageInput")).isInstanceOf(GraphQLInputObjectType.class);
        assertThat(graphQlSource.schema().getType("PageInfo")).isInstanceOf(GraphQLObjectType.class);
    }

    @Test
    @DisplayName("공통 pagination 계약은 도메인 page type의 metadata를 제공한다")
    void sharedPaginationContractProvidesDomainPageMetadata() throws IOException {
        GraphQlSource graphQlSource = graphQlSource(schemaResources());

        assertInputFieldNames((GraphQLInputObjectType) graphQlSource.schema().getType("PageInput"), "page", "size");
        assertFieldNames((GraphQLObjectType) graphQlSource.schema().getType("PageInfo"),
            "page", "size", "totalElements", "totalPages", "hasNext");
        assertFieldNames((GraphQLObjectType) graphQlSource.schema().getType("MemberSearchPage"),
            "content", "pageInfo");
        assertFieldNames((GraphQLObjectType) graphQlSource.schema().getType("ProjectPage"),
            "content", "pageInfo");
        assertFieldNames((GraphQLObjectType) graphQlSource.schema().getType("RecruitingApplicationPage"),
            "content", "pageInfo");
    }

    @Test
    @DisplayName("Challenger와 Form provider IDL은 표준 output 계약을 제공한다")
    void providerGraphQlContractsExposeCanonicalOutputs() throws IOException {
        GraphQlSource graphQlSource = graphQlSource(schemaResources());

        assertThat(graphQlSource.schema().getType("ChallengerPart")).isInstanceOf(GraphQLEnumType.class);
        assertThat(graphQlSource.schema().getType("ChallengerTrack")).isInstanceOf(GraphQLEnumType.class);
        assertThat(graphQlSource.schema().getType("ChallengerStatus")).isInstanceOf(GraphQLEnumType.class);
        assertThat(graphQlSource.schema().getType("Form")).isInstanceOf(GraphQLObjectType.class);
        assertThat(graphQlSource.schema().getType("FormSection")).isInstanceOf(GraphQLObjectType.class);
        assertThat(graphQlSource.schema().getType("FormQuestion")).isInstanceOf(GraphQLObjectType.class);
        assertThat(graphQlSource.schema().getType("FormOption")).isInstanceOf(GraphQLObjectType.class);

        assertFieldNames((GraphQLObjectType) graphQlSource.schema().getType("Form"),
            "formId", "createdMemberId", "title", "description", "status", "anonymous",
            "allowDuplicateResponses", "createdAt", "updatedAt", "sections");
        assertFieldNames((GraphQLObjectType) graphQlSource.schema().getType("FormSection"),
            "sectionId", "title", "description", "orderNo", "questions");
        assertFieldNames((GraphQLObjectType) graphQlSource.schema().getType("FormQuestion"),
            "questionId", "type", "title", "description", "required", "orderNo", "options");
        assertFieldNames((GraphQLObjectType) graphQlSource.schema().getType("FormOption"),
            "optionId", "content", "orderNo", "other", "nextSectionId");

        assertEnumValues((GraphQLEnumType) graphQlSource.schema().getType("ChallengerPart"),
            "PLAN", "DESIGN", "WEB", "ANDROID", "IOS", "NODEJS", "SPRINGBOOT", "ADMIN");
        assertEnumValues((GraphQLEnumType) graphQlSource.schema().getType("ChallengerTrack"),
            "PLAN", "DESIGN", "WEB_PRODUCT_ENGINEER", "MOBILE_PRODUCT_ENGINEER", "INFRA_PLUS");
        assertEnumValues((GraphQLEnumType) graphQlSource.schema().getType("FormStatus"),
            "DRAFT", "PUBLISHED", "CLOSED");
        assertEnumValues((GraphQLEnumType) graphQlSource.schema().getType("FormResponseStatus"),
            "DRAFT", "SUBMITTED");
        assertEnumValues((GraphQLEnumType) graphQlSource.schema().getType("QuestionType"),
            "SHORT_TEXT", "LONG_TEXT", "RADIO", "CHECKBOX", "DROPDOWN", "SCHEDULE", "FILE",
            "PORTFOLIO");
    }

    @Test
    @DisplayName("Recruiting GraphQL은 Form을 트랙 정책에 맞춘 자체 projection으로 제공한다")
    void recruitingGraphQlUsesDomainOwnedFormProjection() throws IOException {
        GraphQlSource graphQlSource = graphQlSource(schemaResources());

        GraphQLObjectType recruitingForm =
            (GraphQLObjectType) graphQlSource.schema().getType("RecruitingApplicationFormStructure");
        assertThat(recruitingForm.getInterfaces()).isEmpty();
        assertThat(typeName(recruitingForm.getFieldDefinition("sections").getType()))
            .isEqualTo("[RecruitingFormSection!]!");

        GraphQLObjectType recruitingSection =
            (GraphQLObjectType) graphQlSource.schema().getType("RecruitingFormSection");
        assertThat(recruitingSection.getInterfaces()).isEmpty();
        assertThat(typeName(recruitingSection.getFieldDefinition("questions").getType()))
            .isEqualTo("[RecruitingFormQuestion!]!");

        GraphQLObjectType recruitingQuestion =
            (GraphQLObjectType) graphQlSource.schema().getType("RecruitingFormQuestion");
        assertThat(typeName(recruitingQuestion.getFieldDefinition("options").getType()))
            .isEqualTo("[RecruitingFormQuestionOption!]!");
    }

    @Test
    @DisplayName("Project GraphQL은 canonical Member와 Project 소유 Form projection을 제공한다")
    void projectGraphQlUsesCanonicalMemberAndDomainOwnedFormProjection() throws IOException {
        GraphQlSource graphQlSource = graphQlSource(schemaResources());

        assertThat(graphQlSource.schema().getType("MemberSummary")).isNull();
        assertThat(graphQlSource.schema().getType("MemberBrief")).isNull();
        assertThat(graphQlSource.schema().getType("MemberPublic")).isInstanceOf(GraphQLObjectType.class);
        assertThat(graphQlSource.schema().getType("MemberPrivate")).isInstanceOf(GraphQLObjectType.class);

        GraphQLObjectType project = (GraphQLObjectType) graphQlSource.schema().getType("Project");
        assertThat(typeName(project.getFieldDefinition("productOwner").getType())).isEqualTo("MemberPublic");
        assertThat(typeName(project.getFieldDefinition("coProductOwners").getType()))
            .isEqualTo("[MemberPublic!]!");

        GraphQLObjectType projectForm =
            (GraphQLObjectType) graphQlSource.schema().getType("ProjectApplicationForm");
        assertThat(projectForm.getInterfaces()).isEmpty();
        assertFieldNames(projectForm,
            "projectId", "applicationFormId", "title", "description", "sections");
        assertThat(typeName(projectForm.getFieldDefinition("sections").getType()))
            .isEqualTo("[ProjectApplicationFormSection!]!");

        GraphQLObjectType projectSection =
            (GraphQLObjectType) graphQlSource.schema().getType("ProjectApplicationFormSection");
        assertThat(projectSection.getInterfaces()).isEmpty();
        assertFieldNames(projectSection,
            "sectionId", "type", "allowedParts", "title", "description", "orderNo", "questions");
        assertThat(typeName(projectSection.getFieldDefinition("questions").getType()))
            .isEqualTo("[ProjectApplicationFormQuestion!]!");

        GraphQLObjectType responseQuestion =
            (GraphQLObjectType) graphQlSource.schema().getType("ProjectApplicationResponseQuestion");
        assertThat(typeName(responseQuestion.getFieldDefinition("options").getType()))
            .isEqualTo("[ProjectApplicationFormOption!]!");

        assertThat(graphQlSource.schema().getType("ApplicationFormSection")).isNull();
        assertThat(graphQlSource.schema().getType("ApplicationFormQuestion")).isNull();
        assertThat(graphQlSource.schema().getType("ApplicationFormOption")).isNull();
    }

    @Test
    @DisplayName("Organization GraphQL은 Gisu, Chapter, School canonical type을 사용한다")
    void organizationGraphQlUsesCanonicalTypes() throws IOException {
        GraphQlSource graphQlSource = graphQlSource(schemaResources());

        GraphQLObjectType gisu = (GraphQLObjectType) graphQlSource.schema().getType("Gisu");
        GraphQLObjectType chapter = (GraphQLObjectType) graphQlSource.schema().getType("Chapter");
        GraphQLObjectType school = (GraphQLObjectType) graphQlSource.schema().getType("School");
        GraphQLObjectType member = (GraphQLObjectType) graphQlSource.schema().getType("MemberPublic");

        assertThat(typeName(gisu.getFieldDefinition("chapters").getType())).isEqualTo("[Chapter!]!");
        assertThat(typeName(gisu.getFieldDefinition("schools").getType())).isEqualTo("[School!]!");
        assertThat(typeName(chapter.getFieldDefinition("schools").getType())).isEqualTo("[School!]!");
        assertThat(typeName(member.getFieldDefinition("school").getType())).isEqualTo("School");
        assertFieldNames(school,
            "id", "name", "remark", "logoImageUrl", "links", "active", "createdAt", "updatedAt");

        assertThat(graphQlSource.schema().getType("GisuChapter")).isNull();
        assertThat(graphQlSource.schema().getType("ChapterSchool")).isNull();
        assertThat(graphQlSource.schema().getType("GisuSchool")).isNull();
        assertThat(graphQlSource.schema().getType("SchoolDetail")).isNull();
    }

    @Test
    @DisplayName("Project response IDL은 provider 선언을 재정의하지 않는다")
    void projectGraphQlResponseDoesNotOwnProviderDeclarations() throws IOException {
        Resource projectSchema = new PathMatchingResourcePatternResolver()
            .getResource("classpath:graphql/project/response.graphqls");
        String source = new String(projectSchema.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(source)
            .doesNotContain("scalar Long", "enum ChallengerPart", "enum FormResponseStatus", "enum QuestionType",
                "type MemberPublic {", "type Form {");
    }

    @Test
    @DisplayName("GraphQL 실행 제한은 timeout과 depth, complexity 빈으로 구성된다")
    void graphQlExecutionLimitsAreConfigured() {
        GraphQlExecutionProperties properties = new GraphQlExecutionProperties(Duration.ofSeconds(5), 10, 200);
        GraphQlExecutionConfig config = new GraphQlExecutionConfig();

        assertThat(config.graphQlTimeoutWebGraphQlInterceptor(properties))
            .isInstanceOf(TimeoutWebGraphQlInterceptor.class);
        assertThat(config.graphQlMaxQueryDepthInstrumentation(properties))
            .isInstanceOf(MaxQueryDepthInstrumentation.class);
        assertThat(config.graphQlMaxQueryComplexityInstrumentation(properties))
            .isInstanceOf(MaxQueryComplexityInstrumentation.class);
    }

    private Resource[] schemaResources() throws IOException {
        return new PathMatchingResourcePatternResolver()
            .getResources("classpath*:graphql/**/*.graphqls");
    }

    private GraphQlSource graphQlSource(Resource[] schemaResources) {
        return GraphQlSource.schemaResourceBuilder()
            .schemaResources(schemaResources)
            .configureRuntimeWiring(new GraphQlRuntimeWiringConfig().graphQlRuntimeWiringConfigurer())
            .build();
    }

    private void assertFieldNames(graphql.schema.GraphQLFieldsContainer type, String... fieldNames) {
        assertThat(type.getFieldDefinitions())
            .extracting(GraphQLFieldDefinition::getName)
            .containsExactly(fieldNames);
    }

    private void assertInputFieldNames(GraphQLInputObjectType type, String... fieldNames) {
        assertThat(type.getFieldDefinitions())
            .extracting(GraphQLInputObjectField::getName)
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
