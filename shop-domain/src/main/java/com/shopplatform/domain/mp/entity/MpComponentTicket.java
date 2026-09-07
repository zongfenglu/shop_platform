package com.shopplatform.domain.mp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopplatform.framework.mybatis.BaseEntity;

import java.time.LocalDateTime;

@TableName("mp_component_ticket")
public class MpComponentTicket extends BaseEntity {

    private String ticket;

    private LocalDateTime receivedTime;

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public LocalDateTime getReceivedTime() {
        return receivedTime;
    }

    public void setReceivedTime(LocalDateTime receivedTime) {
        this.receivedTime = receivedTime;
    }
}
