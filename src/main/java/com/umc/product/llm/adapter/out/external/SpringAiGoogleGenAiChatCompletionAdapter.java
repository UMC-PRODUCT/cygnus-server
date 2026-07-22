package com.umc.product.llm.adapter.out.external;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.umc.product.llm.application.port.in.dto.ChatCompleteCommand;
import com.umc.product.llm.application.port.in.dto.ChatCompletionResult;
import com.umc.product.llm.application.port.out.ChatCompletionPort;
import com.umc.product.llm.domain.exception.LlmDomainException;
import com.umc.product.llm.domain.exception.LlmErrorCode;

import lombok.extern.slf4j.Slf4j;

/**
 * Spring AI ChatClient 위에 얹은 Google GenAI (Gemini Developer API) 어댑터.
 * <p>
 * application property 의 {@code app.llm.provider=google-genai} 일 때만 활성화된다. 인증은 Spring AI 의 google-genai 자동구성이 다음 두 경로 중
 * 하나로 해결한다.
 * <ul>
 *   <li>{@code spring.ai.google.genai.api-key} - Google AI Studio API key (무료 티어 검증/로컬 권장)</li>
 *   <li>{@code spring.ai.google.genai.vertex-ai=true} + project-id/location/credentials - Vertex AI 모드</li>
 * </ul>
 * <p>
 * 응답 파싱은 OpenAI 어댑터와 동일하게 {@link ChatPromptHelper} 를 재사용한다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "google-genai")
public class SpringAiGoogleGenAiChatCompletionAdapter implements ChatCompletionPort {

    private static final String PROVIDER_NAME = "google-genai";

    private final GoogleGenAiChatModel chatModel;
    private final LlmProperties properties;

    public SpringAiGoogleGenAiChatCompletionAdapter(GoogleGenAiChatModel chatModel, LlmProperties properties) {
        this.chatModel = chatModel;
        this.properties = properties;
    }

    @Override
    public ChatCompletionResult complete(ChatCompleteCommand command) {
        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
            .model(properties.model())
            .temperature(properties.temperature())
            .maxOutputTokens(ChatPromptHelper.resolveMaxOutputTokens(command, properties))
            .build();

        String systemPrompt = command.systemPrompt() == null ? "" : command.systemPrompt();
        String userPrompt = command.userPrompt() == null ? "" : command.userPrompt();

        try {
            var promptSpec = ChatClient.builder(chatModel)
                .build()
                .prompt();
            if (!systemPrompt.isBlank()) {
                promptSpec.system(systemPrompt);
            }
            if (!userPrompt.isBlank()) {
                promptSpec.user(userPrompt);
            }
            ChatResponse response = promptSpec
                .options(options)
                .call()
                .chatResponse();
            String content =
                response == null || response.getResult() == null || response.getResult().getOutput() == null
                    ? ""
                    : response.getResult().getOutput().getText();
            String normalized = ChatPromptHelper.normalizeResponse(content);
            Long promptTokens = ChatPromptHelper.extractPromptTokens(response);
            Long completionTokens = ChatPromptHelper.extractCompletionTokens(response);
            log.debug(
                "Google GenAI 호출을 완료했습니다: model={}, length={}, promptTokens={}, completionTokens={}",
                properties.model(), normalized.length(), promptTokens, completionTokens);
            return ChatCompletionResult.of(normalized, PROVIDER_NAME, promptTokens, completionTokens);
        } catch (Exception e) {
            log.warn("Google GenAI 호출 실패: model={}, error={}", properties.model(), e.toString(), e);
            throw new LlmDomainException(LlmErrorCode.CHAT_COMPLETION_FAILED, e.getMessage(), e);
        }
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }
}
