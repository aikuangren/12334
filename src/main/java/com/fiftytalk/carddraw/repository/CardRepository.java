package com.fiftytalk.carddraw.repository;

import com.fiftytalk.carddraw.domain.CardStatus;
import com.fiftytalk.carddraw.domain.Rarity;
import com.fiftytalk.carddraw.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByPoolVersionAndStatusOrderBySortNoAscIdAsc(String poolVersion, CardStatus status);
    List<Card> findByPoolVersionAndRarityAndStatus(String poolVersion, Rarity rarity, CardStatus status);
    long countByPoolVersionAndStatus(String poolVersion, CardStatus status);
    long countByPoolVersionAndRarityAndStatus(String poolVersion, Rarity rarity, CardStatus status);
    long countByPoolVersion(String poolVersion);
    List<Card> findByIdIn(Collection<Long> ids);
}
