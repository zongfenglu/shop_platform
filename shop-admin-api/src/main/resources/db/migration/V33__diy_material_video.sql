-- Material library: support video uploads alongside images.
-- type: 'image' (default, all existing rows) / 'video'.
ALTER TABLE `diy_material`
    ADD COLUMN type VARCHAR(10) NOT NULL DEFAULT 'image' COMMENT 'image / video'
        AFTER url;
