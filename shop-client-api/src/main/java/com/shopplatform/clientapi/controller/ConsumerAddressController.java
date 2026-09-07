package com.shopplatform.clientapi.controller;

import com.shopplatform.clientapi.dto.SaveAddressRequest;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.member.entity.UserAddress;
import com.shopplatform.domain.member.service.UserAddressService;
import com.shopplatform.framework.security.LoginUserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收货地址簿。对应原型 h5/checkout.html 的地址选择入口。
 * <p>
 * 每个写操作都把 userId 从登录态里取、不接受前端传——否则改一下请求体里的 userId
 * 就能操作别人的地址（{@code shop_id} 的自动过滤只挡跨商城，挡不住同商城内跨用户）。
 */
@RestController
@RequestMapping("/api/address")
public class ConsumerAddressController {

    private final UserAddressService userAddressService;

    public ConsumerAddressController(UserAddressService userAddressService) {
        this.userAddressService = userAddressService;
    }

    @GetMapping
    public Result<List<UserAddress>> list() {
        return Result.ok(userAddressService.listByUser(requireLoginUserId()));
    }

    /** 结算页进来时默认选中的地址；一条地址都没有时返回 null（前端引导去新增）。 */
    @GetMapping("/default")
    public Result<UserAddress> getDefault() {
        return Result.ok(userAddressService.findDefault(requireLoginUserId()));
    }

    @GetMapping("/{id}")
    public Result<UserAddress> detail(@PathVariable Long id) {
        return Result.ok(userAddressService.getOwnAddress(requireLoginUserId(), id));
    }

    @PostMapping
    public Result<UserAddress> create(@Valid @RequestBody SaveAddressRequest request) {
        return Result.ok(userAddressService.create(toEntity(request, null), requireLoginUserId()));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SaveAddressRequest request) {
        userAddressService.update(toEntity(request, id), requireLoginUserId());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userAddressService.delete(requireLoginUserId(), id);
        return Result.ok();
    }

    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        userAddressService.setDefault(requireLoginUserId(), id);
        return Result.ok();
    }

    private UserAddress toEntity(SaveAddressRequest request, Long id) {
        UserAddress address = new UserAddress();
        address.setId(id);
        address.setName(request.name());
        address.setPhone(request.phone());
        address.setProvince(request.province());
        address.setCity(request.city());
        address.setRegion(request.region());
        address.setDetail(request.detail());
        address.setIsDefault(Boolean.TRUE.equals(request.isDefault()));
        return address;
    }

    private Long requireLoginUserId() {
        LoginUserContext.LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return loginUser.userId();
    }
}
