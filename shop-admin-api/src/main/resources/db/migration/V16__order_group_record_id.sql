-- Sprint 10 拼团：在 order 上加 group_record_id，把每个拼团订单关联到它所属的 group_record。
-- GroupExpireJob 据此找到超时未成团的全部订单做关单/退款。
-- bargain 不需要此列：bargain_record.order_id 已反向记录下单订单。

ALTER TABLE `order` ADD COLUMN group_record_id BIGINT UNSIGNED NULL COMMENT '拼团记录 id（仅 activityType=group 时有值）' AFTER activity_id;
ALTER TABLE `order` ADD KEY idx_group_record (group_record_id);
