package com.umc.product.llm.adapter.out.external;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.umc.product.llm.application.port.in.dto.ChatCompleteCommand;
import com.umc.product.llm.application.port.in.dto.ChatCompletionResult;
import com.umc.product.llm.application.port.out.ChatCompletionPort;
import com.umc.product.llm.domain.exception.LlmDomainException;
import com.umc.product.llm.domain.exception.LlmErrorCode;

import lombok.extern.slf4j.Slf4j;

/**
 * Spring AI ChatClient 위에 얹은 Vertex AI Gemini 어댑터.
 * <p>
 * application property 의 {@code app.llm.provider=vertexai-gemini} 일 때만 활성화된다. provider 키는 Spring AI 의
 * {@code spring.ai.model.chat=vertexai-gemini} 자동구성 키와 동일한 값으로 맞춰, 두 property 가 한 환경 변수로 일관되게 주입되도록 한다.
 * <p>
 * 응답 파싱은 OpenAI 어댑터와 동일하게 {@link ChatPromptHelper} 를 재사용한다. 인증 자격은 {@code GOOGLE_APPLICATION_CREDENTIALS} 환경 변수의
 * service account 또는 Spring AI 의 Vertex 자동구성이 인식하는 다른 자격 경로로 주입된다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "vertexai-gemini")
public class SpringAiGeminiChatCompletionAdapter implements ChatCompletionPort {

    private static final String PROVIDER_NAME = "vertexai-gemini";

    private final VertexAiGeminiChatModel chatModel;
    private final LlmProperties properties;

    public SpringAiGeminiChatCompletionAdapter(VertexAiGeminiChatModel chatModel, LlmProperties properties) {
        this.chatModel = chatModel;
        this.properties = properties;
    }

    @Override
    public ChatCompletionResult complete(ChatCompleteCommand command) {
        VertexAiGeminiChatOptions options = VertexAiGeminiChatOptions.builder()
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
            log.debug("Gemini 호출을 완료했습니다: model={}, length={}, promptTokens={}, completionTokens={}",
                properties.model(), normalized.length(), promptTokens, completionTokens);
            return ChatCompletionResult.of(normalized, PROVIDER_NAME, promptTokens, completionTokens);
        } catch (Exception e) {
            log.warn("Gemini 호출 실패: model={}, error={}", properties.model(), e.toString(), e);
            throw new LlmDomainException(LlmErrorCode.CHAT_COMPLETION_FAILED, e.getMessage(), e);
        }
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }
}
