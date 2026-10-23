CREATE TABLE card_pool (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    pack_image_url VARCHAR(512) NULL,
    display_sort_no INT NOT NULL DEFAULT 1,
    status VARCHAR(16) NOT NULL,
    start_time DATETIME(6) NULL,
    end_time DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_card_pool_version (version),
    KEY idx_card_pool_status_sort (status, display_sort_no, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE draw_activity (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    pool_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    start_time DATETIME(6) NULL,
    end_time DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_activity_pool_status (pool_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE card (
    id BIGINT NOT NULL AUTO_INCREMENT,
    word VARCHAR(64) NOT NULL,
    meaning VARCHAR(256) NULL,
    arabic_meaning VARCHAR(256) NULL,
    audio_url VARCHAR(512) NULL,
    image_url VARCHAR(512) NULL,
    rarity VARCHAR(16) NOT NULL,
    pool_version VARCHAR(32) NOT NULL,
    sort_no INT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_card_rarity_status (rarity, status),
    KEY idx_card_pool_version (pool_version),
    KEY idx_card_pool_rarity_status (pool_version, rarity, status),
    KEY idx_card_pool_sort (pool_version, status, sort_no, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE draw_slot_rule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    pool_id BIGINT NOT NULL,
    slot_no INT NOT NULL,
    rarity VARCHAR(16) NOT NULL,
    probability_basis_points INT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_slot_pool_slot (pool_id, slot_no),
    KEY idx_slot_pool_slot_rarity (pool_id, slot_no, rarity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE new_card_protection_rule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    min_collection_rate INT NOT NULL,
    max_collection_rate INT NOT NULL,
    new_card_weight_multiplier DOUBLE NOT NULL,
    PRIMARY KEY (id),
    KEY idx_new_card_protection_rate (min_collection_rate, max_collection_rate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_draw_quota (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    free_quota INT NOT NULL,
    extra_quota INT NOT NULL DEFAULT 0,
    used_today INT NOT NULL DEFAULT 0,
    course_reset_count_today INT NOT NULL DEFAULT 0,
    practice_extra_count_today INT NOT NULL DEFAULT 0,
    quota_date DATE NOT NULL,
    last_reset_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quota_user_date (user_id, quota_date),
    KEY idx_quota_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_card (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    card_id BIGINT NOT NULL,
    rarity VARCHAR(16) NOT NULL,
    owned_count INT NOT NULL DEFAULT 1,
    first_obtained_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_card (user_id, card_id),
    KEY idx_user_card_user (user_id),
    KEY idx_user_card_user_rarity (user_id, rarity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE draw_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    pool_id BIGINT NOT NULL,
    request_id VARCHAR(64) NULL,
    draw_no INT NOT NULL,
    cost_type VARCHAR(16) NOT NULL,
    has_new_card BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_user_request (user_id, request_id),
    KEY idx_order_user_time (user_id, created_at),
    KEY idx_order_user_pool_time (user_id, pool_id, created_at),
    KEY idx_order_pool_time (pool_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE draw_order_card (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    slot_no INT NOT NULL,
    card_id BIGINT NOT NULL,
    rarity VARCHAR(16) NOT NULL,
    is_new BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_order_card_order (order_id),
    KEY idx_order_card_user (user_id),
    KEY idx_order_card_card (card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
