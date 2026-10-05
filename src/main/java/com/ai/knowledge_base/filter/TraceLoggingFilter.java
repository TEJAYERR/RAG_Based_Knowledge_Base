package com.ai.knowledge_base.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class TraceLoggingFilter implements Filter {

    public static final String TRACE_ID_KEY = "traceId";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        try{

            HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
            String traceId = httpServletRequest.getHeader("X-Trace-Id");

            if(traceId == null || traceId.isBlank()){
                traceId = UUID.randomUUID().toString().substring(0, 8);
            }

            MDC.put(TRACE_ID_KEY, traceId);
            filterChain.doFilter(servletRequest, servletResponse);
        }
        finally {
            MDC.clear();
        }
    }
}
