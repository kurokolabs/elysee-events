package de.elyseeevents.portal.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private RateLimiter rateLimiter;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter();
        filter = new RateLimitFilter(rateLimiter);
        ReflectionTestUtils.setField(filter, "trustedProxy", "127.0.0.1");
    }

    private MockHttpServletRequest loginRequest(String ip, String username) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod("POST");
        req.setRequestURI("/portal/login");
        req.setRemoteAddr(ip);
        if (username != null) req.setParameter("username", username);
        return req;
    }

    @Test
    void perIpLoginLimitBlocksAfterFive() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(loginRequest("198.51.100.1", "u" + i + "@example.com"), res, chain);
            assertEquals(200, res.getStatus(), "attempt " + (i + 1) + " under per-IP limit must pass");
        }
        verify(chain, times(5)).doFilter(any(), any());

        // 6th from same IP — different user — must STILL be blocked by per-IP rate limit.
        MockHttpServletResponse res6 = new MockHttpServletResponse();
        FilterChain chain6 = mock(FilterChain.class);
        filter.doFilter(loginRequest("198.51.100.1", "different@example.com"), res6, chain6);
        assertEquals(429, res6.getStatus());
        verifyNoInteractions(chain6);
    }

    @Test
    void perUserLoginLimitBlocksDistributedAttack() throws Exception {
        // Simulate a botnet: 10 distinct IPs each attempt login against the same victim
        // username, staying UNDER the per-IP limit (1 attempt each). Without a per-account
        // limit, all 10 would pass. With it, the 11th attempt against `victim@example.com`
        // — even from a fresh 11th IP — must be blocked.
        FilterChain chain = mock(FilterChain.class);
        for (int i = 1; i <= 10; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(loginRequest("203.0.113." + i, "victim@example.com"), res, chain);
            assertEquals(200, res.getStatus(),
                    "Per-user attempt " + i + " of 10 from IP 203.0.113." + i + " must pass");
        }
        verify(chain, times(10)).doFilter(any(), any());

        // 11th unique IP — per-user counter is now full → block.
        MockHttpServletResponse res11 = new MockHttpServletResponse();
        FilterChain chain11 = mock(FilterChain.class);
        filter.doFilter(loginRequest("203.0.113.99", "victim@example.com"), res11, chain11);
        assertEquals(429, res11.getStatus(),
                "11th per-user attempt must be blocked regardless of source IP");
        verifyNoInteractions(chain11);

        // Sanity: a DIFFERENT username from the same fresh IP must still work.
        MockHttpServletResponse resOther = new MockHttpServletResponse();
        FilterChain chainOther = mock(FilterChain.class);
        filter.doFilter(loginRequest("203.0.113.99", "innocent@example.com"), resOther, chainOther);
        assertEquals(200, resOther.getStatus(), "Different username must not be blocked");
        verify(chainOther).doFilter(any(), any());
    }

    @Test
    void usernameIsNormalisedLowercaseTrimmed() throws Exception {
        // Mixed-case + whitespace variants of the same address must hit the SAME bucket so
        // an attacker cannot bypass the per-user limit by varying capitalisation.
        FilterChain chain = mock(FilterChain.class);
        String[] variants = {
                "VICTIM@example.com",
                "  victim@EXAMPLE.com  ",
                "Victim@Example.Com",
                "victim@example.com",
                "VicTim@example.COM",
                "victim@example.com",
                "victim@example.com",
                "victim@example.com",
                "victim@example.com",
                "victim@example.com"
        };
        for (int i = 0; i < variants.length; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(loginRequest("198.51.100." + (i + 10), variants[i]), res, chain);
            assertEquals(200, res.getStatus(), "variant " + i + " (" + variants[i] + ") under per-user limit");
        }

        MockHttpServletResponse res11 = new MockHttpServletResponse();
        FilterChain chain11 = mock(FilterChain.class);
        filter.doFilter(loginRequest("198.51.100.30", "Victim@Example.Com"), res11, chain11);
        assertEquals(429, res11.getStatus(),
                "case-variant of victim address must hit the same per-user bucket");
        verifyNoInteractions(chain11);
    }

    @Test
    void blankUsernameDoesNotAffectPerUserBucket() throws Exception {
        // Login attempt without `username` parameter (e.g. malformed POST) must not
        // accidentally consume a per-user slot keyed on empty string.
        FilterChain chain = mock(FilterChain.class);
        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(loginRequest("198.51.100.50", null), res, chain);
            assertEquals(200, res.getStatus());
        }
        // Now flood with blank username from another IP — the PER-IP limit will stop us
        // (5 attempts/window), but only on the second IP's 6th attempt; the LOGIN_USER
        // bucket for "" was never created.
        MockHttpServletRequest withWhitespaceOnly = loginRequest("198.51.100.51", "   ");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain c = mock(FilterChain.class);
        filter.doFilter(withWhitespaceOnly, res, c);
        assertEquals(200, res.getStatus(), "whitespace-only username must not block (no per-user bucket)");
        verify(c).doFilter(any(), any());
    }

    @Test
    void nonLoginPostsAreUntouchedByPerUserLimit() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setMethod("POST");
            req.setRequestURI("/portal/register");
            req.setRemoteAddr("198.51.100.60");
            req.setParameter("email", "victim@example.com");

            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, chain);
            assertEquals(200, res.getStatus(), "register attempt " + (i + 1) + " under per-IP limit");
        }
        // 6th register from same IP → blocked by per-IP REGISTER bucket, not LOGIN_USER.
        MockHttpServletRequest req6 = new MockHttpServletRequest();
        req6.setMethod("POST");
        req6.setRequestURI("/portal/register");
        req6.setRemoteAddr("198.51.100.60");
        req6.setParameter("email", "victim@example.com");
        MockHttpServletResponse res6 = new MockHttpServletResponse();
        FilterChain chain6 = mock(FilterChain.class);
        filter.doFilter(req6, res6, chain6);
        assertEquals(429, res6.getStatus());

        // And a LOGIN attempt for the same victim from a different IP must still pass,
        // because register attempts didn't poison LOGIN_USER:victim@example.com.
        MockHttpServletResponse loginRes = new MockHttpServletResponse();
        FilterChain loginChain = mock(FilterChain.class);
        filter.doFilter(loginRequest("198.51.100.61", "victim@example.com"), loginRes, loginChain);
        assertEquals(200, loginRes.getStatus(),
                "register-action attempts must not consume LOGIN_USER slots");
        verify(loginChain).doFilter(any(), any());
    }
}
