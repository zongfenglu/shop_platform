package com.shopplatform.storeapi.controller;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.setting.entity.ExpressCompany;
import com.shopplatform.domain.setting.entity.ReceiptPrinter;
import com.shopplatform.domain.setting.entity.ReturnAddress;
import com.shopplatform.domain.setting.entity.SmsChannel;
import com.shopplatform.domain.setting.entity.StoreOperationSetting;
import com.shopplatform.domain.setting.service.ExpressCompanyService;
import com.shopplatform.domain.setting.service.ReceiptPrinterService;
import com.shopplatform.domain.setting.service.ReturnAddressService;
import com.shopplatform.domain.setting.service.SmsChannelService;
import com.shopplatform.domain.setting.service.StoreOperationSettingService;
import com.shopplatform.framework.crypto.AesGcmEncryptor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/store/settings")
public class StoreOperationSettingController {

    private static final Set<String> STORAGE_PROVIDERS = Set.of("local");
    private static final Set<String> PRINTER_PROVIDERS = Set.of("feie", "yilianyun", "cloud", "custom_http");
    private static final Set<String> SMS_PROVIDERS = Set.of("aliyun", "tencent", "huawei", "yunpian", "custom_http");

    private final ExpressCompanyService expressCompanyService;
    private final ReturnAddressService returnAddressService;
    private final ReceiptPrinterService receiptPrinterService;
    private final SmsChannelService smsChannelService;
    private final StoreOperationSettingService settingService;
    private final AesGcmEncryptor encryptor;

    public StoreOperationSettingController(ExpressCompanyService expressCompanyService,
                                           ReturnAddressService returnAddressService,
                                           ReceiptPrinterService receiptPrinterService,
                                           SmsChannelService smsChannelService,
                                           StoreOperationSettingService settingService,
                                           AesGcmEncryptor encryptor) {
        this.expressCompanyService = expressCompanyService;
        this.returnAddressService = returnAddressService;
        this.receiptPrinterService = receiptPrinterService;
        this.smsChannelService = smsChannelService;
        this.settingService = settingService;
        this.encryptor = encryptor;
    }

    @GetMapping("/express-companies")
    public Result<List<ExpressCompany>> listExpressCompanies() {
        return Result.ok(expressCompanyService.listAllWithDefaults());
    }

    @GetMapping("/express-companies/enabled")
    public Result<List<ExpressCompany>> listEnabledExpressCompanies() {
        return Result.ok(expressCompanyService.listEnabled());
    }

    @PostMapping("/express-companies")
    public Result<ExpressCompany> createExpressCompany(@Valid @RequestBody ExpressRequest request) {
        ExpressCompany item = new ExpressCompany();
        applyExpress(item, request);
        expressCompanyService.save(item);
        return Result.ok(item);
    }

    @PutMapping("/express-companies/{id}")
    public Result<ExpressCompany> updateExpressCompany(@PathVariable Long id,
                                                       @Valid @RequestBody ExpressRequest request) {
        ExpressCompany item = expressCompanyService.getByIdWithTenant(id);
        applyExpress(item, request);
        expressCompanyService.updateById(item);
        return Result.ok(item);
    }

    @DeleteMapping("/express-companies/{id}")
    public Result<Void> deleteExpressCompany(@PathVariable Long id) {
        expressCompanyService.getByIdWithTenant(id);
        expressCompanyService.removeById(id);
        return Result.ok();
    }

    @GetMapping("/return-addresses")
    public Result<List<ReturnAddress>> listReturnAddresses() {
        return Result.ok(returnAddressService.listAll());
    }

    @PostMapping("/return-addresses")
    public Result<ReturnAddress> createReturnAddress(@Valid @RequestBody ReturnAddressRequest request) {
        ReturnAddress item = new ReturnAddress();
        applyReturnAddress(item, request);
        returnAddressService.saveAsDefault(item);
        return Result.ok(item);
    }

    @PutMapping("/return-addresses/{id}")
    public Result<ReturnAddress> updateReturnAddress(@PathVariable Long id,
                                                     @Valid @RequestBody ReturnAddressRequest request) {
        ReturnAddress item = returnAddressService.getByIdWithTenant(id);
        applyReturnAddress(item, request);
        returnAddressService.saveAsDefault(item);
        return Result.ok(item);
    }

    @PutMapping("/return-addresses/{id}/default")
    public Result<Void> setDefaultReturnAddress(@PathVariable Long id) {
        returnAddressService.setDefault(id);
        return Result.ok();
    }

    @DeleteMapping("/return-addresses/{id}")
    public Result<Void> deleteReturnAddress(@PathVariable Long id) {
        ReturnAddress item = returnAddressService.getByIdWithTenant(id);
        returnAddressService.removeById(id);
        if (Boolean.TRUE.equals(item.getIsDefault())) {
            List<ReturnAddress> remaining = returnAddressService.listAll();
            if (!remaining.isEmpty()) returnAddressService.setDefault(remaining.get(0).getId());
        }
        return Result.ok();
    }

