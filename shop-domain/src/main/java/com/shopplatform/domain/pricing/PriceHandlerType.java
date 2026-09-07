package com.shopplatform.domain.pricing;

/**
 * 价格计算责任链的固定顺序。见文档三 §4：
 * "责任链顺序（固定，不可配置乱序）"。
 * <p>
 * 用枚举而不是让 Spring 按 Bean 注入顺序（{@code @Order} 注解、List 注入顺序）决定执行顺序——
 * 注入顺序依赖 classpath 扫描结果，不同环境/不同 Spring 版本可能不一致，属于"能跑但不可靠"的写法。
 * {@link PriceCalculator} 拿到所有 handler 后按本枚举的 ordinal 显式排序，顺序由这里唯一确定。
 */
public enum PriceHandlerType {

    /** 原价小计（按 SKU） */
    BASE,
    /** 秒杀 / 拼团 / 限时折扣（互斥，取活动价替换原价）—— M2 落地 */
    ACTIVITY,
    /** 会员等级折扣（活动商品默认不叠加，可配置）—— M2 落地 */
    MEMBER_DISCOUNT,
    /** 满减 / 满件折 —— M2 落地 */
    FULL_REDUCE,
    /** 优惠券（校验适用范围、门槛按"参与商品实付小计"判定）—— M2 落地 */
    COUPON,
    /** 积分抵扣（受最高抵扣比例限制）—— M2 落地 */
    POINTS,
    /** 运费（运费模板 + 包邮规则 + 自提免运费） */
    FREIGHT,
    /** 分摊与舍入 */
    ROUNDING
}
