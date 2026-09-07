package com.shopplatform.domain.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shopplatform.domain.platform.entity.SysLog;

public interface SysLogService extends IService<SysLog> {

    void record(Long shopId,
                int operatorType,
                Long operatorId,
                String operatorName,
                boolean byPlatform,
                String action,
                String description,
                String ip);

    /** 从 {@code LoginUserContext} 取操作人；登录等尚未入上下文的场景请用完整 {@link #record}。 */
    void record(Long shopId, String action, String description, String ip);

    IPage<SysLog> page(String kind, int pageNum, int pageSize);
}
