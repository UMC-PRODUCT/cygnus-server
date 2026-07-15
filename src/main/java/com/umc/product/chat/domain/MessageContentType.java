package com.umc.product.chat.domain;

/**
 * 채팅 메시지의 콘텐츠 종류.
 * <p>
 * 발신자 역할(운영진/문의자 등)이 아니라 "메시지 내용이 무엇인가"를 구분한다.
 * <ul>
 *     <li>{@code TEXT} — 일반 텍스트 메시지.</li>
 *     <li>{@code IMAGE} — 이미지 첨부 (content는 캡션, fileMetadataIds에 이미지 참조).</li>
 *     <li>{@code FILE} — 파일 첨부 (content는 캡션, fileMetadataIds에 파일 참조).</li>
 *     <li>{@code SYSTEM} — 시스템 자동 생성 메시지 (입장/퇴장 등, senderMemberId 없음).</li>
 * </ul>
 */
public enum MessageContentType {
    TEXT,
    IMAGE,
    FILE,
    SYSTEM
}
