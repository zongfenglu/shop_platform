package com.shopplatform.framework.security;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.framework.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientTenantFilterTest {

    private final JwtTokenProvider jwtTokenProvider =
            new JwtTokenProvider("test_secret_key_that_is_long_enough_for_hmac_sha256_algorithm", 12);
    private final ClientTenantFilter.ShopResolver shopResolver = mock(ClientTenantFilter.ShopResolver.class);
    private final ClientTenantFilter filter = new ClientTenantFilter(jwtTokenProvider, shopResolver);

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        LoginUserContext.clear();
    }

    @Test
    void missingTenant_returnsBusinessResponseInsteadOfHttp500() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/diy/home");
        request.setServerName("h5.shop.test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(shopResolver.resolveByHost("h5.shop.test")).thenReturn(null);

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":20000"));
        verify(chain, never()).doFilter(any(), any());
        assertNull(TenantContext.get());
    }

    @Test
    void explicitShopHeader_setsTenantForRequestAndClearsItAfterward() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/diy/home");
        request.addHeader("X-Shop-Id", "2097131103839346689");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> assertEquals(2097131103839346689L, TenantContext.getRequired());
        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(shopResolver).ensureAccessible(2097131103839346689L);
        assertNull(TenantContext.get());
    }

    @Test
    void inaccessibleShop_returnsItsBusinessCode() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/diy/home");
        request.addHeader("X-Shop-Id", "42");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.TENANT_DISABLED))
                .when(shopResolver).ensureAccessible(42L);

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":20002"));
        verify(chain, never()).doFilter(any(), any());
    }
}
