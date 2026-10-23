const { BusinessError } = require("./errors");
const { drawFiveCards } = require("./drawLogic");
const { businessDate } = require("./time");
const { RARITIES } = require("./cardPool");

class CardDrawService {
  constructor(dbPool, poolConfig) {
    this.dbPool = dbPool;
    this.poolConfig = poolConfig;
  }

  async activity(userId) {
    assertUserId(userId);
    const conn = await this.dbPool.getConnection();
    try {
      const quota = await this.currentQuota(conn, userId, false);
      const totalDrawCount = await this.countOrders(conn, userId);
      return {
        activityId: 1,
        activityName: this.poolConfig.pool.name,
        poolId: 1,
        poolVersion: this.poolConfig.pool.version,
        quota,
        totalDrawCount,
        buttonText: quota.availableQuota > 0 ? "抽卡" : "抽卡",
        tipText: quota.availableQuota > 0 ? "每日次数可用" : "次数已用尽，明天再来吧"
      };
    } finally {
      conn.release();
    }
  }

  async draw(userId, requestId) {
    assertUserId(userId);
    const normalizedRequestId = normalizeRequestId(requestId);
    const conn = await this.dbPool.getConnection();
    try {
      await conn.beginTransaction();

      if (normalizedRequestId) {
        const existing = await this.findOrderByRequestId(conn, userId, normalizedRequestId);
        if (existing) {
          const quota = await this.currentQuota(conn, userId, true);
          const response = await this.responseFromExistingOrder(conn, userId, existing, quota);
          await conn.commit();
          return response;
        }
      }

      const lockedQuota = await this.currentQuota(conn, userId, true);
      if (normalizedRequestId) {
        const existing = await this.findOrderByRequestId(conn, userId, normalizedRequestId);
        if (existing) {
          const response = await this.responseFromExistingOrder(conn, userId, existing, lockedQuota);
          await conn.commit();
          return response;
        }
      }
      if (lockedQuota.availableQuota <= 0) {
        throw new BusinessError("次数已用尽，明天再来吧");
      }

      await conn.execute(
        "UPDATE user_draw_quota SET used_today = used_today + 1 WHERE user_id = :userId AND quota_date = :quotaDate",
        { userId, quotaDate: lockedQuota.quotaDate }
      );
      const quotaAfterConsume = {
        ...lockedQuota,
        usedToday: lockedQuota.usedToday + 1,
        availableQuota: lockedQuota.availableQuota - 1
      };

      const ownedCards = await this.findUserCards(conn, userId);
      const ownedCardIds = new Set(ownedCards.map((row) => Number(row.card_id)));
      const forceNew = await this.needsConsecutiveNoNewProtection(conn, userId);
      const drawnCards = drawFiveCards(this.poolConfig, ownedCardIds, { forceNew });
      const hasNewCard = drawnCards.some((drawn) => drawn.isNewCard);
      const drawNo = (await this.countOrders(conn, userId)) + 1;

      const [orderResult] = await conn.execute(
        "INSERT INTO draw_order (user_id, pool_version, request_id, draw_no, cost_type, has_new_card) VALUES (:userId, :poolVersion, :requestId, :drawNo, 'FREE', :hasNewCard)",
        {
          userId,
          poolVersion: this.poolConfig.pool.version,
          requestId: normalizedRequestId,
          drawNo,
          hasNewCard: hasNewCard ? 1 : 0
        }
      );
      const orderId = Number(orderResult.insertId);

      const resultCards = [];
      for (const drawn of drawnCards) {
        const saved = await this.incrementUserCard(conn, userId, drawn.card);
        await conn.execute(
          "INSERT INTO draw_order_card (order_id, user_id, slot_no, card_id, rarity, is_new) VALUES (:orderId, :userId, :slotNo, :cardId, :rarity, :isNew)",
          {
            orderId,
            userId,
            slotNo: drawn.slotNo,
            cardId: drawn.card.cardId,
            rarity: drawn.card.rarity,
            isNew: saved.isNew ? 1 : 0
          }
        );
        resultCards.push(toDrawCardResult(drawn.slotNo, drawn.card, saved.isNew, saved.ownedCount));
      }

      await conn.commit();
      return {
        orderId,
        hasNewCard,
        cards: resultCards,
        quota: quotaAfterConsume,
        totalDrawCount: drawNo
      };
    } catch (error) {
      await conn.rollback();
      throw error;
    } finally {
      conn.release();
    }
  }

