-- Persist map coordinates for consumer, order snapshot and return addresses.
-- offline_store already has longitude/latitude columns (V20).
ALTER TABLE user_address
    ADD COLUMN longitude DECIMAL(10, 6) NULL COMMENT 'map longitude' AFTER detail,
    ADD COLUMN latitude  DECIMAL(10, 6) NULL COMMENT 'map latitude' AFTER longitude;

ALTER TABLE order_address
    ADD COLUMN longitude DECIMAL(10, 6) NULL COMMENT 'map longitude snapshot' AFTER detail,
    ADD COLUMN latitude  DECIMAL(10, 6) NULL COMMENT 'map latitude snapshot' AFTER longitude;

ALTER TABLE return_address
    ADD COLUMN longitude DECIMAL(10, 6) NULL COMMENT 'map longitude' AFTER detail,
    ADD COLUMN latitude  DECIMAL(10, 6) NULL COMMENT 'map latitude' AFTER longitude;
