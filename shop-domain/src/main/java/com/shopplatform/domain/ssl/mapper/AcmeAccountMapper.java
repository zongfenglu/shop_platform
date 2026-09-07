package com.shopplatform.domain.ssl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopplatform.domain.ssl.entity.AcmeAccount;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AcmeAccountMapper extends BaseMapper<AcmeAccount> {
}
