CREATE TABLE IF NOT EXISTS user_draw_quota (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id VARCHAR(64) NOT NULL,
  quota_date DATE NOT NULL,
  free_quota INT NOT NULL DEFAULT 0,
  extra_quota INT NOT NULL DEFAULT 0,
  used_today INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_quota_date (user_id, quota_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_card (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id VARCHAR(64) NOT NULL,
  card_id BIGINT NOT NULL,
  rarity VARCHAR(16) NOT NULL,
  owned_count INT NOT NULL DEFAULT 1,
  first_obtained_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_card (user_id, card_id),
  KEY idx_user_card_user (user_id),
  KEY idx_user_card_rarity (user_id, rarity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS draw_order (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id VARCHAR(64) NOT NULL,
  pool_version VARCHAR(32) NOT NULL,
  request_id VARCHAR(64) NULL,
  draw_no INT NOT NULL,
  cost_type VARCHAR(16) NOT NULL DEFAULT 'FREE',
  has_new_card TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_request (user_id, request_id),
  KEY idx_user_pool_time (user_id, pool_version, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS draw_order_card (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  slot_no INT NOT NULL,
  card_id BIGINT NOT NULL,
  rarity VARCHAR(16) NOT NULL,
  is_new TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_order_id (order_id),
  KEY idx_user_card (user_id, card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
