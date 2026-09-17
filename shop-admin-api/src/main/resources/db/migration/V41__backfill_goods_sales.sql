-- 历史版本只扣减库存，未在支付成功后累计 goods.sales_actual。
-- 按曾经支付成功的订单商品行回填一次；退款订单仍属于历史已售口径，与新支付链路不回减销量保持一致。
UPDATE goods g
LEFT JOIN (
    SELECT og.shop_id, og.goods_id, SUM(og.total_num) AS sold_num
    FROM order_goods og
    INNER JOIN `order` o
        ON o.id = og.order_id
        AND o.shop_id = og.shop_id
        AND o.is_delete = 0
    WHERE og.is_delete = 0
      AND o.pay_status IN ('paid', 'refunding', 'refunded')
    GROUP BY og.shop_id, og.goods_id
) paid_goods
    ON paid_goods.shop_id = g.shop_id
    AND paid_goods.goods_id = g.id
SET g.sales_actual = COALESCE(paid_goods.sold_num, 0)
WHERE g.is_delete = 0;