    @GetMapping("/printers")
    public Result<List<PrinterView>> listPrinters() {
        return Result.ok(receiptPrinterService.listAll().stream().map(this::printerView).toList());
    }

    @PostMapping("/printers")
    public Result<PrinterView> createPrinter(@Valid @RequestBody PrinterRequest request) {
        ReceiptPrinter item = new ReceiptPrinter();
        applyPrinter(item, request);
        receiptPrinterService.save(item);
        return Result.ok(printerView(item));
    }

    @PutMapping("/printers/{id}")
    public Result<PrinterView> updatePrinter(@PathVariable Long id, @Valid @RequestBody PrinterRequest request) {
        ReceiptPrinter item = receiptPrinterService.getByIdWithTenant(id);
        applyPrinter(item, request);
        receiptPrinterService.updateById(item);
        return Result.ok(printerView(item));
    }

    @DeleteMapping("/printers/{id}")
    public Result<Void> deletePrinter(@PathVariable Long id) {
        receiptPrinterService.getByIdWithTenant(id);
        StoreOperationSetting setting = settingService.getOrCreate();
        if (id.equals(setting.getPrintPrinterId())) {
            setting.setPrintEnabled(false);
            setting.setPrintPrinterId(null);
            settingService.updateById(setting);
        }
        receiptPrinterService.removeById(id);
        return Result.ok();
    }

    @GetMapping("/sms-channels")
    public Result<List<SmsChannelView>> listSmsChannels() {
        return Result.ok(smsChannelService.listAll().stream().map(this::smsChannelView).toList());
    }

    @PostMapping("/sms-channels")
    public Result<SmsChannelView> createSmsChannel(@Valid @RequestBody SmsChannelRequest request) {
        SmsChannel item = new SmsChannel();
        applySmsChannel(item, request);
        smsChannelService.save(item);
        return Result.ok(smsChannelView(item));
    }

    @PutMapping("/sms-channels/{id}")
    public Result<SmsChannelView> updateSmsChannel(@PathVariable Long id,
                                                   @Valid @RequestBody SmsChannelRequest request) {
        SmsChannel item = smsChannelService.getByIdWithTenant(id);
        applySmsChannel(item, request);
        smsChannelService.updateById(item);
        return Result.ok(smsChannelView(item));
    }

    @DeleteMapping("/sms-channels/{id}")
    public Result<Void> deleteSmsChannel(@PathVariable Long id) {
        smsChannelService.getByIdWithTenant(id);
        smsChannelService.removeById(id);
        return Result.ok();
    }

    @GetMapping("/operations")
    public Result<OperationSettingView> getOperationSetting() {
        return Result.ok(operationView(settingService.getOrCreate()));
    }

    @PutMapping("/upload")
    public Result<OperationSettingView> saveUploadSetting(@Valid @RequestBody UploadSettingRequest request) {
        requireOneOf(request.provider(), STORAGE_PROVIDERS, "不支持的存储渠道");
        StoreOperationSetting item = settingService.getOrCreate();
        item.setUploadProvider(request.provider());
        item.setUploadBucket(trimToNull(request.bucket()));
        item.setUploadRegion(trimToNull(request.region()));
        item.setUploadEndpoint(trimToNull(request.endpoint()));
        item.setUploadDomain(normalizeDomain(request.domain()));
        item.setImageMaxMb(request.imageMaxMb());
        item.setVideoMaxMb(request.videoMaxMb());
        if (StringUtils.hasText(request.accessKeyId())) {
            item.setUploadAccessKeyIdEncrypted(encryptor.encrypt(request.accessKeyId().trim()));
        }
        if (StringUtils.hasText(request.accessKeySecret())) {
            item.setUploadAccessKeySecretEncrypted(encryptor.encrypt(request.accessKeySecret().trim()));
        }
        settingService.updateById(item);
        return Result.ok(operationView(item));
    }

