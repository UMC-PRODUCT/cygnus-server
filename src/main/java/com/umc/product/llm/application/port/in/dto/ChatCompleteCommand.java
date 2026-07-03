package com.umc.product.llm.application.port.in.dto;

/**
 * LLM 호출 입력. 본 도메인은 generic chat completion 추상이며, system/user prompt 의 모든 의미는 호출자가 완성해 보낸다. LLM 도메인은
 * prompt 를 변형하지 않는다.
 *
 * @param systemPrompt            모델의 역할/제약을 지시하는 system 프롬프트 (없으면 null/empty)
 * @param userPrompt              실제 입력 본문
 * @param maxOutputTokensOverride 호출 단위로 응답 토큰 한도를 override 한다. null 이면
 *                                {@link com.umc.product.llm.adapter.out.external.LlmProperties#maxOutputTokens()} 를 사용.
 *                                batch JSON 응답처럼 응답이 길어지는 호출에서만 명시한다.
 */
public record ChatCompleteCommand(
    String systemPrompt,
    String userPrompt,
    Integer maxOutputTokensOverride
) {
    public static ChatCompleteCommand freeForm(String systemPrompt, String userPrompt) {
        return new ChatCompleteCommand(systemPrompt, userPrompt, null);
    }

    public static ChatCompleteCommand freeFormWithMaxTokens(String systemPrompt, String userPrompt,
                                                            int maxOutputTokens) {
        return new ChatCompleteCommand(systemPrompt, userPrompt, maxOutputTokens);
    }
}
