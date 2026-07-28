package br.com.systemcommerce.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldReuseIncomingCorrelationIdHeader() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(CorrelationIdConstants.HEADER)).thenReturn("fixed-correlation-id");

        filter.doFilter(request, response, chain);

        verify(request).setAttribute(CorrelationIdConstants.REQUEST_ATTRIBUTE, "fixed-correlation-id");
        verify(response).setHeader(CorrelationIdConstants.HEADER, "fixed-correlation-id");
        verify(chain).doFilter(request, response);
        assertThat(MDC.get(CorrelationIdConstants.MDC_KEY)).isNull();
    }

    @Test
    void shouldGenerateCorrelationIdWhenMissing() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(CorrelationIdConstants.HEADER)).thenReturn(null);
        when(request.getAttribute(CorrelationIdConstants.REQUEST_ATTRIBUTE)).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(response).setHeader(eq(CorrelationIdConstants.HEADER), any(String.class));
        verify(chain).doFilter(request, response);
    }
}
