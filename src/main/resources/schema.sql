-- 校园一卡通消费与结算系统 建表脚本（MySQL 8.0 / InnoDB / utf8mb4）
-- 说明：所有金额一律使用 DECIMAL(12,2)，禁止使用浮点类型，避免精度丢失。

CREATE TABLE IF NOT EXISTS card (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    card_no      VARCHAR(32)  NOT NULL COMMENT '卡号（业务主键）',
    student_no   VARCHAR(32)  DEFAULT NULL COMMENT '学号/工号',
    holder_name  VARCHAR(64)  NOT NULL COMMENT '持卡人姓名',
    card_type    VARCHAR(16)  NOT NULL DEFAULT 'STUDENT' COMMENT 'STUDENT/TEACHER',
    status       VARCHAR(16)  NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/LOST/FROZEN/CANCELLED',
    pin          VARCHAR(64)  DEFAULT NULL COMMENT '交易密码',
    version      INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_card_no (card_no),
    KEY idx_card_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '校园卡';

CREATE TABLE IF NOT EXISTS account (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    card_id       BIGINT        NOT NULL COMMENT '关联 card.id',
    balance       DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
    points        INT           NOT NULL DEFAULT 0 COMMENT '可用积分',
    frozen_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '冻结金额',
    version       INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_card (card_id),
    CONSTRAINT fk_account_card FOREIGN KEY (card_id) REFERENCES card (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '一卡通账户';

CREATE TABLE IF NOT EXISTS terminal (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    terminal_no   VARCHAR(32) NOT NULL COMMENT '终端编号',
    terminal_type VARCHAR(16) NOT NULL COMMENT 'POS/SUPERMARKET/ACCESS/WATER',
    location      VARCHAR(64) DEFAULT NULL COMMENT '安装位置',
    vendor        VARCHAR(32) DEFAULT NULL COMMENT '厂商',
    secret        VARCHAR(64) DEFAULT NULL COMMENT '接入密钥',
    status        VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_terminal_no (terminal_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '消费终端';

CREATE TABLE IF NOT EXISTS transaction (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    tx_no            VARCHAR(32)   NOT NULL COMMENT '交易流水号',
    card_id          BIGINT        NOT NULL,
    card_no          VARCHAR(32)   NOT NULL,
    terminal_id      BIGINT        DEFAULT NULL,
    terminal_no      VARCHAR(32)   DEFAULT NULL,
    tx_type          VARCHAR(16)   NOT NULL COMMENT 'RECHARGE/CONSUME/REFUND',
    amount           DECIMAL(12,2) NOT NULL COMMENT '原始金额',
    discount_amount  DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    actual_amount    DECIMAL(12,2) NOT NULL COMMENT '实付/实收金额',
    strategy_type    VARCHAR(24)   DEFAULT NULL COMMENT '命中的优惠策略',
    status           VARCHAR(16)   NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS/FAILED/REVERSED',
    idempotency_key  VARCHAR(64)   DEFAULT NULL COMMENT '幂等键（防重复消费）',
    original_tx_no   VARCHAR(32)   DEFAULT NULL COMMENT '退款对应的原交易号',
    remark           VARCHAR(255)  DEFAULT NULL,
    operator         VARCHAR(32)   DEFAULT NULL,
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    settled_at       DATETIME      DEFAULT NULL COMMENT '结算时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tx_no (tx_no),
    UNIQUE KEY uk_idempotency_key (idempotency_key),
    KEY idx_tx_card_time (card_id, created_at),
    KEY idx_tx_terminal_time (terminal_id, created_at),
    KEY idx_tx_type_status (tx_type, status),
    CONSTRAINT fk_tx_card FOREIGN KEY (card_id) REFERENCES card (id),
    CONSTRAINT fk_tx_terminal FOREIGN KEY (terminal_id) REFERENCES terminal (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '交易主表';

CREATE TABLE IF NOT EXISTS transaction_item (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    tx_id      BIGINT        NOT NULL,
    tx_no      VARCHAR(32)   NOT NULL,
    item_name  VARCHAR(64)   NOT NULL COMMENT '品名/消费项目',
    quantity   INT           NOT NULL DEFAULT 1,
    unit_price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    amount     DECIMAL(12,2) NOT NULL COMMENT '小计',
    PRIMARY KEY (id),
    KEY idx_item_tx (tx_id),
    CONSTRAINT fk_item_tx FOREIGN KEY (tx_id) REFERENCES transaction (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '交易明细（由建造者模式组装）';

CREATE TABLE IF NOT EXISTS reconcile_record (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    terminal_id  BIGINT        NOT NULL,
    settle_date  DATE          NOT NULL COMMENT '结算日期',
    tx_count     INT           NOT NULL DEFAULT 0 COMMENT '交易笔数',
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '合计金额',
    status       VARCHAR(16)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/BALANCED/DIFF',
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reconcile (terminal_id, settle_date),
    CONSTRAINT fk_reconcile_terminal FOREIGN KEY (terminal_id) REFERENCES terminal (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '终端对账记录';

CREATE TABLE IF NOT EXISTS notify_log (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    tx_no      VARCHAR(32) NOT NULL,
    channel    VARCHAR(16) NOT NULL COMMENT 'PUSH/AUDIT',
    content    VARCHAR(255) DEFAULT NULL,
    status     VARCHAR(16) NOT NULL DEFAULT 'SENT',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_notify_tx (tx_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '消息通知与操作审计日志';

CREATE TABLE IF NOT EXISTS points_log (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    card_id       BIGINT      NOT NULL,
    tx_no         VARCHAR(32) NOT NULL,
    points_change INT         NOT NULL COMMENT '积分变动值',
    balance_after INT         NOT NULL COMMENT '变动后积分',
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_points_card (card_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '积分流水';
