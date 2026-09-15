package com.shopplatform.framework.web;

import com.shopplatform.common.result.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    @Test
    void typeMismatch_isReportedAsInvalidParameterInsteadOfSystemError() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var result = handler.handleParamInvalid(mock(MethodArgumentTypeMismatchException.class));

        assertEquals(ErrorCode.PARAM_INVALID.getCode(), result.getCode());
        assertEquals(ErrorCode.PARAM_INVALID.getMsg(), result.getMsg());
    }
}
