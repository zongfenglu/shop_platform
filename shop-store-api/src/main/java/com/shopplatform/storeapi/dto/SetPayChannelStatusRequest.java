package com.shopplatform.storeapi.dto;

import jakarta.validation.constraints.NotNull;

public record SetPayChannelStatusRequest(@NotNull(message = "启用状态不能为空") Boolean enabled) {
}
