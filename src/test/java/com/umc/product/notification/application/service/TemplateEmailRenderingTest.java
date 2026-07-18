package com.umc.product.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.umc.product.notification.domain.EmailTemplateType;

class TemplateEmailRenderingTest {

    private static final String ACTION_URL = "https://university.neordinary.com/recruiting/interview?slot=1#confirm";
    private static final String PRIVACY_URL =
        "https://makeus-challenge.notion.site/2b4b57f4596b80d683ecd6f15f0b0f7c?source=copy_link";
    private static final Path PREVIEW_DIRECTORY = Path.of("build/reports/template-email-preview");

    private final Map<EmailTemplateType, String> templateNames = new EnumMap<>(EmailTemplateType.class);
    private EmailTemplateCatalog catalog;
    private SpringTemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(false);

        templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        catalog = new EmailTemplateCatalog(List.of("https://university.neordinary.com"));
        for (EmailTemplateType type : EmailTemplateType.values()) {
            templateNames.put(type, catalog.templateResourcePath(type));
        }
    }

    @Test
    @DisplayName("4종 채용 메일이 실제 Thymeleaf로 UTF-8 preview를 만든다")
    void 네_종류_템플릿을_실제_렌더링한다() throws IOException {
        Files.createDirectories(PREVIEW_DIRECTORY);

        for (EmailTemplateType type : EmailTemplateType.values()) {
            String rendered = render(type, values(type));
            assertSafeRenderedHtml(type, rendered);
            assertTypeCopy(type, rendered);
            Files.writeString(
                PREVIEW_DIRECTORY.resolve(type.name().toLowerCase(Locale.ROOT) + ".html"),
                rendered,
                StandardCharsets.UTF_8
            );
        }

        try (Stream<Path> previews = Files.list(PREVIEW_DIRECTORY)) {
            assertThat(previews.filter(path -> path.toString().endsWith(".html")).count()).isEqualTo(4);
        }
    }

    @Test
    @DisplayName("동적 값은 HTML/Thymeleaf injection 없이 text와 href로 escape된다")
    void 동적_값을_escape한다() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("applicantName", "<script>alert(1)</script> ${th:text}");
        String rendered = render(EmailTemplateType.RECRUITMENT_FINAL_FAILED, values);

        assertThat(rendered)
            .contains("&lt;script&gt;alert(1)&lt;/script&gt;")
            .contains("${th:text}")
            .doesNotContain("<script>alert(1)</script>")
            .doesNotContain("th:text=")
            .doesNotContain("th:href=");
    }

    @Test
    @DisplayName("서류 합격 메일만 정확히 하나의 CTA를 갖고 나머지는 CTA가 없다")
    void cta_개수를_검증한다() {
        String documentPassed = render(
            EmailTemplateType.RECRUITMENT_DOCUMENT_PASSED_INTERVIEW_AVAILABILITY_REQUEST,
            values(EmailTemplateType.RECRUITMENT_DOCUMENT_PASSED_INTERVIEW_AVAILABILITY_REQUEST)
        );
        assertThat(documentPassed).contains("면접 가능 시간 제출하기");
        assertThat(count(documentPassed, "면접 가능 시간 제출하기")).isEqualTo(1);
        assertThat(count(documentPassed, "href=\"" + ACTION_URL)).isEqualTo(1);

        for (EmailTemplateType type : new EmailTemplateType[] {
            EmailTemplateType.RECRUITMENT_INTERVIEW_CONFIRMATION,
            EmailTemplateType.RECRUITMENT_FINAL_PASSED,
            EmailTemplateType.RECRUITMENT_FINAL_FAILED
        }) {
            String rendered = render(type, values(type));
            assertThat(rendered).doesNotContain("면접 가능 시간 제출하기");
        }
    }

    private String render(EmailTemplateType type, Map<String, String> values) {
        Context context = new Context(Locale.KOREAN);
        values.forEach(context::setVariable);
        return templateEngine.process(templateNames.get(type), context);
    }

    private void assertSafeRenderedHtml(EmailTemplateType type, String rendered) {
        assertThat(rendered)
            .as(type.name())
            .contains("<html lang=\"ko\"")
            .contains("data-email-banner=\"empty\"")
            .contains("word-break: keep-all")
            .contains("white-space: nowrap")
            .doesNotContain("${applicantName}")
            .doesNotContain("${contactSnapshot}")
            .doesNotContain("${actionUrl}")
            .doesNotContain("${interviewDate}")
            .doesNotContain("${interviewTime}")
            .doesNotContain("${location}")
            .doesNotContain("${acceptedTrack}")
            .doesNotMatch("(?s).*\\bth:(?:text|href|replace|insert|fragment)=.*")
            .doesNotContain("<img")
            .doesNotContain("src=")
            .doesNotContain("daangn")
            .doesNotContain("tracking")
            .doesNotContain("@font-face")
            .doesNotContain("<script")
            .contains("https://university.neordinary.com")
            .contains(PRIVACY_URL);
        assertThat(rendered.getBytes(StandardCharsets.UTF_8)).isNotEmpty();
    }

    private void assertTypeCopy(EmailTemplateType type, String rendered) {
        String visibleText = rendered.replaceAll("(?s)<[^>]*>", "");
        switch (type) {
            case RECRUITMENT_DOCUMENT_PASSED_INTERVIEW_AVAILABILITY_REQUEST -> assertThat(visibleText)
                .contains("서류 전형 합격을 안내드립니다.")
                .contains("서류 전형에 합격하셨습니다.")
                .contains("다음 전형을 위해 면접 가능 시간을 제출해 주세요.")
                .contains("홍길동")
                .contains("채용 담당자 recruit@university.neordinary.com");
            case RECRUITMENT_INTERVIEW_CONFIRMATION -> assertThat(visibleText)
                .contains("면접 일정이 확정되었습니다.")
                .contains("2026년 7월 20일")
                .contains("오후 2시 30분")
                .contains("온라인 화상 면접")
                .contains("채용 담당자 recruit@university.neordinary.com");
            case RECRUITMENT_FINAL_PASSED -> assertThat(visibleText)
                .contains("최종 합격을 축하드립니다.")
                .contains("Backend Track")
                .contains("합격 트랙");
            case RECRUITMENT_FINAL_FAILED -> assertThat(visibleText)
                .contains("최종 전형 결과를 안내드립니다.")
                .contains("신중한 검토 끝에 이번 전형에서는 함께하지 못하게 되었습니다.")
                .contains("앞으로의 여정에 좋은 기회가 함께하기를 응원합니다.")
                .doesNotContain("판정 사유")
                .doesNotContain("불합격 사유");
        }
    }

    private Map<String, String> values(EmailTemplateType type) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("applicantName", "홍길동");
        switch (type) {
            case RECRUITMENT_DOCUMENT_PASSED_INTERVIEW_AVAILABILITY_REQUEST -> {
                values.put("contactSnapshot", "채용 담당자 recruit@university.neordinary.com");
                values.put("actionUrl", ACTION_URL);
            }
            case RECRUITMENT_INTERVIEW_CONFIRMATION -> {
                values.put("interviewDate", "2026년 7월 20일");
                values.put("interviewTime", "오후 2시 30분");
                values.put("location", "온라인 화상 면접");
                values.put("contactSnapshot", "채용 담당자 recruit@university.neordinary.com");
            }
            case RECRUITMENT_FINAL_PASSED -> values.put("acceptedTrack", "Backend Track");
            case RECRUITMENT_FINAL_FAILED -> {
            }
        }
        return values;
    }

    private int count(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
