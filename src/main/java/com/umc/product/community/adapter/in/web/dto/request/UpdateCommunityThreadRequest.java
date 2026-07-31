package com.umc.product.community.adapter.in.web.dto.request;

import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.umc.product.community.application.port.in.command.thread.dto.UpdateCommunityThreadCommand;
import com.umc.product.community.domain.enums.CommunityThreadCategory;

import jakarta.validation.constraints.AssertTrue;

public final class UpdateCommunityThreadRequest {

    private static final Pattern GRAPHEME = Pattern.compile("\\X");

    private boolean titlePresent;
    private boolean descriptionPresent;
    private boolean categoryPresent;
    private boolean iconPresent;

    private String title;
    private String description;
    private CommunityThreadCategory category;
    private String icon;

    @JsonSetter("title")
    public void setTitle(String title) {
        titlePresent = true;
        this.title = title;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        descriptionPresent = true;
        this.description = description;
    }

    @JsonSetter("category")
    public void setCategory(CommunityThreadCategory category) {
        categoryPresent = true;
        this.category = category;
    }

    @JsonSetter("icon")
    public void setIcon(String icon) {
        iconPresent = true;
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public CommunityThreadCategory getCategory() {
        return category;
    }

    public String getIcon() {
        return icon;
    }

    @JsonIgnore
    @AssertTrue(message = "수정할 필드를 하나 이상 제공하고 explicit null을 사용할 수 없습니다.") public boolean isValidPatch() {
        return hasPresentField()
            && validRequiredText(titlePresent, title, 80)
            && validOptionalText(descriptionPresent, description, 500)
            && (!categoryPresent || category != null)
            && validIcon();
    }

    public UpdateCommunityThreadCommand toCommand(Long threadId, Long actorMemberId) {
        return new UpdateCommunityThreadCommand(
            threadId,
            actorMemberId,
            titlePresent ? title : null,
            descriptionPresent ? description : null,
            descriptionPresent,
            categoryPresent ? category : null,
            iconPresent ? icon : null
        );
    }

    private boolean hasPresentField() {
        return titlePresent || descriptionPresent || categoryPresent || iconPresent;
    }

    private boolean validRequiredText(boolean present, String value, int maxCodePoints) {
        return !present || (value != null && !value.isBlank() && validLength(value, maxCodePoints));
    }

    private boolean validOptionalText(boolean present, String value, int maxCodePoints) {
        return !present || (value != null && validLength(value, maxCodePoints));
    }

    private boolean validIcon() {
        return !iconPresent
            || (icon != null
                && !icon.isBlank()
                && validLength(icon, 32)
                && GRAPHEME.matcher(icon).matches());
    }

    private boolean validLength(String value, int maxCodePoints) {
        String normalized = value.strip();
        return normalized.codePointCount(0, normalized.length()) <= maxCodePoints;
    }
}