    @PutMapping("/print-rules")
    public Result<OperationSettingView> savePrintRules(@Valid @RequestBody PrintRuleRequest request) {
        StoreOperationSetting item = settingService.getOrCreate();
        if (Boolean.TRUE.equals(request.enabled())) {
            if (request.printerId() == null) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "开启自动打印前请选择打印机");
            }
            ReceiptPrinter printer = receiptPrinterService.getByIdWithTenant(request.printerId());
            if (!"enabled".equals(printer.getStatus())) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "所选打印机已停用");
            }
        }
        item.setPrintEnabled(request.enabled());
        item.setPrintPrinterId(request.printerId());
        item.setPrintOnPaid(request.onPaid());
        item.setPrintOnRefund(request.onRefund());
        item.setPrintCopies(request.copies());
        settingService.updateById(item);
        return Result.ok(operationView(item));
    }

    @PutMapping("/sms-rules")
    public Result<OperationSettingView> saveSmsRules(@Valid @RequestBody SmsRuleRequest request) {
        if (Boolean.TRUE.equals(request.enabled())
                && smsChannelService.listAll().stream().noneMatch(c -> "enabled".equals(c.getStatus()))) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "开启短信前请先配置并启用一个短信渠道");
        }
        StoreOperationSetting item = settingService.getOrCreate();
        item.setSmsEnabled(request.enabled());
        item.setSmsNewOrderTemplate(trimToNull(request.newOrderTemplate()));
        item.setSmsPaidTemplate(trimToNull(request.paidTemplate()));
        item.setSmsShippedTemplate(trimToNull(request.shippedTemplate()));
        item.setSmsRefundTemplate(trimToNull(request.refundTemplate()));
        item.setSmsNotifyPhones(trimToNull(request.notifyPhones()));
        settingService.updateById(item);
        return Result.ok(operationView(item));
    }

    private void applyExpress(ExpressCompany item, ExpressRequest request) {
        item.setName(request.name().trim());
        item.setCode(request.code().trim().toLowerCase());
        item.setSort(request.sort());
        item.setStatus(request.enabled() ? "enabled" : "disabled");
    }

    private void applyReturnAddress(ReturnAddress item, ReturnAddressRequest request) {
        item.setContactName(request.contactName().trim());
        item.setPhone(request.phone().trim());
        item.setProvince(request.province().trim());
        item.setCity(request.city().trim());
        item.setDistrict(request.district().trim());
        item.setDetail(request.detail().trim());
        item.setPostalCode(trimToNull(request.postalCode()));
        item.setIsDefault(request.isDefault());
        item.setSort(request.sort());
        item.setStatus(request.enabled() ? "enabled" : "disabled");
    }

    private void applyPrinter(ReceiptPrinter item, PrinterRequest request) {
        requireOneOf(request.provider(), PRINTER_PROVIDERS, "不支持的打印机渠道");
        item.setName(request.name().trim());
        item.setProvider(request.provider());
        item.setDeviceNo(request.deviceNo().trim());
        item.setEndpoint(trimToNull(request.endpoint()));
        item.setSort(request.sort());
        item.setStatus(request.enabled() ? "enabled" : "disabled");
        if (StringUtils.hasText(request.accessKey())) item.setAccessKeyEncrypted(encryptor.encrypt(request.accessKey().trim()));
        if (StringUtils.hasText(request.accessSecret())) item.setAccessSecretEncrypted(encryptor.encrypt(request.accessSecret().trim()));
    }

    private void applySmsChannel(SmsChannel item, SmsChannelRequest request) {
        requireOneOf(request.provider(), SMS_PROVIDERS, "不支持的短信渠道");
        item.setName(request.name().trim());
        item.setProvider(request.provider());
        item.setAppId(trimToNull(request.appId()));
        item.setSignName(trimToNull(request.signName()));
        item.setEndpoint(trimToNull(request.endpoint()));
        item.setPriority(request.priority());
        item.setStatus(request.enabled() ? "enabled" : "disabled");
        if (StringUtils.hasText(request.accessKeyId())) item.setAccessKeyIdEncrypted(encryptor.encrypt(request.accessKeyId().trim()));
        if (StringUtils.hasText(request.accessKeySecret())) item.setAccessKeySecretEncrypted(encryptor.encrypt(request.accessKeySecret().trim()));
    }

    private PrinterView printerView(ReceiptPrinter item) {
        return new PrinterView(item.getId(), item.getName(), item.getProvider(), item.getDeviceNo(), item.getEndpoint(),
                item.getSort(), item.getStatus(), StringUtils.hasText(item.getAccessKeyEncrypted()),
                StringUtils.hasText(item.getAccessSecretEncrypted()), item.getCreateTime());
    }

    private SmsChannelView smsChannelView(SmsChannel item) {
        return new SmsChannelView(item.getId(), item.getName(), item.getProvider(), item.getAppId(), item.getSignName(),
                item.getEndpoint(), item.getPriority(), item.getStatus(),
                StringUtils.hasText(item.getAccessKeyIdEncrypted()), StringUtils.hasText(item.getAccessKeySecretEncrypted()),
                item.getCreateTime());
    }

    private OperationSettingView operationView(StoreOperationSetting item) {
        return new OperationSettingView(item.getUploadProvider(), item.getUploadBucket(), item.getUploadRegion(),
                item.getUploadEndpoint(), item.getUploadDomain(), item.getImageMaxMb(), item.getVideoMaxMb(),
                StringUtils.hasText(item.getUploadAccessKeyIdEncrypted()),
                StringUtils.hasText(item.getUploadAccessKeySecretEncrypted()), item.getPrintEnabled(),
                item.getPrintPrinterId(), item.getPrintOnPaid(), item.getPrintOnRefund(), item.getPrintCopies(),
                item.getSmsEnabled(), item.getSmsNewOrderTemplate(), item.getSmsPaidTemplate(),
                item.getSmsShippedTemplate(), item.getSmsRefundTemplate(), item.getSmsNotifyPhones());
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String normalizeDomain(String value) {
        String domain = trimToNull(value);
        if (domain == null) return null;
        if (!domain.matches("^https?://[^\\s/]+(?:/[^\\s]*)?$")) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "访问域名必须以 http:// 或 https:// 开头");
        }
        return domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain;
    }

    private static void requireOneOf(String value, Set<String> allowed, String message) {
        if (!allowed.contains(value)) throw new BusinessException(ErrorCode.PARAM_INVALID, message);
    }

    public record ExpressRequest(
            @NotBlank @Size(max = 64) String name,
            @NotBlank @Pattern(regexp = "^[a-z0-9_-]{2,32}$") String code,
            @NotNull @Min(0) @Max(9999) Integer sort,
            boolean enabled) {}

    public record ReturnAddressRequest(
            @NotBlank @Size(max = 64) String contactName,
            @NotBlank @Pattern(regexp = "^[0-9+() -]{6,20}$") String phone,
            @NotBlank @Size(max = 32) String province,
            @NotBlank @Size(max = 32) String city,
            @NotBlank @Size(max = 32) String district,
            @NotBlank @Size(max = 255) String detail,
            @Size(max = 12) String postalCode,
            boolean isDefault,
            @NotNull @Min(0) @Max(9999) Integer sort,
            boolean enabled) {}

    public record PrinterRequest(
            @NotBlank @Size(max = 64) String name,
            @NotBlank String provider,
            @NotBlank @Size(max = 128) String deviceNo,
            @Size(max = 512) String accessKey,
            @Size(max = 1024) String accessSecret,
            @Size(max = 255) String endpoint,
            @NotNull @Min(0) @Max(9999) Integer sort,
            boolean enabled) {}

    public record SmsChannelRequest(
            @NotBlank @Size(max = 64) String name,
            @NotBlank String provider,
            @Size(max = 128) String appId,
            @Size(max = 512) String accessKeyId,
            @Size(max = 1024) String accessKeySecret,
            @Size(max = 64) String signName,
            @Size(max = 255) String endpoint,
            @NotNull @Min(0) @Max(9999) Integer priority,
            boolean enabled) {}

    public record UploadSettingRequest(
            @NotBlank String provider,
            @Size(max = 128) String bucket,
            @Size(max = 64) String region,
            @Size(max = 255) String endpoint,
            @Size(max = 255) String domain,
            @Size(max = 512) String accessKeyId,
            @Size(max = 1024) String accessKeySecret,
            @NotNull @Min(1) @Max(20) Integer imageMaxMb,
            @NotNull @Min(1) @Max(50) Integer videoMaxMb) {}

    public record PrintRuleRequest(
            @NotNull Boolean enabled,
            Long printerId,
            @NotNull Boolean onPaid,
            @NotNull Boolean onRefund,
            @NotNull @Min(1) @Max(5) Integer copies) {}

    public record SmsRuleRequest(
            @NotNull Boolean enabled,
            @Size(max = 128) String newOrderTemplate,
            @Size(max = 128) String paidTemplate,
            @Size(max = 128) String shippedTemplate,
            @Size(max = 128) String refundTemplate,
            @Size(max = 512) String notifyPhones) {}

    public record PrinterView(Long id, String name, String provider, String deviceNo, String endpoint,
                              Integer sort, String status, boolean accessKeySet, boolean accessSecretSet,
                              java.time.LocalDateTime createTime) {}

    public record SmsChannelView(Long id, String name, String provider, String appId, String signName,
                                 String endpoint, Integer priority, String status, boolean accessKeyIdSet,
                                 boolean accessKeySecretSet, java.time.LocalDateTime createTime) {}

    public record OperationSettingView(String uploadProvider, String uploadBucket, String uploadRegion,
                                       String uploadEndpoint, String uploadDomain, Integer imageMaxMb,
                                       Integer videoMaxMb, boolean uploadAccessKeyIdSet,
                                       boolean uploadAccessKeySecretSet, Boolean printEnabled,
                                       Long printPrinterId, Boolean printOnPaid, Boolean printOnRefund,
                                       Integer printCopies, Boolean smsEnabled, String smsNewOrderTemplate,
                                       String smsPaidTemplate, String smsShippedTemplate,
                                       String smsRefundTemplate, String smsNotifyPhones) {}
}
