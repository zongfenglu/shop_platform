package com.shopplatform.clientapi.controller;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.marketing.service.SignRecordService;
import com.shopplatform.framework.security.LoginUserContext;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 消费者签到。对应原型 h5/sign-in.html。
 * 所有接口需要登录，仅操作当前登录用户自己的数据。
 */
@RestController
@RequestMapping("/api/sign")
public class ConsumerSignController {

    private final SignRecordService signRecordService;

    public ConsumerSignController(SignRecordService signRecordService) {
        this.signRecordService = signRecordService;
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        return Result.ok(signRecordService.status(requireLoginUserId()));
    }

    @PostMapping("/daily")
    public Result<Map<String, Object>> dailySign() {
        return Result.ok(signRecordService.dailySign(requireLoginUserId()));
    }

    @PostMapping("/makeup")
    public Result<Map<String, Object>> makeupSign(@RequestBody Map<String, String> body) {
        String dateStr = body.get("date");
        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "日期格式错误");
        }
        return Result.ok(signRecordService.makeupSign(requireLoginUserId(), date));
    }

    private Long requireLoginUserId() {
        LoginUserContext.LoginUser user = LoginUserContext.get();
        if (user == null || user.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return user.userId();
    }
}
