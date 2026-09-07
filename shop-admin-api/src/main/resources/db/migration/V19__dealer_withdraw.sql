-- Sprint 12: dealer withdrawal + settlement. See doc 03 sec 3.6.
-- dealer_withdraw: withdrawal application records (apply → store review → approve/reject).
-- Settlement job: DealerSettleJob runs periodically to move pending→settled commissions.

CREATE TABLE `dealer_withdraw` (
    id                  BIGINT UNSIGNED PRIMARY KEY,
    shop_id             BIGINT UNSIGNED NOT NULL,
    dealer_user_id      BIGINT UNSIGNED NOT NULL,
    user_id             BIGINT UNSIGNED NOT NULL,
    amount              DECIMAL(10,2)   NOT NULL COMMENT 'withdraw amount',
    method              VARCHAR(20)     NOT NULL DEFAULT 'wechat' COMMENT 'wechat/alipay/bank',
    account_info        VARCHAR(255)    NULL COMMENT 'account identifier (masked)',
    status              VARCHAR(10)     NOT NULL DEFAULT 'applying' COMMENT 'applying/approved/rejected/paid',
    remark              VARCHAR(255)    NULL,
    apply_time          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    review_time         DATETIME        NULL,
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete           TINYINT         NOT NULL DEFAULT 0,
    KEY idx_shop_dealer (shop_id, dealer_user_id),
    KEY idx_shop_status (shop_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='dealer withdrawal applications';
