package com.umc.product.demoday.domain;

import com.umc.product.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "demoday_booth")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DemodayBooth extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demoday_vote_event_id", nullable = false)
    private DemodayVoteEvent voteEvent;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "display_name")
    private String displayName;
}
