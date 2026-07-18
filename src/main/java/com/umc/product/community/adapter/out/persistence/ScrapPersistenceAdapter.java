package com.umc.product.community.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.community.application.port.out.scrap.LoadScrapPort;
import com.umc.product.community.application.port.out.scrap.SaveScrapPort;
import com.umc.product.community.domain.Post;
import com.umc.product.community.domain.Scrap;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ScrapPersistenceAdapter implements LoadScrapPort, SaveScrapPort {

    private final ScrapRepository scrapRepository;
    private final EntityManager entityManager;

    @Override
    public Optional<Scrap> findByPostIdAndChallengerId(Long postId, Long challengerId) {
        return scrapRepository.findByPost_IdAndChallengerId(postId, challengerId);
    }

    @Override
    public boolean existsByPostIdAndChallengerId(Long postId, Long challengerId) {
        return scrapRepository.existsByPost_IdAndChallengerId(postId, challengerId);
    }

    @Override
    public int countByPostId(Long postId) {
        return scrapRepository.countByPost_Id(postId);
    }

    @Override
    public Scrap save(Scrap scrap) {
        return scrapRepository.save(scrap);
    }

    @Override
    public void delete(Scrap scrap) {
        if (scrap.getId() != null) {
            scrapRepository.deleteById(scrap.getId());
        }
    }

    @Override
    @Transactional
    public void deleteByPostIdAndChallengerId(Long postId, Long challengerId) {
        scrapRepository.deleteByPost_IdAndChallengerId(postId, challengerId);
    }

    @Override
    @Transactional
    public boolean toggleScrap(Long postId, Long challengerId) {
        lockToggle(postId, challengerId);
        Optional<Scrap> existing = scrapRepository.findByPost_IdAndChallengerId(postId, challengerId);

        if (existing.isPresent()) {
            scrapRepository.delete(existing.get());
            return false;
        }

        Post post = entityManager.getReference(Post.class, postId);
        Scrap scrap = Scrap.create(post, challengerId);
        scrapRepository.save(scrap);
        return true;
    }

    @Override
    public void deleteByPostId(Long postId) {
        scrapRepository.deleteAllByPost_Id(postId);
    }

    private void lockToggle(Long postId, Long challengerId) {
        String scopeKey = "community-scrap|" + postId + '|' + challengerId;
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(:scopeKey, 0))")
            .setParameter("scopeKey", scopeKey)
            .getSingleResult();
    }
}
