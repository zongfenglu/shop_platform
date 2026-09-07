package com.shopplatform.clientapi.controller;

import com.shopplatform.clientapi.service.DiyRenderAppService;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.diy.entity.ShopCategoryPage;
import com.shopplatform.domain.diy.service.ShopCategoryPageService;
import com.shopplatform.framework.tenant.TenantContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 消费者端装修渲染。租户由 shop-client-api 统一的 ClientTenantFilter
 * （X-Shop-Id / Host / AppID）解析并写入 TenantContext，与其余 Consumer* 控制器一致。
 * - GET /api/diy/home：当前商城首页，未装修返回 {exists:false}
 * - GET /api/diy/page/{id}：指定自定义页，未发布/跨店/不存在统一按 NOT_FOUND 处理
 */
@RestController
@RequestMapping("/api/diy")
public class ConsumerDiyController {

    private final DiyRenderAppService diyRenderAppService;
    private final ShopCategoryPageService shopCategoryPageService;

    public ConsumerDiyController(DiyRenderAppService diyRenderAppService,
                                 ShopCategoryPageService shopCategoryPageService) {
        this.diyRenderAppService = diyRenderAppService;
        this.shopCategoryPageService = shopCategoryPageService;
    }

    @GetMapping("/home")
    public Result<Map<String, Object>> home() {
        return Result.ok(diyRenderAppService.renderHome(TenantContext.getRequired()));
    }

    @GetMapping("/tabbar")
    public Result<Map<String, Object>> tabbar() {
        return Result.ok(diyRenderAppService.renderTabbar(TenantContext.getRequired()));
    }

    @GetMapping("/page/{id}")
    public Result<Map<String, Object>> page(@PathVariable Long id) {
        return Result.ok(diyRenderAppService.renderPage(TenantContext.getRequired(), id));
    }

    /** 分类 Tab 版式。未配置时返回默认一级小图。 */
    @GetMapping("/category-page")
    public Result<Map<String, Object>> categoryPage() {
        ShopCategoryPage row = shopCategoryPageService.getOrCreate();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("style", row.getStyle());
        out.put("shareTitle", row.getShareTitle());
        return Result.ok(out);
    }
}
