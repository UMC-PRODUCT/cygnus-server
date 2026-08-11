package com.umc.product.organization.adapter.in.graphql;

import org.springframework.stereotype.Component;

import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.graphql.relay.NodeFetcher;
import com.umc.product.global.graphql.relay.RelayNode;
import com.umc.product.organization.adapter.in.graphql.dto.ChapterGraphQlResponse;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * {@code node(id:)}의 Chapter 타입 재조회. {@code chapter(id:)} 쿼리와 동일하게 권한 검증 없이 조회한다.
 */
@Component
@RequiredArgsConstructor
public class ChapterNodeFetcher implements NodeFetcher {

    private final GetChapterUseCase getChapterUseCase;

    @Override
    public String typeName() {
        return GlobalIdTypes.CHAPTER;
    }

    @Override
    public RelayNode fetchOrNull(long rawId) {
        try {
            return ChapterGraphQlResponse.from(getChapterUseCase.getChapterById(rawId));
        } catch (OrganizationDomainException exception) {
            if (exception.getBaseCode() == OrganizationErrorCode.CHAPTER_NOT_FOUND) {
                return null;
            }
            throw exception;
        }
    }
}
