-- 商户分享页与小程序分享卡片配置。
ALTER TABLE store_operation_setting
    ADD COLUMN share_title VARCHAR(64) NULL COMMENT '分享页标题' AFTER sms_notify_phones,
    ADD COLUMN share_subtitle VARCHAR(128) NULL COMMENT '分享页副标题' AFTER share_title,
    ADD COLUMN share_brand VARCHAR(64) NULL COMMENT '分享页品牌文案' AFTER share_subtitle,
    ADD COLUMN share_image_url VARCHAR(512) NULL COMMENT '分享海报图片地址' AFTER share_brand;
