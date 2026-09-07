package com.shopplatform.domain.member.service;

import com.shopplatform.domain.member.entity.UserGrade;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface UserGradeService extends TenantSafeService<UserGrade> {

    /**
     * 建店时灌入默认会员等级（普通/银卡/金卡/钻石）。要求幂等：已存在等级则跳过。
     * 调用时 TenantContext 必须已设为本租户。
     */
    void seedDefaults();

    /** 列出当前租户全部等级，按 weight 升序。 */
    List<UserGrade> listAllOrdered();
}
