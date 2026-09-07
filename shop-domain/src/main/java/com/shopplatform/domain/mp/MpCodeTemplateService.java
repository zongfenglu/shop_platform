package com.shopplatform.domain.mp;

import com.shopplatform.domain.mp.entity.MpCodeTemplate;

import java.util.List;

public interface MpCodeTemplateService {

    List<MpCodeTemplate> listAll();

    MpCodeTemplate save(String templateId, String userVersion, String userDesc);

    void disable(Long id);
}
