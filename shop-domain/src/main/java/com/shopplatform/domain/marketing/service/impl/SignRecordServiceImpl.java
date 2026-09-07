package com.shopplatform.domain.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.marketing.entity.SignConfig;
import com.shopplatform.domain.marketing.entity.SignRecord;
import com.shopplatform.domain.marketing.mapper.SignRecordMapper;
import com.shopplatform.domain.marketing.service.ContinuousRule;
import com.shopplatform.domain.marketing.service.SignConfigService;
import com.shopplatform.domain.marketing.service.SignRecordService;
import com.shopplatform.domain.marketing.service.UserCouponService;
import com.shopplatform.domain.member.service.MemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class SignRecordServiceImpl extends ServiceImpl<SignRecordMapper, SignRecord> implements SignRecordService {

    private static final Logger log = LoggerFactory.getLogger(SignRecordServiceImpl.class);

    private final SignConfigService signConfigService;
    private final MemberService memberService;
    private final UserCouponService userCouponService;

    public SignRecordServiceImpl(SignConfigService signConfigService,
                                  MemberService memberService,
                                  UserCouponService userCouponService) {
        this.signConfigService = signConfigService;
        this.memberService = memberService;
        this.userCouponService = userCouponService;
    }

    @Override
    public Map<String, Object> status(Long userId) {
        LocalDate today = LocalDate.now();
        SignConfig config = signConfigService.getOrCreate();
        List<ContinuousRule> rules = ContinuousRule.parseList(config.getContinuousRules());

        // 最近一次签到记录
        SignRecord lastRecord = getOne(new LambdaQueryWrapper<SignRecord>()
                .eq(SignRecord::getUserId, userId)
                .orderByDesc(SignRecord::getSignDate)
                .last("LIMIT 1"));

        boolean signedToday = lastRecord != null && today.equals(lastRecord.getSignDate());
        int streakDays = 0;
        if (lastRecord != null) {
            LocalDate lastDate = lastRecord.getSignDate();
            if (lastDate.equals(today) || lastDate.equals(today.minusDays(1))) {
                streakDays = lastRecord.getDayNumber();
            }
        }

        // 本周一~周日的签到状态
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        List<SignRecord> weekRecords = list(new LambdaQueryWrapper<SignRecord>()
                .eq(SignRecord::getUserId, userId)
                .between(SignRecord::getSignDate, monday, sunday)
                .orderByAsc(SignRecord::getSignDate));
        Map<LocalDate, SignRecord> weekMap = new LinkedHashMap<>();
        for (SignRecord r : weekRecords) {
            weekMap.put(r.getSignDate(), r);
        }

        List<Map<String, Object>> week = new ArrayList<>();
        for (LocalDate d = monday; !d.isAfter(sunday); d = d.plusDays(1)) {
            SignRecord sr = weekMap.get(d);
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", d.toString());
            day.put("signed", sr != null);
            day.put("isToday", d.equals(today));
            day.put("isMakeup", sr != null && sr.getIsMakeup() == 1);
            day.put("points", sr != null ? sr.getPointsEarned() : 0);
            // 如果是今天或未来，显示当日预计可得积分
            if (!d.isBefore(today) && sr == null) {
                day.put("previewPoints", config.getDailyPoints());
            }
            week.add(day);
        }

        // 补签：昨天未签且昨天之前有签到记录
        boolean canMakeupYesterday = false;
        if (!signedToday) {
            LocalDate yesterday = today.minusDays(1);
            boolean yesterdaySigned = false;
            if (lastRecord != null && yesterday.equals(lastRecord.getSignDate())) {
                yesterdaySigned = true;
            }
            if (!yesterdaySigned && streakDays > 0) {
                // 检查昨天是否已有记录（通过单独查询）
                long yesterdayCount = count(new LambdaQueryWrapper<SignRecord>()
                        .eq(SignRecord::getUserId, userId)
                        .eq(SignRecord::getSignDate, yesterday));
                canMakeupYesterday = yesterdayCount == 0;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("signedToday", signedToday);
        result.put("streakDays", streakDays);
        result.put("dailyPoints", config.getDailyPoints());
        result.put("continuousRules", rules);
        result.put("week", week);
        result.put("canMakeupYesterday", canMakeupYesterday);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> dailySign(Long userId) {
        LocalDate today = LocalDate.now();
        // 防重
        long count = count(new LambdaQueryWrapper<SignRecord>()
                .eq(SignRecord::getUserId, userId)
                .eq(SignRecord::getSignDate, today));
        if (count > 0) {
            throw new BusinessException(ErrorCode.SIGN_ALREADY_SIGNED, "今日已签到");
        }

        SignConfig config = signConfigService.getOrCreate();
        int dailyPoints = config.getDailyPoints() == null ? 2 : config.getDailyPoints();
        List<ContinuousRule> rules = ContinuousRule.parseList(config.getContinuousRules());

        // 查昨天的记录，定连续天数
        SignRecord yesterdayRecord = getOne(new LambdaQueryWrapper<SignRecord>()
                .eq(SignRecord::getUserId, userId)
                .eq(SignRecord::getSignDate, today.minusDays(1)));
        int dayNumber = (yesterdayRecord != null) ? yesterdayRecord.getDayNumber() + 1 : 1;

        int totalPoints = dailyPoints;

        // 每日签到积分
        memberService.adjustPoints(userId, dailyPoints, "sign", "每日签到");

        // 连续签到奖励
        Long bonusCouponId = null;
        for (ContinuousRule rule : rules) {
            if (rule.days() != null && rule.days() == dayNumber) {
                if ("points".equals(rule.type()) && rule.value() != null) {
                    memberService.adjustPoints(userId, rule.value(), "sign",
                            "连续签到" + dayNumber + "天奖励");
                    totalPoints += rule.value();
                } else if ("coupon".equals(rule.type()) && rule.couponId() != null) {
                    try {
                        userCouponService.issueForReward(userId, rule.couponId());
                        bonusCouponId = rule.couponId();
                    } catch (Exception e) {
                        log.warn("连续签到发券失败 day={} couponId={}: {}", dayNumber, rule.couponId(), e.getMessage());
                    }
                }
            }
        }

        // 写入签到记录
        SignRecord record = new SignRecord();
        record.setUserId(userId);
        record.setSignDate(today);
        record.setDayNumber(dayNumber);
        record.setPointsEarned(totalPoints);
        record.setIsMakeup(0);
        try {
            save(record);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.SIGN_ALREADY_SIGNED, "今日已签到");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("earnedPoints", totalPoints);
        result.put("dayNumber", dayNumber);
        result.put("userCouponId", bonusCouponId);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> makeupSign(Long userId, LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date == null || !date.equals(today.minusDays(1))) {
            throw new BusinessException(ErrorCode.SIGN_MAKEUP_INVALID, "仅可补签昨日");
        }

        // 检查该日期是否已有签到
        long count = count(new LambdaQueryWrapper<SignRecord>()
                .eq(SignRecord::getUserId, userId)
                .eq(SignRecord::getSignDate, date));
        if (count > 0) {
            throw new BusinessException(ErrorCode.SIGN_MAKEUP_INVALID, "该日期已有签到记录");
        }

        // 计算连续天数
        SignRecord dayBeforeRecord = getOne(new LambdaQueryWrapper<SignRecord>()
                .eq(SignRecord::getUserId, userId)
                .eq(SignRecord::getSignDate, date.minusDays(1)));
        int dayNumber = (dayBeforeRecord != null) ? dayBeforeRecord.getDayNumber() + 1 : 1;

        SignRecord record = new SignRecord();
        record.setUserId(userId);
        record.setSignDate(date);
        record.setDayNumber(dayNumber);
        record.setPointsEarned(0);
        record.setIsMakeup(1);
        try {
            save(record);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.SIGN_MAKEUP_INVALID, "该日期已有签到记录");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("earnedPoints", 0);
        result.put("dayNumber", dayNumber);
        result.put("userCouponId", null);
        return result;
    }
}
