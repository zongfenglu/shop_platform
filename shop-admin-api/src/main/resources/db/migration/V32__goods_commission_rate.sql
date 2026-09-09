-- Per-goods commission rate for dealer_setting.commission_type = 'goods'.
-- NULL = fall back to the shop-level dealer_setting.commission_rate.
-- Stored on goods (not sku): commission differentiation is a merchandising decision
-- made per product, and the order line already snapshots goods_id.
ALTER TABLE `goods`
    ADD COLUMN commission_rate DECIMAL(5,2) NULL DEFAULT NULL
        COMMENT 'per-goods dealer commission rate in percent; NULL = use shop default'
        AFTER limit_num;
