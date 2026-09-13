package com.shopplatform.domain.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopplatform.domain.content.entity.Article;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
}
