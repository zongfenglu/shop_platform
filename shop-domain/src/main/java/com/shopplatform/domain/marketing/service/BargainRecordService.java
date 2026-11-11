package com.shopplatform.domain.marketing.service;

import com.shopplatform.domain.marketing.entity.BargainRecord;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface BargainRecordService extends TenantSafeService<BargainRecord> {

    /** 发起砍价：创建 ongoing 记录，current_price=originPrice，expire=now+validHours。已存在则返回已有记录。 */
    BargainRecord startBargain(Long activeId, Long userId, BigDecimal originPrice, int validHours);

    /** 助力砍一刀：current_price 递减 cutAmount（不低于 floorPrice），help_count+1。返回更新后记录；已结束返回 null。 */
    BargainRecord helpCut(Long recordId, BigDecimal cutAmount, BigDecimal floorPrice);

    /** 按活动+用户查记录（含过期/已下单，uk_active_user 每人每活动一条），不存在返回 null */
    BargainRecord findByActiveAndUser(Long activeId, Long userId);

    /** 按下单后的订单 id 找回砍价记录，订单详情展示用 */
    BargainRecord findByOrderId(Long orderId);

    /** 当前用户尚未下单、且未过期的砍价（ongoing / done） */
    List<BargainRecord> listInProgress(Long userId);

    /** 扫描 ongoing/done 且已过期的砍价记录（BargainExpireJob 用） */
    List<BargainRecord> listExpired(LocalDateTime now);

    /** 置为过期 */
    void markExpired(Long recordId);

    /** 下单后置为 ordered 并记录订单 id */
    void markOrdered(Long recordId, Long orderId);
}
