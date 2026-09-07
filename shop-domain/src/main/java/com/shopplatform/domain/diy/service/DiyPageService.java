package com.shopplatform.domain.diy.service;

import com.shopplatform.domain.diy.entity.DiyPage;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface DiyPageService extends TenantSafeService<DiyPage> {

    /** 当前商城全部装修页面（home+custom）。 */
    List<DiyPage> list();

    DiyPage createPage(CreatePageCommand command);

    /** 保存草稿内容，不影响已发布快照。 */
    DiyPage updateDraft(Long id, String draftDataJson);

    /** 校验草稿内容合法后发布：page_data=draft_data，version自增，publish_time刷新。 */
    DiyPage publish(Long id);

    /** 复制页面：内容取源页已发布快照（无则取草稿），新页始终从未发布过。 */
    DiyPage copyPage(Long id, String newName);

    /** 设为首页：同shop_id下home类型至多一条is_default=1。 */
    void setHome(Long id);

    void delete(Long id);

    /** 套用行业模板，创建一个新的custom草稿页。 */
    DiyPage applyTemplate(Long templateId, String name);

    /** 按shopId查当前首页（无租户上下文场景，供消费者端渲染API使用）；不存在返回null。 */
    DiyPage getDefaultHome(Long shopId);

    record CreatePageCommand(String name, String pageType, Long copyFromPageId) {
    }
}