  async myCards(userId) {
    assertUserId(userId);
    const conn = await this.dbPool.getConnection();
    try {
      const userCards = await this.findUserCards(conn, userId);
      const ownedMap = new Map(userCards.map((row) => [Number(row.card_id), Number(row.owned_count)]));
      const cards = this.poolConfig.indexes.onlineCards.map((card) => ({
        cardId: card.cardId,
        word: card.word,
        meaning: card.meaning,
        arabicMeaning: card.arabicMeaning,
        audioUrl: card.audioUrl,
        imageUrl: card.imageUrl,
        rarity: card.rarity,
        ownedCount: ownedMap.get(card.cardId) || 0,
        isNew: false
      }));
      const rarityProgress = {};
      for (const rarity of RARITIES) {
        const total = this.poolConfig.indexes.onlineCards.filter((card) => card.rarity === rarity).length;
        const obtained = this.poolConfig.indexes.onlineCards.filter((card) => card.rarity === rarity && ownedMap.has(card.cardId)).length;
        rarityProgress[rarity] = {
          obtained,
          total,
          rate: total === 0 ? 0 : Math.floor((obtained * 100) / total)
        };
      }
      return {
        userId,
        obtainedCardCount: ownedMap.size,
        totalCardCount: this.poolConfig.indexes.onlineCards.length,
        rarityProgress,
        cards
      };
    } finally {
      conn.release();
    }
  }

  async history(userId) {
    assertUserId(userId);
    const conn = await this.dbPool.getConnection();
    try {
      const [orders] = await conn.execute(
        "SELECT id, draw_no, cost_type, has_new_card, created_at FROM draw_order WHERE user_id = :userId AND pool_version = :poolVersion ORDER BY created_at DESC, id DESC LIMIT 20",
        { userId, poolVersion: this.poolConfig.pool.version }
      );
      if (orders.length === 0) {
        return [];
      }
      const orderIds = orders.map((order) => Number(order.id));
      const placeholders = orderIds.map((_, index) => `:id${index}`).join(",");
      const params = Object.fromEntries(orderIds.map((id, index) => [`id${index}`, id]));
      const [rows] = await conn.execute(
        `SELECT order_id, slot_no, card_id, is_new FROM draw_order_card WHERE order_id IN (${placeholders}) ORDER BY slot_no ASC`,
        params
      );
      const cardsByOrder = new Map();
      for (const row of rows) {
        const orderId = Number(row.order_id);
        if (!cardsByOrder.has(orderId)) {
          cardsByOrder.set(orderId, []);
        }
        const card = this.poolConfig.indexes.cardsById.get(Number(row.card_id));
        if (card) {
          cardsByOrder.get(orderId).push(toDrawCardResult(Number(row.slot_no), card, Boolean(row.is_new), null));
        }
      }
      return orders.map((order) => ({
        orderId: Number(order.id),
        drawNo: Number(order.draw_no),
        costType: order.cost_type,
        hasNewCard: Boolean(order.has_new_card),
        createdAt: toIsoDateTime(order.created_at),
        cards: cardsByOrder.get(Number(order.id)) || []
      }));
    } finally {
      conn.release();
    }
  }

  async simulateNextDay(userId) {
    assertUserId(userId);
    const conn = await this.dbPool.getConnection();
    try {
      const quotaDate = businessDate(this.poolConfig.quota.businessResetHour);
      await conn.execute(
        "INSERT INTO user_draw_quota (user_id, quota_date, free_quota, extra_quota, used_today) VALUES (:userId, :quotaDate, :freeQuota, 0, 0) ON DUPLICATE KEY UPDATE free_quota = VALUES(free_quota), extra_quota = 0, used_today = 0",
        {
          userId,
          quotaDate,
          freeQuota: this.poolConfig.quota.dailyFreeQuota
        }
      );
      return this.currentQuota(conn, userId, false);
    } finally {
      conn.release();
    }
  }

  async health() {
    const conn = await this.dbPool.getConnection();
    try {
      await conn.query("SELECT 1");
      return {
        status: "ok",
        poolVersion: this.poolConfig.pool.version
      };
    } finally {
      conn.release();
    }
  }

  async currentQuota(conn, userId, forUpdate) {
    const quotaDate = businessDate(this.poolConfig.quota.businessResetHour);
    await conn.execute(
      "INSERT INTO user_draw_quota (user_id, quota_date, free_quota, extra_quota, used_today) VALUES (:userId, :quotaDate, :freeQuota, 0, 0) ON DUPLICATE KEY UPDATE user_id = user_id",
      {
        userId,
        quotaDate,
        freeQuota: this.poolConfig.quota.dailyFreeQuota
      }
    );
    const [rows] = await conn.execute(
      `SELECT user_id, quota_date, free_quota, extra_quota, used_today FROM user_draw_quota WHERE user_id = :userId AND quota_date = :quotaDate ${forUpdate ? "FOR UPDATE" : ""}`,
      { userId, quotaDate }
    );
    if (rows.length === 0) {
      throw new BusinessError("次数初始化失败");
    }
    return toQuotaDto(rows[0]);
  }

