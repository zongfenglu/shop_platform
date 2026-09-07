package com.shopplatform.domain.shop.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.common.enums.ShopStatus;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.shop.entity.Shop;
import com.shopplatform.domain.shop.entity.ShopDomain;
import com.shopplatform.domain.shop.entity.ShopPackage;
import com.shopplatform.domain.shop.entity.StoreRole;
import com.shopplatform.domain.shop.entity.StoreUser;
import com.shopplatform.domain.shop.mapper.ShopMapper;
import com.shopplatform.domain.shop.service.PackageTplService;
import com.shopplatform.domain.shop.service.ShopDomainService;
import com.shopplatform.domain.shop.service.ShopPackageService;
import com.shopplatform.domain.shop.service.ShopService;
import com.shopplatform.domain.shop.service.StoreRoleService;
import com.shopplatform.domain.shop.service.StoreUserService;
import com.shopplatform.domain.shop.entity.PackageTpl;
import com.shopplatform.domain.member.service.UserGradeService;
import com.shopplatform.framework.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements ShopService {

    private static final List<String> RESERVED_CODES = List.of(
            "admin", "api", "www", "store", "mp", "cdn", "static", "mail", "ftp");

    private final PackageTplService packageTplService;
    private final ShopPackageService shopPackageService;
    private final StoreRoleService storeRoleService;
    private final StoreUserService storeUserService;
    private final ShopDomainService shopDomainService;
    private final UserGradeService userGradeService;
    private final PasswordEncoder passwordEncoder;

    @Value("${shop.platform.base-domain:shop.com}")
    private String platformBaseDomain;

    public ShopServiceImpl(PackageTplService packageTplService,
                            ShopPackageService shopPackageService,
                            StoreRoleService storeRoleService,
                            StoreUserService storeUserService,
                            ShopDomainService shopDomainService,
                            UserGradeService userGradeService,
                            PasswordEncoder passwordEncoder) {
        this.packageTplService = packageTplService;
        this.shopPackageService = shopPackageService;
        this.storeRoleService = storeRoleService;
        this.storeUserService = storeUserService;
        this.shopDomainService = shopDomainService;
        this.userGradeService = userGradeService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Shop createShop(CreateShopCommand cmd) {
        validateCode(cmd.code());

        // 幂等：code 唯一约束，重复调用直接返回已存在记录，见文档二 §1.2 建店流程要求"可重试"
        Shop existing = this.getOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<Shop>lambdaQuery().eq(Shop::getCode, cmd.code()));
        if (existing != null) {
            return existing;
        }

        // package_tpl 是平台级表（无 shop_id），仍统一走 getByIdWithTenant 而不是裸用 getById——
        // 保持全代码库"按主键查询只有一种写法"，不要因为某张表恰好没有 shop_id 就破例，
        // 否则往后每个人都要先判断"这张表算不算租户表"才知道该用哪个方法，维护成本更高。
        PackageTpl tpl;
        try {
            tpl = packageTplService.getByIdWithTenant(cmd.packageTplId());
        } catch (com.shopplatform.common.exception.TenantAccessDeniedException e) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "套餐模板不存在");
        }

        Shop shop = new Shop();
        shop.setCode(cmd.code());
        shop.setName(cmd.name());
        shop.setIndustry(cmd.industry());
        shop.setContact(cmd.contact());
        shop.setMobile(cmd.mobile());
        shop.setRemark(cmd.remark());
        shop.setStatus(ShopStatus.TRIAL.getCode());
        int months = cmd.durationMonth() == null ? 1 : cmd.durationMonth();
        LocalDateTime expireTime = LocalDateTime.now().plusMonths(months);
        shop.setExpireTime(expireTime);
        this.save(shop);

        // 套餐快照：文档一 §3.4 —— 开通时把套餐内容快照进 shop_package，之后平台改模板不影响本记录
        ShopPackage shopPackage = new ShopPackage();
        shopPackage.setShopId(shop.getId());
        shopPackage.setPackageTplId(tpl.getId());
        shopPackage.setName(tpl.getName());
        shopPackage.setMenus(tpl.getMenus());
        shopPackage.setQuota(tpl.getQuota());
        shopPackage.setPriceSnapshot(tpl.getPrice());
        shopPackage.setStartTime(LocalDateTime.now());
        shopPackage.setExpireTime(expireTime);
        // shop_package 是租户表（带 shop_id），必须在租户上下文里保存
        TenantContext.set(shop.getId());
        try {
            shopPackageService.save(shopPackage);

            shop.setPackageId(shopPackage.getId());
            this.updateById(shop);

            // 泛域名绑定：{code}.{平台基础域名}，建店即可用，无需额外审核（见文档三 §2.2 消费者端 Host 识别）
            ShopDomain domain = new ShopDomain();
            domain.setShopId(shop.getId());
            domain.setDomain(cmd.code() + "." + platformBaseDomain);
            domain.setType("sub");
            domain.setCertStatus("valid");
            domain.setVerifyStatus("verified");
            domain.setCnameStatus("ok");
            shopDomainService.save(domain);

            // 超级店主账号 + 超级管理员角色：建店流程的必要产物，见文档一 §3.1
            StoreRole ownerRole = new StoreRole();
            ownerRole.setName("超级管理员");
            ownerRole.setMenuIds("[]");
            ownerRole.setDataScope("all");
            ownerRole.setIsBuiltin(true);
            storeRoleService.save(ownerRole);

            StoreUser owner = new StoreUser();
            owner.setUsername(cmd.code() + "_admin");
            owner.setPassword(passwordEncoder.encode(generateInitialPassword()));
            owner.setRealName(cmd.contact());
            owner.setMobile(cmd.mobile());
            owner.setRoleId(ownerRole.getId());
            owner.setIsSuperOwner(true);
            owner.setStatus(1);
            storeUserService.save(owner);

            // 默认会员等级（普通/银卡/金卡/钻石）——文档二 §1.2 建店种子数据清单中的"会员等级"项。
            // 放在租户上下文里灌入，UserGradeService.seedDefaults 内部已做幂等，建店重试不会产生重复等级。
            userGradeService.seedDefaults();
        } finally {
            TenantContext.clear();
        }

        return shop;
    }

    @Override
    public Shop findByCode(String code) {
        // shop 表本身不带 shop_id（它是租户的根），走忽略表白名单，无需 TenantContext.ignoreTenant 包裹。
        return this.getOne(com.baomidou.mybatisplus.core.toolkit.Wrappers.<Shop>lambdaQuery().eq(Shop::getCode, code));
    }

    @Override
    public List<Long> listAllShopIds() {
        // shop 表在忽略表白名单里，任何 TenantContext 状态下都返回全部商城
        return this.list().stream().map(Shop::getId).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetOwnerPassword(Long shopId, String newPassword) {
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 8 || newPassword.length() > 72) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "密码长度需为 8～72 位");
        }
        getByIdWithTenant(shopId);
        StoreUser owner = storeUserService.findSuperOwner(shopId);
        if (owner == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "该商城没有店主账号");
        }
        owner.setPassword(passwordEncoder.encode(newPassword));
        TenantContext.set(shopId);
        try {
            storeUserService.updateById(owner);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long shopId) {
        Shop shop = getByIdWithTenant(shopId);
        if (ShopStatus.ARCHIVED.getCode().equals(shop.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "已归档的商城不能停用");
        }
        if (ShopStatus.DISABLED.getCode().equals(shop.getStatus())) {
            return;
        }
        shop.setStatus(ShopStatus.DISABLED.getCode());
        updateById(shop);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enable(Long shopId) {
        Shop shop = getByIdWithTenant(shopId);
        if (!ShopStatus.DISABLED.getCode().equals(shop.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "仅已停用的商城可以启用");
        }
        LocalDateTime expire = shop.getExpireTime();
        if (expire != null && expire.isBefore(LocalDateTime.now())) {
            shop.setStatus(ShopStatus.EXPIRED.getCode());
        } else {
            shop.setStatus(ShopStatus.NORMAL.getCode());
        }
        updateById(shop);
    }

    private void validateCode(String code) {
        if (!StringUtils.hasText(code) || !code.matches("^[a-z0-9-]{3,32}$")) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "二级域名前缀仅支持小写字母、数字、短横线，长度3-32");
        }
        if (RESERVED_CODES.contains(code)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "该域名前缀为系统保留词，请更换");
        }
    }

    /** 初始密码后续通过短信下发给店主（见文档二 §1.2），此处仅生成，短信发送由通知服务异步处理。 */
    private String generateInitialPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
        }
        return sb.toString();
    }
}
