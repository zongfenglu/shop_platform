package com.shopplatform.domain.shop.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.enums.ShopStatus;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.diy.entity.DiyPage;
import com.shopplatform.domain.diy.service.DiyPageService;
import com.shopplatform.domain.goods.service.GoodsService;
import com.shopplatform.domain.offlinestore.entity.OfflineStore;
import com.shopplatform.domain.offlinestore.service.OfflineStoreService;
import com.shopplatform.domain.shop.entity.PackageTpl;
import com.shopplatform.domain.shop.entity.Shop;
import com.shopplatform.domain.shop.entity.ShopOrder;
import com.shopplatform.domain.shop.entity.ShopPackage;
import com.shopplatform.domain.shop.entity.StoreUser;
import com.shopplatform.domain.shop.service.PackageTplService;
import com.shopplatform.domain.shop.service.ShopOrderService;
import com.shopplatform.domain.shop.service.ShopPackageService;
import com.shopplatform.domain.shop.service.ShopService;
import com.shopplatform.domain.shop.service.ShopSubscriptionService;
import com.shopplatform.domain.shop.service.StoreUserService;
import com.shopplatform.framework.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ShopSubscriptionServiceImpl implements ShopSubscriptionService {

    private final ShopService shopService;
    private final ShopPackageService shopPackageService;
    private final PackageTplService packageTplService;
    private final ShopOrderService shopOrderService;
    private final GoodsService goodsService;
    private final StoreUserService storeUserService;
    private final DiyPageService diyPageService;
    private final OfflineStoreService offlineStoreService;
    private final ObjectMapper objectMapper;

    public ShopSubscriptionServiceImpl(ShopService shopService,
                                       ShopPackageService shopPackageService,
                                       PackageTplService packageTplService,
                                       ShopOrderService shopOrderService,
                                       GoodsService goodsService,
                                       StoreUserService storeUserService,
                                       DiyPageService diyPageService,
                                       OfflineStoreService offlineStoreService,
                                       ObjectMapper objectMapper) {
        this.shopService = shopService;
        this.shopPackageService = shopPackageService;
        this.packageTplService = packageTplService;
        this.shopOrderService = shopOrderService;
        this.goodsService = goodsService;
        this.storeUserService = storeUserService;
        this.diyPageService = diyPageService;
        this.offlineStoreService = offlineStoreService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Overview overview(Long shopId) {
        Shop shop = requireShop(shopId);
        ShopPackage current = currentPackage(shop);
        List<QuotaUsage> quotas = buildQuotas(current);
        List<PackageTpl> plans = packageTplService.list(Wrappers.<PackageTpl>lambdaQuery()
                .eq(PackageTpl::getIsShow, true)
                .orderByAsc(PackageTpl::getSort));
        List<ShopOrder> orders = shopOrderService.list(Wrappers.<ShopOrder>lambdaQuery()
                .eq(ShopOrder::getShopId, shopId)
                .orderByDesc(ShopOrder::getCreateTime));
        long daysLeft = 0;
        if (shop.getExpireTime() != null) {
            daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), shop.getExpireTime().toLocalDate());
        }
        return new Overview(
                shop.getName(),
                shop.getStatus(),
                shop.getExpireTime(),
                daysLeft,
                current,
                quotas,
                plans,
                orders
        );
    }

    @Override
    @Transactional
    public ShopOrder placeOrder(Long shopId, String type, Long packageTplId, int durationMonth) {
        if (!"renew".equals(type) && !"upgrade".equals(type)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "仅支持续费或升级");
        }
        if (durationMonth != 1 && durationMonth != 3 && durationMonth != 12) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "订购周期仅支持 1/3/12 个月");
        }
        Shop shop = requireShop(shopId);
        long pending = shopOrderService.count(Wrappers.<ShopOrder>lambdaQuery()
                .eq(ShopOrder::getShopId, shopId)
                .eq(ShopOrder::getPayStatus, "pending"));
        if (pending > 0) {
            throw new BusinessException(ErrorCode.SHOP_ORDER_PENDING_EXISTS);
        }

        ShopPackage current = currentPackage(shop);
        PackageTpl tpl;
        if ("renew".equals(type)) {
            Long tplId = packageTplId != null ? packageTplId
                    : (current == null ? null : current.getPackageTplId());
            if (tplId == null) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "当前没有可续费的套餐");
            }
            tpl = requireTpl(tplId);
        } else {
            if (packageTplId == null) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "请选择要升级的套餐");
            }
            tpl = requireTpl(packageTplId);
            if (current != null && packageTplId.equals(current.getPackageTplId())) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "已是该套餐，请使用续费");
            }
        }

        ShopOrder order = new ShopOrder();
        order.setOrderNo(nextOrderNo());
        order.setShopId(shopId);
        order.setType(type);
        order.setPackageTplId(tpl.getId());
        order.setDurationMonth(durationMonth);
        order.setAmount(priceOf(tpl, durationMonth));
        order.setPayStatus("pending");
        order.setPayMethod("offline");
        shopOrderService.save(order);
        return order;
    }

    @Override
    @Transactional
    public ShopOrder confirmPaid(Long shopOrderId) {
        ShopOrder order = shopOrderService.getOne(Wrappers.<ShopOrder>lambdaQuery().eq(ShopOrder::getId, shopOrderId));
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订购单不存在");
        }
        if (!"pending".equals(order.getPayStatus())) {
            throw new BusinessException(ErrorCode.SHOP_ORDER_STATUS_INVALID);
        }
        Shop shop = requireShop(order.getShopId());
        PackageTpl tpl = requireTpl(order.getPackageTplId());
        int months = order.getDurationMonth() == null ? 12 : order.getDurationMonth();

        order.setPayStatus("paid");
        order.setPayTime(LocalDateTime.now());
        shopOrderService.updateById(order);

        ShopPackage pkg = currentPackage(shop);
        LocalDateTime base = shop.getExpireTime() != null && shop.getExpireTime().isAfter(LocalDateTime.now())
                ? shop.getExpireTime()
                : LocalDateTime.now();
        LocalDateTime expire = base.plusMonths(months);

        TenantContext.set(shop.getId());
        try {
            if (pkg == null || "upgrade".equals(order.getType())) {
                if (pkg == null) {
                    pkg = new ShopPackage();
                    pkg.setShopId(shop.getId());
                    pkg.setStartTime(LocalDateTime.now());
                }
                pkg.setPackageTplId(tpl.getId());
                pkg.setName(tpl.getName());
                pkg.setMenus(tpl.getMenus());
                pkg.setQuota(tpl.getQuota());
                pkg.setPriceSnapshot(tpl.getPrice());
                pkg.setExpireTime(expire);
                if (pkg.getId() == null) {
                    shopPackageService.save(pkg);
                } else {
                    shopPackageService.updateById(pkg);
                }
                shop.setPackageId(pkg.getId());
            } else {
                pkg.setExpireTime(expire);
                shopPackageService.updateById(pkg);
            }
        } finally {
            TenantContext.clear();
        }

        shop.setExpireTime(expire);
        if (ShopStatus.EXPIRED.getCode().equals(shop.getStatus()) || ShopStatus.TRIAL.getCode().equals(shop.getStatus())) {
            shop.setStatus(ShopStatus.NORMAL.getCode());
        }
        shopService.updateById(shop);
        return order;
    }

    private List<QuotaUsage> buildQuotas(ShopPackage current) {
        JsonNode quota = readJson(current == null ? null : current.getQuota());
        long goodsMax = longVal(quota, "goods_max", 0);
        long staffMax = longVal(quota, "staff_max", 0);
        long storeMax = longVal(quota, "store_max", 0);
        long diyMax = longVal(quota, "diy_page_max", 0);
        long goodsUsed = goodsService.count();
        long staffUsed = storeUserService.count(Wrappers.<StoreUser>lambdaQuery());
        long storeUsed = offlineStoreService.count(Wrappers.<OfflineStore>lambdaQuery());
        long diyUsed = diyPageService.count(Wrappers.<DiyPage>lambdaQuery());
        List<QuotaUsage> out = new ArrayList<>();
        out.add(usage("goods", "商品数", goodsUsed, goodsMax));
        out.add(usage("staff", "员工账号", staffUsed, staffMax));
        out.add(usage("store", "自提门店", storeUsed, storeMax));
        out.add(usage("diy", "装修页", diyUsed, diyMax));
        return out;
    }

    private QuotaUsage usage(String key, String label, long used, long max) {
        int percent;
        if (max < 0) {
            percent = 0;
        } else if (max == 0) {
            percent = used > 0 ? 100 : 0;
        } else {
            percent = (int) Math.min(100, Math.round(used * 100.0 / max));
        }
        return new QuotaUsage(key, label, used, max, percent);
    }

    private ShopPackage currentPackage(Shop shop) {
        if (shop.getPackageId() == null) {
            return shopPackageService.getOne(Wrappers.<ShopPackage>lambdaQuery()
                    .eq(ShopPackage::getShopId, shop.getId())
                    .orderByDesc(ShopPackage::getExpireTime)
                    .last("LIMIT 1"));
        }
        return shopPackageService.getOne(Wrappers.<ShopPackage>lambdaQuery().eq(ShopPackage::getId, shop.getPackageId()));
    }

    private Shop requireShop(Long shopId) {
        Shop shop = shopService.getOne(Wrappers.<Shop>lambdaQuery().eq(Shop::getId, shopId));
        if (shop == null) {
            throw new BusinessException(ErrorCode.TENANT_NOT_FOUND);
        }
        return shop;
    }

    private PackageTpl requireTpl(Long id) {
        PackageTpl tpl = packageTplService.getOne(Wrappers.<PackageTpl>lambdaQuery().eq(PackageTpl::getId, id));
        if (tpl == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "套餐不存在");
        }
        return tpl;
    }

    private BigDecimal priceOf(PackageTpl tpl, int durationMonth) {
        JsonNode price = readJson(tpl.getPrice());
        String key = durationMonth >= 12 ? "year" : (durationMonth >= 3 ? "quarter" : "month");
        JsonNode n = price == null ? null : price.get(key);
        if (n == null || !n.isNumber()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return n.decimalValue().setScale(2, RoundingMode.HALF_UP);
    }

    private JsonNode readJson(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private static long longVal(JsonNode node, String field, long fallback) {
        if (node == null || node.get(field) == null || !node.get(field).isNumber()) {
            return fallback;
        }
        return node.get(field).asLong();
    }

    private static String nextOrderNo() {
        return "PO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
