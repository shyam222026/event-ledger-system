package com.example.eventgateway.config;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

@Component
public class TracingClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String traceId = Objects.requireNonNullElse(MDC.get("traceId"), "n/a");
        request.getHeaders().add("X-Trace-Id", traceId);
        request.getHeaders().add("traceparent", "00-" + traceId + "-" + traceId.substring(0, 16) + "-01");
        return execution.execute(request, body);
    }
}
