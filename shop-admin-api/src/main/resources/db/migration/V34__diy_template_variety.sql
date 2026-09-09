-- The four industry templates seeded in V21 were structurally near-identical
-- (search + banner + goods, only bgColor differed) and used a `data` key the
-- editor doesn't read (editor blocks use `images`/`items`). Merchants rightly
-- complained the "styles" looked the same. Re-seed with genuinely different
-- compositions using editor-compatible block shapes. Only non-package-locked
-- component types are used (no coupon/seckill/group/bargain).
UPDATE diy_template SET page_data = JSON_OBJECT(
    'page', JSON_OBJECT('name', '首页', 'bgColor', '#f8f8f8'),
    'items', JSON_ARRAY(
        JSON_OBJECT('type', 'search', 'placeholder', '搜索商品'),
        JSON_OBJECT('type', 'banner', 'images', JSON_ARRAY(JSON_OBJECT('url', '', 'link', '')), 'bgColor', '#f8f8f8'),
        JSON_OBJECT('type', 'navBar', 'items', JSON_ARRAY(
            JSON_OBJECT('icon', '', 'text', '新品', 'link', ''),
            JSON_OBJECT('icon', '', 'text', '上衣', 'link', ''),
            JSON_OBJECT('icon', '', 'text', '裤装', 'link', ''),
            JSON_OBJECT('icon', '', 'text', '鞋包', 'link', ''),
            JSON_OBJECT('icon', '', 'text', '配饰', 'link', '')), 'bgColor', '#ffffff'),
        JSON_OBJECT('type', 'blank', 'height', 12),
        JSON_OBJECT('type', 'goods', 'style', 'grid', 'goodsIds', JSON_ARRAY(), 'limit', 6,
                    'bgColor', '#f8f8f8', 'showName', true, 'showPrice', true, 'showLinePrice', true)
    ))
WHERE id = 1;

UPDATE diy_template SET page_data = JSON_OBJECT(
    'page', JSON_OBJECT('name', '首页', 'bgColor', '#eefaf3'),
    'items', JSON_ARRAY(
        JSON_OBJECT('type', 'notice', 'text', '今日现摘现发，下午 4 点前下单当日达', 'bgColor', '#e3f6ea'),
        JSON_OBJECT('type', 'banner', 'images', JSON_ARRAY(JSON_OBJECT('url', '', 'link', '')), 'bgColor', '#eefaf3'),
        JSON_OBJECT('type', 'imageWindow', 'images', JSON_ARRAY(
            JSON_OBJECT('url', '', 'link', ''), JSON_OBJECT('url', '', 'link', ''), JSON_OBJECT('url', '', 'link', '')), 'bgColor', '#ffffff'),
        JSON_OBJECT('type', 'goods', 'style', 'list', 'goodsIds', JSON_ARRAY(), 'limit', 4,
                    'bgColor', '#eefaf3', 'showName', true, 'showPrice', true, 'showLinePrice', false),
        JSON_OBJECT('type', 'news', 'label', '快报', 'items', JSON_ARRAY(JSON_OBJECT('title', '本周时令蔬果上新', 'link', '')), 'bgColor', '#ffffff')
    ))
WHERE id = 2;

UPDATE diy_template SET page_data = JSON_OBJECT(
    'page', JSON_OBJECT('name', '首页', 'bgColor', '#fdece9'),
    'items', JSON_ARRAY(
        JSON_OBJECT('type', 'banner', 'images', JSON_ARRAY(JSON_OBJECT('url', '', 'link', '')), 'bgColor', '#fdece9'),
        JSON_OBJECT('type', 'imageGroup', 'images', JSON_ARRAY(JSON_OBJECT('url', '', 'link', '')), 'bgColor', '#ffffff'),
        JSON_OBJECT('type', 'article', 'items', JSON_ARRAY(
            JSON_OBJECT('title', '换季护肤指南', 'cover', '', 'views', 0, 'link', '')), 'bgColor', '#ffffff'),
        JSON_OBJECT('type', 'goods', 'style', 'grid', 'goodsIds', JSON_ARRAY(), 'limit', 4,
                    'bgColor', '#fdece9', 'showName', true, 'showPrice', true, 'showLinePrice', true),
        JSON_OBJECT('type', 'divider')
    ))
WHERE id = 3;

UPDATE diy_template SET page_data = JSON_OBJECT(
    'page', JSON_OBJECT('name', '首页', 'bgColor', '#eef2fb'),
    'items', JSON_ARRAY(
        JSON_OBJECT('type', 'search', 'placeholder', '搜索型号 / 配件'),
        JSON_OBJECT('type', 'imageWindow', 'images', JSON_ARRAY(
            JSON_OBJECT('url', '', 'link', ''), JSON_OBJECT('url', '', 'link', ''), JSON_OBJECT('url', '', 'link', '')), 'bgColor', '#eef2fb'),
        JSON_OBJECT('type', 'video', 'cover', '', 'url', '', 'height', 190, 'autoplay', false, 'margin', 0, 'bgColor', '#000000'),
        JSON_OBJECT('type', 'goods', 'style', 'grid', 'goodsIds', JSON_ARRAY(), 'limit', 6,
                    'bgColor', '#eef2fb', 'showName', true, 'showPrice', true, 'showLinePrice', true),
        JSON_OBJECT('type', 'richText', 'html', '<p>7 天无理由退换 · 全国联保</p>', 'bgColor', '#ffffff')
    ))
WHERE id = 4;
