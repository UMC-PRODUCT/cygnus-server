package com.umc.product.notice.application.service.command;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.form.application.port.in.command.ManageVoteUseCase;
import com.umc.product.form.application.port.in.command.dto.CreateVoteCommand;
import com.umc.product.notice.application.port.in.command.ManageNoticeContentUseCase;
import com.umc.product.notice.application.port.in.command.dto.AddNoticeImagesCommand;
import com.umc.product.notice.application.port.in.command.dto.AddNoticeLinksCommand;
import com.umc.product.notice.application.port.in.command.dto.AddNoticeVoteCommand;
import com.umc.product.notice.application.port.in.command.dto.AddNoticeVoteResult;
import com.umc.product.notice.application.port.in.command.dto.ReplaceNoticeImagesCommand;
import com.umc.product.notice.application.port.in.command.dto.ReplaceNoticeLinksCommand;
import com.umc.product.notice.application.port.out.LoadNoticeImagePort;
import com.umc.product.notice.application.port.out.LoadNoticeLinkPort;
import com.umc.product.notice.application.port.out.LoadNoticePort;
import com.umc.product.notice.application.port.out.LoadNoticeVotePort;
import com.umc.product.notice.application.port.out.SaveNoticeImagePort;
import com.umc.product.notice.application.port.out.SaveNoticeLinkPort;
import com.umc.product.notice.application.port.out.SaveNoticeVotePort;
import com.umc.product.notice.domain.Notice;
import com.umc.product.notice.domain.NoticeImage;
import com.umc.product.notice.domain.NoticeLink;
import com.umc.product.notice.domain.NoticeVote;
import com.umc.product.notice.domain.exception.NoticeDomainException;
import com.umc.product.notice.domain.exception.NoticeErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NoticeContentService implements ManageNoticeContentUseCase {
    private final LoadNoticeVotePort loadNoticeVotePort;
    private final LoadNoticeLinkPort loadNoticeLinkPort;
    private final LoadNoticeImagePort loadNoticeImagePort;
    private final SaveNoticeVotePort saveNoticeVotePort;
    private final SaveNoticeImagePort saveNoticeImagePort;
    private final SaveNoticeLinkPort saveNoticeLinkPort;
    private final LoadNoticePort loadNoticePort;

    private final ManageVoteUseCase manageVoteUseCase;

    @Override
    public AddNoticeVoteResult addVote(AddNoticeVoteCommand command, Long noticeId) {
        Notice notice = findNoticeById(noticeId);
        notice.validateAuthorMember(command.createdMemberId());

        if (loadNoticeVotePort.existsVoteByNoticeId(noticeId)) {
            throw new NoticeDomainException(NoticeErrorCode.VOTE_ALREADY_EXISTS);
        }

        Long voteId = manageVoteUseCase.createVote(
            CreateVoteCommand.builder()
                .createdMemberId(command.createdMemberId())
                .title(command.title())
                .isAnonymous(command.isAnonymous())
                .allowMultipleChoice(command.allowMultipleChoice())
                .options(command.options())
                .build()
        );

        NoticeVote noticeVote = NoticeVote.create(voteId, notice, command.startsAt(), command.endsAtExclusive());
        NoticeVote savedVote = saveNoticeVotePort.saveVote(noticeVote);

        return new AddNoticeVoteResult(savedVote.getId(), voteId);
    }

    @Override
    public List<Long> addImages(AddNoticeImagesCommand command, Long noticeId, Long memberId) {
        Notice notice = findNoticeById(noticeId);
        notice.validateAuthorMember(memberId);

        if (command.imageIds() == null || command.imageIds().isEmpty()) {
            throw new NoticeDomainException(NoticeErrorCode.IMAGE_URLS_REQUIRED);
        }

        /*
         * 공지 이미지 최대 개수는 10장으로 제한
         */
        int existing = loadNoticeImagePort.countImageByNoticeId(noticeId);
        int adding = command.imageIds().size();

        if (existing + adding > 10) {
            throw new NoticeDomainException(NoticeErrorCode.IMAGE_LIMIT_EXCEEDED);
        }

        AtomicInteger order = new AtomicInteger(loadNoticeImagePort.findNextImageDisplayOrder(noticeId));
        List<NoticeImage> images = command.imageIds().stream()
            .map(imgId -> NoticeImage.create(imgId, notice, order.getAndIncrement()))
            .toList();

        List<NoticeImage> savedImages = saveNoticeImagePort.saveAllImages(images);
        return savedImages.stream()
            .map(NoticeImage::getId)
            .toList();
    }

    @Override
    public List<Long> addLinks(AddNoticeLinksCommand command, Long noticeId, Long memberId) {
        Notice notice = findNoticeById(noticeId);
        notice.validateAuthorMember(memberId);

        if (command.links() == null || command.links().isEmpty()) {
            throw new NoticeDomainException(NoticeErrorCode.LINK_URLS_REQUIRED);
        }

        AtomicInteger order = new AtomicInteger(loadNoticeLinkPort.findNextLinkDisplayOrder(noticeId));
        List<NoticeLink> links = command.links().stream()
            .map(link -> NoticeLink.create(link, notice, order.getAndIncrement()))
            .toList();

        List<NoticeLink> savedLinks = saveNoticeLinkPort.saveAllLinks(links);
        return savedLinks.stream()
            .map(NoticeLink::getId)
            .toList();
    }

    @Override
    public void deleteVote(Long noticeId, Long memberId) {
        Notice notice = findNoticeById(noticeId);
        notice.validateAuthorMember(memberId);

        NoticeVote vote = loadNoticeVotePort.findVoteByNoticeId(noticeId)
            .orElseThrow(() -> new NoticeDomainException(NoticeErrorCode.NOTICE_VOTE_NOT_FOUND));

        saveNoticeVotePort.deleteVote(vote);
        manageVoteUseCase.deleteVote(vote.getVoteId());
    }

    @Override
    public void removeContentsByNoticeId(Long noticeId, Long memberId) {
        saveNoticeImagePort.deleteAllImagesByNoticeId(noticeId);
        saveNoticeLinkPort.deleteAllLinksByNoticeId(noticeId);

        loadNoticeVotePort.findVoteByNoticeId(noticeId)
            .ifPresent(vote -> {
                saveNoticeVotePort.deleteAllVotesByNoticeId(noticeId);
                manageVoteUseCase.deleteVote(vote.getVoteId());
            });
    }

    @Override
    public void replaceImages(ReplaceNoticeImagesCommand command, Long noticeId, Long memberId) {
        if (command.imageIds() == null) {
            return;
        }

        if (command.imageIds().size() > 10) {
            throw new NoticeDomainException(NoticeErrorCode.IMAGE_LIMIT_EXCEEDED);
        }

        Notice notice = findNoticeById(noticeId);
        notice.validateAuthorMember(memberId);

        saveNoticeImagePort.deleteAllImagesByNoticeId(noticeId);

        if (command.imageIds().isEmpty()) {
            return;
        }

        AtomicInteger order = new AtomicInteger(0);
        List<NoticeImage> images = command.imageIds().stream()
            .map(imageId -> NoticeImage.create(imageId, notice, order.getAndIncrement()))
            .toList();

        saveNoticeImagePort.saveAllImages(images);
    }

    @Override
    public void replaceLinks(ReplaceNoticeLinksCommand command, Long noticeId, Long memberId) {
        if (command.links() == null) {
            return;
        }

        Notice notice = findNoticeById(noticeId);
        notice.validateAuthorMember(memberId);
        saveNoticeLinkPort.deleteAllLinksByNoticeId(noticeId);

        if (command.links().isEmpty()) {
            return;
        }

        AtomicInteger order = new AtomicInteger(0);
        List<NoticeLink> links = command.links().stream()
            .map(link -> NoticeLink.create(link, notice, order.getAndIncrement()))
            .toList();

        saveNoticeLinkPort.saveAllLinks(links);
    }

    private Notice findNoticeById(Long noticeId) {
        return loadNoticePort.findNoticeById(noticeId)
            .orElseThrow(() -> new NoticeDomainException(NoticeErrorCode.NOTICE_NOT_FOUND));
    }

}
