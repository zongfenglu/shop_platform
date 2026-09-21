-- Merchant map provider settings used by the platform map picker.
ALTER TABLE store_operation_setting
    ADD COLUMN map_provider VARCHAR(24) NOT NULL DEFAULT 'amap' COMMENT 'map provider' AFTER share_image_url,
    ADD COLUMN map_api_key VARCHAR(256) NULL COMMENT 'map web api key' AFTER map_provider;
