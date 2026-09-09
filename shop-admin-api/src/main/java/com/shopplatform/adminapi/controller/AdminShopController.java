package com.shopplatform.adminapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shopplatform.adminapi.dto.CreateShopRequest;
import com.shopplatform.adminapi.dto.CreateShopResponse;
import com.shopplatform.adminapi.dto.DisableShopRequest;
import com.shopplatform.adminapi.dto.ResetShopPasswordRequest;
import com.shopplatform.adminapi.dto.ShopListQuery;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.shop.entity.Shop;
import com.shopplatform.domain.shop.entity.ShopPackage;
import com.shopplatform.domain.shop.entity.StoreUser;
import com.shopplatform.domain.platform.service.SysLogService;
import com.shopplatform.domain.shop.service.ImpersonateTicketService;
import com.shopplatform.domain.shop.service.ShopPackageService;
import com.shopplatform.domain.shop.service.ShopService;
import com.shopplatform.domain.shop.service.StoreUserService;
import com.shopplatform.framework.web.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import com.shopplatform.adminapi.dto.UpdateShopRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 商城管理。对应原型 admin/shop-list.html、shop-new.html、shop-detail.html。
 * <p>
 * 平台管理员操作，本身不带租户上下文（default 无租户），
 * 但建店流程内部会临时切换到新建商城的 shopId 去写入种子数据，见 {@code ShopServiceImpl#createShop}。
 */
@RestController
@RequestMapping("/admin/shops")
public class AdminShopController {

    private final ShopService shopService;
    private final StoreUserService storeUserService;
    private final ImpersonateTicketService impersonateTicketService;
    private final SysLogService sysLogService;
    private final ShopPackageService shopPackageService;
    private final String storeConsoleUrl;

    public AdminShopController(ShopService shopService,
                               StoreUserService storeUserService,
                               ImpersonateTicketService impersonateTicketService,
                               SysLogService sysLogService,
                               ShopPackageService shopPackageService,
                               @Value("${shop.store-console-url:http://localhost:5174}") String storeConsoleUrl) {
        this.shopService = shopService;
        this.storeUserService = storeUserService;
        this.impersonateTicketService = impersonateTicketService;
        this.sysLogService = sysLogService;
        this.shopPackageService = shopPackageService;
        this.storeConsoleUrl = storeConsoleUrl.endsWith("/")
                ? storeConsoleUrl.substring(0, storeConsoleUrl.length() - 1)
                : storeConsoleUrl;
    }

    @GetMapping
    public Result<IPage<Shop>> list(ShopListQuery query) {
        var wrapper = Wrappers.<Shop>lambdaQuery();
        if (StringUtils.hasText(query.keyword())) {
            wrapper.and(w -> w.like(Shop::getName, query.keyword())
                    .or().like(Shop::getMobile, query.keyword())
                    .or().like(Shop::getCode, query.keyword()));
        }
        if (StringUtils.hasText(query.status())) {
            wrapper.eq(Shop::getStatus, query.status());
        }
        wrapper.orderByDesc(Shop::getCreateTime);

        Page<Shop> page = new Page<>(query.pageNumOrDefault(), query.pageSizeOrDefault());
        IPage<Shop> result = shopService.page(page, wrapper);
        fillPackageNames(result.getRecords());
        return Result.ok(result);
    }

    private void fillPackageNames(List<Shop> shops) {
        List<Long> packageIds = shops.stream().map(Shop::getPackageId).filter(Objects::nonNull).distinct().toList();
        if (packageIds.isEmpty()) {
            return;
        }
        Map<Long, String> names = shopPackageService.listByIds(packageIds).stream()
                .collect(Collectors.toMap(ShopPackage::getId, ShopPackage::getName, (a, b) -> a));
        for (Shop shop : shops) {
            shop.setPackageName(shop.getPackageId() == null ? null : names.get(shop.getPackageId()));
        }
    }

    @PostMapping
    public Result<CreateShopResponse> create(@Valid @RequestBody CreateShopRequest request) {
        Shop shop = shopService.createShop(new ShopService.CreateShopCommand(
                request.name(),
                request.code(),
                request.industry(),
                request.contact(),
                request.mobile(),
                request.remark(),
                request.packageTplId(),
                request.durationMonth()
        ));

        return Result.ok(new CreateShopResponse(
                shop.getId(),
                shop.getCode(),
                shop.getName(),
                shop.getCode() + "_admin",
                shop.getExpireTime()
        ));
    }

    @GetMapping("/{id}")
    public Result<Shop> detail(@PathVariable Long id) {
        // 平台侧查看任意商城详情，属于合法的跨租户查询场景（管理员视角），
        // 但 shop 表本身不带 shop_id（它是租户的根），走的是忽略表白名单。
        // 仍统一走 getByIdWithTenant：查不到时返回 404 语义而不是"200+null"，与全站其他按主键查询接口保持一致。
        return Result.ok(shopService.getByIdWithTenant(id));
    }

    @PatchMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody UpdateShopRequest request,
                               HttpServletRequest httpRequest) {
        Shop before = shopService.getByIdWithTenant(id);
        shopService.updateInfo(id, new ShopService.UpdateShopCommand(
                request.name(), request.code(), request.industry(),
                request.contact(), request.mobile(), request.remark()));
        sysLogService.record(before.getId(), "shop-update",
                "修改商城信息 " + before.getName(), ClientIp.resolve(httpRequest));
        return Result.ok();
    }

    /**
     * 平台免密进入商户后台。一次性 ticket 5 分钟有效，打开 store 登录页兑换 JWT。
     * 会话带 platformImpersonation=true，商户后台顶部展示「平台代管理中」。
     */
    @PostMapping("/{id}/impersonate")
    public Result<Map<String, Object>> impersonate(@PathVariable Long id, HttpServletRequest request) {
        Shop shop = shopService.getByIdWithTenant(id);
        String status = shop.getStatus();
        if (!"trial".equals(status) && !"normal".equals(status) && !"expired".equals(status)) {
            throw new BusinessException(ErrorCode.TENANT_DISABLED, "商城已停用，无法免密登录");
        }
        StoreUser user = storeUserService.findImpersonationTarget(shop.getId());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "该商城没有可用的店主账号");
        }
        String ticket = impersonateTicketService.issue(shop.getId(), user.getId());
        sysLogService.record(shop.getId(), "impersonate", "免密进入 " + shop.getName(), ClientIp.resolve(request));
        String loginUrl = storeConsoleUrl + "/login?ticket=" + ticket;
        return Result.ok(Map.of(
                "ticket", ticket,
                "expireSeconds", ImpersonateTicketService.TTL_SECONDS,
                "loginUrl", loginUrl
        ));
    }

    @PostMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @Valid @RequestBody ResetShopPasswordRequest request,
                                      HttpServletRequest httpRequest) {
        Shop shop = shopService.getByIdWithTenant(id);
        shopService.resetOwnerPassword(id, request.password());
        sysLogService.record(shop.getId(), "reset-owner-password",
                "重置店主密码 " + shop.getName(), ClientIp.resolve(httpRequest));
        return Result.ok();
    }

    @PostMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable Long id,
                                @Valid @RequestBody DisableShopRequest request,
                                HttpServletRequest httpRequest) {
        Shop shop = shopService.getByIdWithTenant(id);
        shopService.disable(id);
        String reason = request.reason().trim();
        sysLogService.record(shop.getId(), "shop-disable",
                "停用商城 " + shop.getName() + "：" + reason, ClientIp.resolve(httpRequest));
        return Result.ok();
    }

    @PostMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable Long id, HttpServletRequest httpRequest) {
        Shop shop = shopService.getByIdWithTenant(id);
        shopService.enable(id);
        sysLogService.record(shop.getId(), "shop-enable",
                "启用商城 " + shop.getName(), ClientIp.resolve(httpRequest));
        return Result.ok();
    }
}
