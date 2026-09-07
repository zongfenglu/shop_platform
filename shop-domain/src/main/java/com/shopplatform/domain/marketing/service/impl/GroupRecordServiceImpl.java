package com.shopplatform.domain.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.marketing.entity.GroupRecord;
import com.shopplatform.domain.marketing.mapper.GroupRecordMapper;
import com.shopplatform.domain.marketing.service.GroupRecordService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GroupRecordServiceImpl extends ServiceImpl<GroupRecordMapper, GroupRecord> implements GroupRecordService {

    @Override
    public GroupRecord openGroup(Long activeId, Long leaderUserId, Long leaderOrderId, int validHours) {
        GroupRecord r = new GroupRecord();
        r.setActiveId(activeId);
        r.setLeaderUserId(leaderUserId);
        r.setLeaderOrderId(leaderOrderId);
        r.setStatus("pending");
        r.setActualNum(1);
        LocalDateTime now = LocalDateTime.now();
        r.setExpireTime(now.plusHours(validHours));
        save(r);
        return r;
    }

    @Override
    public GroupRecord joinGroup(Long recordId, int groupNum) {
        int affected = baseMapper.joinGroup(recordId, 1, groupNum);
        if (affected == 0) {
            return null;
        }
        GroupRecord r = getByIdWithTenant(recordId);
        if ("pending".equals(r.getStatus()) && r.getActualNum() != null && r.getActualNum() >= groupNum) {
            r.setStatus("success");
            r.setSuccessTime(LocalDateTime.now());
            updateById(r);
        }
        return r;
    }

    @Override
    public List<GroupRecord> listExpiredPending(LocalDateTime now) {
        return list(new LambdaQueryWrapper<GroupRecord>()
                .eq(GroupRecord::getStatus, "pending")
                .lt(GroupRecord::getExpireTime, now));
    }

    @Override
    public void markFail(Long recordId) {
        GroupRecord r = getByIdWithTenant(recordId);
        if ("pending".equals(r.getStatus())) {
            r.setStatus("fail");
            updateById(r);
        }
    }

    @Override
    public void setLeaderOrder(Long recordId, Long orderId) {
        GroupRecord r = getByIdWithTenant(recordId);
        r.setLeaderOrderId(orderId);
        updateById(r);
    }
}
