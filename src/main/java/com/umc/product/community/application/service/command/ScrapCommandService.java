package com.umc.product.community.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.community.application.port.in.command.post.ToggleScrapUseCase;
import com.umc.product.community.application.port.out.post.LoadPostPort;
import com.umc.product.community.application.port.out.scrap.LoadScrapPort;
import com.umc.product.community.application.port.out.scrap.SaveScrapPort;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ScrapCommandService implements ToggleScrapUseCase {

    private final LoadPostPort loadPostPort;
    private final LoadScrapPort loadScrapPort;
    private final SaveScrapPort saveScrapPort;

    @Override
    public ScrapResult toggleScrap(Long postId, Long challengerId) {
        loadPostPort.findById(postId)
            .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.POST_NOT_FOUND));

        boolean scrapped = saveScrapPort.toggleScrap(postId, challengerId);
        int scrapCount = loadScrapPort.countByPostId(postId);

        return new ScrapResult(scrapped, scrapCount);
    }
}
