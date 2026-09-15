-- 微信小程序先按 open_id 静默注册/登录，手机号仅在用户主动授权后绑定。
ALTER TABLE `user`
    MODIFY COLUMN mobile VARCHAR(20) NULL,
    ADD UNIQUE KEY uk_shop_open_id (shop_id, open_id);
