package com.shopplatform.domain.marketing.service;

import com.shopplatform.domain.marketing.entity.SignRecord;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.time.LocalDate;
import java.util.Map;

public interface SignRecordService extends TenantSafeService<SignRecord> {

    /**
     * 签到状态查询。
     * 返回：signedToday, streakDays, dailyPoints, continuousRules(已解析列表),
     *       week[{date, signed, isToday, isMakeup, points}],
     *       canMakeupYesterday
     */
    Map<String, Object> status(Long userId);

    /** 每日签到。返回 {earnedPoints, dayNumber, userCouponId(nullable)}。 */
    Map<String, Object> dailySign(Long userId);

    /** 补签指定日期（仅接受昨天）。补签不发放积分，仅恢复连续记录。 */
    Map<String, Object> makeupSign(Long userId, LocalDate date);
}
