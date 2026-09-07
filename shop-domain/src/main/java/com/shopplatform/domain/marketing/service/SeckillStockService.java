package com.shopplatform.domain.marketing.service;

/**
 * 秒杀库存的 Redis 快路径：原子预扣 + 限购 + 回补 + 初始化/对账。
 * 见文档三 §5："秒杀 | Redis 预扣（Lua 原子脚本）拦掉绝大部分流量 → 入队 → 落库扣减；失败回补 Redis"。
 * <p>
 * 本项目采用"同步预扣闸 + DB 乐观扣双保险"：预扣成功后再走 OrderService.createOrder 的 DB 扣减，
 * 失败回补 Redis；对账定时任务据此自愈。秒杀限量池（seckill_num）独立于 goods_sku.stock。
 */
public interface SeckillStockService {

    int SUCCESS = 1;
    int STOCK_INSUFFICIENT = -1;
    int LIMIT_EXCEEDED = -2;

    /**
     * 原子预扣：校验限购（bought+qty<=limitPerUser，0=不限）与库存（stock>=qty），通过则 DECRBY stock / INCRBY bought。
     * @return {@link #SUCCESS} / {@link #STOCK_INSUFFICIENT} / {@link #LIMIT_EXCEEDED}
     */
    int preDeduct(Long userId, Long activeId, Long skuId, int qty, int limitPerUser);

    /** 回补：INCRBY stock / DECRBY bought（下单失败回滚用）。 */
    void rollback(Long userId, Long activeId, Long skuId, int qty);

    /** 初始化秒杀限量池：SET stock = seckillNum（活动商品保存/上架时调用）。 */
    void initStock(Long activeId, Long skuId, int seckillNum);

    /** 对账：SET stock = max(0, seckillNum - sold)，用 DB 已售修正 Redis（对账定时任务调用）。 */
    void reconcile(Long activeId, Long skuId, int seckillNum, int sold);

    /** 清理：删除 stock key（活动结束清理）。 */
    void evictStock(Long activeId, Long skuId);

    /** 读取 Redis 剩余限量；key 不存在返回 null（调用方回退到 seckill_num - sold）。 */
    Integer getStock(Long activeId, Long skuId);
}
