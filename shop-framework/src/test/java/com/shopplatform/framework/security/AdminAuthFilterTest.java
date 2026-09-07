package com.shopplatform.framework.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 平台超管鉴权过滤器测试。
 * <p>
 * 这组断言是安全红线：`/admin/**` 面向全平台数据，没有租户上下文兜底，
 * 一旦过滤器把无 token / 商户 token 的请求放过去，就是全平台数据裸奔。
 * 历史上这里曾经完全没有过滤器（不带 token 直接 GET /admin/shops 返回 200 + 数据），
 * 所以「未通过鉴权时 filterChain 绝不能被调用」必须有测试钉死。
 */
class AdminAuthFilterTest {

    private final JwtTokenProvider jwtTokenProvider =
            new JwtTokenProvider("test_secret_key_that_is_long_enough_for_hmac_sha256_algorithm", 12);
    private final AdminAuthFilter filter = new AdminAuthFilter(jwtTokenProvider);

    @AfterEach
    void tearDown() {
        LoginUserContext.clear();
    }

    @Test
    @DisplayName("无 Authorization 头：401，且请求不得进入业务逻辑")
    void rejectsMissingToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/shops");
        request.setRequestURI("/admin/shops");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
        assertTrue(response.getContentAsString().contains("10002"));
    }

    @Test
    @DisplayName("token 被篡改：401")
    void rejectsTamperedToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/shops");
        request.setRequestURI("/admin/shops");
        request.addHeader("Authorization", "Bearer not.a.valid.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("用别的密钥签发的 token：401（防止拿其它环境的 token 打进来）")
    void rejectsTokenSignedWithOtherSecret() throws Exception {
        JwtTokenProvider attacker =
                new JwtTokenProvider("a_completely_different_secret_key_used_by_the_attacker_side", 12);
        String forged = attacker.generate("1", Map.of("username", "admin"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/shops");
        request.setRequestURI("/admin/shops");
        request.addHeader("Authorization", "Bearer " + forged);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("★商户后台 token（带 shopId）调平台接口：必须 403，否则任意商户员工可越权访问全平台")
    void rejectsStoreTokenOnAdminApi() throws Exception {
        String storeToken = jwtTokenProvider.generate("100", Map.of("username", "store_admin", "shopId", 42L));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/shops");
        request.setRequestURI("/admin/shops");
        request.addHeader("Authorization", "Bearer " + storeToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("10003"), "应返回 FORBIDDEN 业务码");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("合法平台 token：放行并写入 LoginUserContext")
    void acceptsValidPlatformToken() throws Exception {
        String token = jwtTokenProvider.generate("1", Map.of("username", "admin"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/shops");
        request.setRequestURI("/admin/shops");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // 在 chain 内部断言上下文已就绪：出了 filter 就会被 finally 清理，外面断言不到
        FilterChain chain = (req, res) -> {
            LoginUserContext.LoginUser user = LoginUserContext.get();
            assertNotNull(user, "过滤器应已写入 LoginUserContext");
            assertEquals(1L, user.userId());
            assertEquals("admin", user.username());
            assertNull(user.shopId(), "平台管理员不应带 shopId");
        };

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("登录接口免鉴权，否则没人能登录进来")
    void allowsLoginEndpointWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/auth/login");
        request.setRequestURI("/admin/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(any(), any());
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("请求结束后必须清理 LoginUserContext，否则线程池复用会串号")
    void clearsContextAfterRequest() throws Exception {
        String token = jwtTokenProvider.generate("7", Map.of("username", "ops"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/shops");
        request.setRequestURI("/admin/shops");
        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertNull(LoginUserContext.get(), "过滤器必须在 finally 里清理上下文");
    }

    @Test
    @DisplayName("业务逻辑抛异常时同样要清理上下文")
    void clearsContextWhenChainThrows() {
        String token = jwtTokenProvider.generate("7", Map.of("username", "ops"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/shops");
        request.setRequestURI("/admin/shops");
        request.addHeader("Authorization", "Bearer " + token);

        FilterChain boom = (req, res) -> {
            throw new IllegalStateException("业务异常");
        };

        try {
            filter.doFilter(request, new MockHttpServletResponse(), boom);
        } catch (Exception ignored) {
            // 异常本身由全局异常处理器负责，这里只关心上下文有没有泄漏
        }

        assertNull(LoginUserContext.get());
    }

    @Test
    @DisplayName("Bearer 后面为空串：401（不能当成无 token 放过，也不能 NPE）")
    void rejectsEmptyBearerToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/shops");
        request.setRequestURI("/admin/shops");
        request.addHeader("Authorization", "Bearer ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }
}