  async findOrderByRequestId(conn, userId, requestId) {
    const [rows] = await conn.execute(
      "SELECT id, user_id, pool_version, request_id, draw_no, cost_type, has_new_card, created_at FROM draw_order WHERE user_id = :userId AND request_id = :requestId LIMIT 1",
      { userId, requestId }
    );
    return rows[0] || null;
  }

  async responseFromExistingOrder(conn, userId, order, quota) {
    const [rows] = await conn.execute(
      "SELECT slot_no, card_id, is_new FROM draw_order_card WHERE order_id = :orderId ORDER BY slot_no ASC",
      { orderId: order.id }
    );
    const userCards = await this.findUserCards(conn, userId);
    const ownedCountByCardId = new Map(userCards.map((row) => [Number(row.card_id), Number(row.owned_count)]));
    const cards = rows
      .map((row) => {
        const card = this.poolConfig.indexes.cardsById.get(Number(row.card_id));
        return card ? toDrawCardResult(Number(row.slot_no), card, Boolean(row.is_new), ownedCountByCardId.get(card.cardId) || null) : null;
      })
      .filter(Boolean);
    return {
      orderId: Number(order.id),
      hasNewCard: Boolean(order.has_new_card),
      cards,
      quota,
      totalDrawCount: await this.countOrders(conn, userId)
    };
  }

  async findUserCards(conn, userId) {
    const [rows] = await conn.execute(
      "SELECT card_id, rarity, owned_count FROM user_card WHERE user_id = :userId",
      { userId }
    );
    return rows;
  }

  async incrementUserCard(conn, userId, card) {
    const [insertResult] = await conn.execute(
      "INSERT IGNORE INTO user_card (user_id, card_id, rarity, owned_count) VALUES (:userId, :cardId, :rarity, 1)",
      {
        userId,
        cardId: card.cardId,
        rarity: card.rarity
      }
    );
    const isNew = insertResult.affectedRows === 1;
    if (!isNew) {
      await conn.execute(
        "UPDATE user_card SET owned_count = owned_count + 1, rarity = :rarity WHERE user_id = :userId AND card_id = :cardId",
        {
          userId,
          cardId: card.cardId,
          rarity: card.rarity
        }
      );
    }
    const [rows] = await conn.execute(
      "SELECT owned_count FROM user_card WHERE user_id = :userId AND card_id = :cardId",
      {
        userId,
        cardId: card.cardId
      }
    );
    if (rows.length === 0) {
      throw new BusinessError("卡册更新失败");
    }
    return {
      isNew,
      ownedCount: Number(rows[0].owned_count)
    };
  }

  async countOrders(conn, userId) {
    const [rows] = await conn.execute(
      "SELECT COUNT(*) AS total FROM draw_order WHERE user_id = :userId",
      { userId }
    );
    return Number(rows[0].total);
  }

  async needsConsecutiveNoNewProtection(conn, userId) {
    const [rows] = await conn.execute(
      "SELECT has_new_card FROM draw_order WHERE user_id = :userId AND pool_version = :poolVersion ORDER BY created_at DESC, id DESC LIMIT 2",
      {
        userId,
        poolVersion: this.poolConfig.pool.version
      }
    );
    return rows.length >= 2 && rows.every((row) => !Boolean(row.has_new_card));
  }
}

function assertUserId(userId) {
  if (!userId || String(userId).trim().length === 0) {
    throw new BusinessError("userId 不能为空");
  }
  if (String(userId).length > 64) {
    throw new BusinessError("userId 长度不能超过 64");
  }
}

function normalizeRequestId(requestId) {
  if (requestId === undefined || requestId === null || String(requestId).trim() === "") {
    return null;
  }
  const normalized = String(requestId).trim();
  if (normalized.length > 64) {
    throw new BusinessError("requestId 长度不能超过 64");
  }
  return normalized;
}

function toQuotaDto(row) {
  const freeQuota = Number(row.free_quota);
  const extraQuota = Number(row.extra_quota);
  const usedToday = Number(row.used_today);
  return {
    freeQuota,
    extraQuota,
    availableQuota: Math.max(0, freeQuota + extraQuota - usedToday),
    usedToday,
    quotaDate: normalizeDate(row.quota_date)
  };
}

function toDrawCardResult(slotNo, card, isNew, ownedCount) {
  return {
    slotNo,
    cardId: card.cardId,
    word: card.word,
    meaning: card.meaning,
    arabicMeaning: card.arabicMeaning,
    audioUrl: card.audioUrl,
    imageUrl: card.imageUrl,
    rarity: card.rarity,
    isNew,
    ownedCount
  };
}

function normalizeDate(value) {
  if (value instanceof Date) {
    return value.toISOString().slice(0, 10);
  }
  return String(value).slice(0, 10);
}

function toIsoDateTime(value) {
  if (value instanceof Date) {
    return value.toISOString();
  }
  return String(value);
}

module.exports = {
  CardDrawService,
  normalizeRequestId,
  toQuotaDto
};
