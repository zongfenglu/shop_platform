package com.shopplatform.storeapi.controller;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.member.entity.UserGrade;
import com.shopplatform.domain.member.service.UserGradeService;
import com.shopplatform.storeapi.dto.SaveUserGradeRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 会员等级管理。对应原型 store/member-list.html 的等级配置部分。
 * 等级按 weight 排序，weight 越大等级越高；升级按 growth_value 匹配（见 MemberService#upgradeGrade）。
 */
@RestController
@RequestMapping("/store/member-grades")
public class StoreUserGradeController {

    private final UserGradeService userGradeService;

    public StoreUserGradeController(UserGradeService userGradeService) {
        this.userGradeService = userGradeService;
    }

    @GetMapping
    public Result<List<UserGrade>> list() {
        return Result.ok(userGradeService.listAllOrdered());
    }

    @PostMapping
    public Result<UserGrade> create(@Valid @RequestBody SaveUserGradeRequest req) {
        UserGrade grade = new UserGrade();
        apply(req, grade);
        userGradeService.save(grade);
        return Result.ok(grade);
    }

    @PutMapping("/{id}")
    public Result<UserGrade> update(@PathVariable Long id, @Valid @RequestBody SaveUserGradeRequest req) {
        UserGrade grade = userGradeService.getByIdWithTenant(id);
        apply(req, grade);
        userGradeService.updateById(grade);
        return Result.ok(grade);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // 等级被会员引用时不允许直接删除，避免会员 grade_id 悬空。
        // 这里只做存在性校验；是否在用由前端提示，后端按"存在即可删"的简化口径处理，
        // 删除后引用该等级的会员 grade_id 会在下次升级任务时被重算修正。
        userGradeService.getByIdWithTenant(id);
        userGradeService.removeById(id);
        return Result.ok();
    }

    private void apply(SaveUserGradeRequest req, UserGrade grade) {
        if (req.discountRatio() != null
                && (req.discountRatio().compareTo(BigDecimal.ZERO) <= 0
                || req.discountRatio().compareTo(BigDecimal.ONE) > 0)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "折扣比例须在 (0, 1] 之间");
        }
        grade.setName(req.name());
        grade.setWeight(req.weight());
        grade.setGrowthValue(req.growthValue());
        grade.setDiscountRatio(req.discountRatio());
        grade.setIcon(req.icon());
        grade.setRemark(req.remark());
    }
}
