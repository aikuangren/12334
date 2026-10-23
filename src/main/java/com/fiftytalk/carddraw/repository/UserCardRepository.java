package com.fiftytalk.carddraw.repository;

import com.fiftytalk.carddraw.domain.Rarity;
import com.fiftytalk.carddraw.entity.UserCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserCardRepository extends JpaRepository<UserCard, Long> {
    Optional<UserCard> findByUserIdAndCardId(Long userId, Long cardId);
    List<UserCard> findByUserId(Long userId);
    List<UserCard> findByUserIdAndRarity(Long userId, Rarity rarity);
    long countByUserIdAndRarity(Long userId, Rarity rarity);

    @Query(value = """
            select count(*)
              from user_card user_card
              join card card on card.id = user_card.card_id
             where user_card.user_id = :userId
               and card.pool_version = :poolVersion
               and card.status = :status
            """, nativeQuery = true)
    long countOwnedOnlineCardsByUserIdAndPoolVersion(
            @Param("userId") Long userId,
            @Param("poolVersion") String poolVersion,
            @Param("status") String status
    );

    @Modifying
    @Query(value = """
            insert ignore into user_card (
                user_id,
                card_id,
                rarity,
                owned_count,
                first_obtained_at,
                updated_at
            ) values (
                :userId,
                :cardId,
                :rarity,
                1,
                current_timestamp,
                current_timestamp
            )
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId, @Param("cardId") Long cardId, @Param("rarity") String rarity);

    @Modifying
    @Query("""
            update UserCard userCard
               set userCard.ownedCount = userCard.ownedCount + 1,
                   userCard.updatedAt = current_timestamp
             where userCard.userId = :userId
               and userCard.cardId = :cardId
            """)
    int increaseOwnedCount(@Param("userId") Long userId, @Param("cardId") Long cardId);
}
