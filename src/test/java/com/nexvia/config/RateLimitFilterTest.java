package com.nexvia.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private RateLimitFilter rateLimitFilter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        rateLimitFilter = new RateLimitFilter();
        filterChain = mock(FilterChain.class);
    }

    @Test
    void nonAuthEndpoint_passesThrough() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/camiones");
        var response = new MockHttpServletResponse();

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void loginEndpoint_firstRequest_passes() throws Exception {
        rateLimitFilter.clearClients();
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("127.0.0.1");
        var response = new MockHttpServletResponse();

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void loginEndpoint_exceedsLimit_returns429() throws Exception {
        rateLimitFilter.clearClients();

        for (int i = 0; i < 11; i++) {
            var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.setRemoteAddr("10.0.0.1");
            var response = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            if (i < 10) {
                assertThat(response.getStatus()).isEqualTo(200);
            } else {
                assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_GONE + 19);
            }
        }
    }

    @Test
    void registerEndpoint_rateLimited() throws Exception {
        rateLimitFilter.clearClients();

        for (int i = 0; i < 11; i++) {
            var request = new MockHttpServletRequest("POST", "/api/v1/auth/register");
            request.setRemoteAddr("10.0.0.2");
            var response = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request, response, filterChain);
        }

        var request = new MockHttpServletRequest("POST", "/api/v1/auth/register");
        request.setRemoteAddr("10.0.0.2");
        var response = new MockHttpServletResponse();
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void forwardedHeader_usesFirstIp() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.addHeader("X-Forwarded-For", "192.168.1.1, 10.0.0.1");

        String ip = rateLimitFilter.getClientIp(request);

        assertThat(ip).isEqualTo("192.168.1.1");
    }

    @Test
    void noForwardedHeader_usesRemoteAddr() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("127.0.0.1");

        String ip = rateLimitFilter.getClientIp(request);

        assertThat(ip).isEqualTo("127.0.0.1");
    }
}
