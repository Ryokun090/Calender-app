-- ============================================================
-- 共有カレンダーアプリ DDL
-- MySQL 8.0以降を想定(CHECK制約が実際に効くバージョン)
-- ============================================================

CREATE TABLE users (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  name              VARCHAR(50)  NOT NULL,
  email             VARCHAR(255) NOT NULL UNIQUE,
  password_hash     VARCHAR(255) NOT NULL,
  is_2fa_enabled    BOOLEAN      NOT NULL DEFAULT FALSE,  -- ユーザーごとにON/OFFを選べる仕様
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE password_reset_tokens (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id         BIGINT       NOT NULL,
  token           VARCHAR(64)  NOT NULL UNIQUE,   -- URLに載せるランダム文字列(6桁コードではなく長めのトークン)
  expires_at      DATETIME     NOT NULL,          -- 発行から30分程度を想定
  is_used         BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE otp_codes (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id         BIGINT      NOT NULL,
  code            VARCHAR(6)  NOT NULL,        -- 6桁の数字コード
  expires_at      DATETIME    NOT NULL,        -- 発行から5分程度を想定
  is_used         BOOLEAN     NOT NULL DEFAULT FALSE,
  created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_otp_user (user_id, is_used)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE calendars (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  name            VARCHAR(100) NOT NULL,
  owner_id        BIGINT       NOT NULL,
  is_personal     BOOLEAN      NOT NULL DEFAULT FALSE,  -- 個人用カレンダーは削除・退出不可にする印
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE calendar_members (
  calendar_id     BIGINT NOT NULL,
  user_id         BIGINT NOT NULL,
  role            ENUM('owner', 'member') NOT NULL DEFAULT 'member',
  status          ENUM('pending', 'accepted') NOT NULL DEFAULT 'pending',
  joined_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (calendar_id, user_id),
  FOREIGN KEY (calendar_id) REFERENCES calendars(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id)     REFERENCES users(id)     ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE invite_codes (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  calendar_id     BIGINT      NOT NULL,
  code            VARCHAR(12) NOT NULL UNIQUE,
  created_by      BIGINT      NOT NULL,
  expires_at      DATETIME    NOT NULL,
  created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (calendar_id) REFERENCES calendars(id) ON DELETE CASCADE,
  FOREIGN KEY (created_by)  REFERENCES users(id)     ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE schedules (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  calendar_id       BIGINT       NOT NULL,
  google_event_id   VARCHAR(255) NULL,       -- Google Calendar連携(オプション機能)を使った場合だけ入る
  title             VARCHAR(100) NOT NULL,
  description       TEXT         NULL,
  location          VARCHAR(200) NULL,
  start_time        DATETIME     NOT NULL,
  end_time          DATETIME     NOT NULL,
  created_by        BIGINT       NOT NULL,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (calendar_id) REFERENCES calendars(id) ON DELETE CASCADE,
  FOREIGN KEY (created_by)  REFERENCES users(id)     ON DELETE CASCADE,
  CONSTRAINT chk_schedule_time CHECK (end_time > start_time),
  INDEX idx_schedules_calendar_range (calendar_id, start_time, end_time)  -- 日ごと・空き日程の範囲検索用
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 空き日程調整(調整さん型)
-- 候補日程を複数出し、メンバーが○×で回答、確定したらschedulesに1件作られる
-- ============================================================

CREATE TABLE schedule_polls (
  id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
  calendar_id           BIGINT       NOT NULL,
  title                 VARCHAR(100) NOT NULL,
  location              VARCHAR(200) NULL,
  description           TEXT         NULL,
  status                ENUM('open', 'confirmed') NOT NULL DEFAULT 'open',
  confirmed_schedule_id BIGINT       NULL,     -- 確定後、対応するschedulesの行を指す
  created_by            BIGINT       NOT NULL,
  created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (calendar_id) REFERENCES calendars(id) ON DELETE CASCADE,
  FOREIGN KEY (created_by)  REFERENCES users(id)     ON DELETE CASCADE,
  FOREIGN KEY (confirmed_schedule_id) REFERENCES schedules(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE poll_candidates (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  poll_id         BIGINT   NOT NULL,
  start_time      DATETIME NOT NULL,
  end_time        DATETIME NOT NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (poll_id) REFERENCES schedule_polls(id) ON DELETE CASCADE,
  CONSTRAINT chk_candidate_time CHECK (end_time > start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE poll_responses (
  poll_candidate_id   BIGINT  NOT NULL,
  user_id             BIGINT  NOT NULL,
  is_available        BOOLEAN NOT NULL,
  responded_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (poll_candidate_id, user_id),
  FOREIGN KEY (poll_candidate_id) REFERENCES poll_candidates(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id)           REFERENCES users(id)           ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE comments (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  schedule_id     BIGINT      NOT NULL,
  user_id         BIGINT      NOT NULL,
  content         VARCHAR(500) NOT NULL,
  created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (schedule_id) REFERENCES schedules(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id)     REFERENCES users(id)     ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notifications (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id         BIGINT       NOT NULL,
  schedule_id     BIGINT       NULL,          -- 予定が削除されても通知履歴は残す(SET NULL)
  type            VARCHAR(30)  NOT NULL,       -- 'comment' / 'invite_request' / 'schedule_created' / 'poll_confirmed' / 'reminder' など
  message         VARCHAR(255) NOT NULL,
  is_read         BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id)     REFERENCES users(id)     ON DELETE CASCADE,
  FOREIGN KEY (schedule_id) REFERENCES schedules(id) ON DELETE SET NULL,
  INDEX idx_notifications_user (user_id, is_read)   -- 未読バッジ集計用
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
