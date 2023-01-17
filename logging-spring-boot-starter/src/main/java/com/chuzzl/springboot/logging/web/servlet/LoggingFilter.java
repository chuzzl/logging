package com.chuzzl.springboot.logging.web.servlet;

import brave.Span;
import brave.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.StreamUtils;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import com.chuzzl.springboot.logging.api.Message;
import com.chuzzl.springboot.logging.api.MessageFormatter;
import com.chuzzl.springboot.logging.api.OneLineFormatter;
import com.chuzzl.springboot.logging.web.HttpMessage;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class LoggingFilter implements Filter {
  private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);
  @Autowired
  @Qualifier("disabledLoggingServletRequestsFilter")
  private HttpRequestsFilter disabledLoggingRequestsFilter;
  @Autowired(required = false)
  @Qualifier("servletHttpMessageFormatter")
  private MessageFormatter messageFormatter = new OneLineFormatter();

  @Autowired(required = false)
  private Tracer tracer;


  private static Map<String, String> getRequestHeaders(HttpServletRequest request) {
    Map<String, String> headers = new HashMap<>();
    Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String headerName = headerNames.nextElement();
      headers.put(headerName, request.getHeader(headerName));
    }
    return headers;
  }

  private static Map<String, String> getResponseHeaders(HttpServletResponse response) {
    Map<String, String> headers = new HashMap<>();
    Collection<String> headerNames = response.getHeaderNames();
    for (String headerName : headerNames) {
      headers.put(headerName, response.getHeader(headerName));
    }
    return headers;
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
    throws IOException, ServletException {
    ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper((HttpServletRequest) servletRequest);
    ContentCachingResponseWrapper cachedResponse =
      new ContentCachingResponseWrapper((HttpServletResponse) servletResponse);

    boolean logExchange = !disabledLoggingRequestsFilter.match(cachedRequest);

    Span span = null;
    if (tracer != null) {
      span = tracer.currentSpan();
    }

    if (log.isInfoEnabled() && logExchange) {
      StringBuffer requestUrl = cachedRequest.getRequestURL()
        .append(cachedRequest.getQueryString() != null ? "?" + cachedRequest.getQueryString() : "");
      HttpMessage message = new HttpMessage(
        Message.Type.INBOUND,
        String.format("%s %s", cachedRequest.getMethod(), requestUrl.toString()),
        null,
        getRequestHeaders(cachedRequest),
        StreamUtils.copyToString(cachedRequest.getInputStream(), Charset.defaultCharset())
      );
      log.info("{}", messageFormatter.format(message));
    }
    try {
      InputStream cachedBodyInputStream = new ByteArrayInputStream(cachedRequest.getContentAsByteArray());
      filterChain.doFilter(new HttpServletRequestWrapper(cachedRequest) {
        @Override
        public ServletInputStream getInputStream() {
          return new ServletInputStream() {

            @Override
            public boolean isFinished() {
              try {
                return cachedBodyInputStream.available() == 0;
              } catch (IOException ignore) {
              }
              return false;
            }

            @Override
            public boolean isReady() {
              return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
              throw new UnsupportedOperationException();
            }

            @Override
            public int read() throws IOException {
              return cachedBodyInputStream.read();
            }
          };
        }
      }, cachedResponse);
    } finally {
      if (WebAsyncUtils.getAsyncManager(servletRequest).isConcurrentHandlingStarted()) {
        Span finalSpan = span;
        servletRequest.getAsyncContext().addListener(new AsyncListener() {
          @Override
          public void onComplete(AsyncEvent asyncEvent) throws IOException {
            if (finalSpan != null) {
              try (Tracer.SpanInScope sis = tracer.withSpanInScope(finalSpan)) {
                maybeLogResponse(cachedResponse, logExchange);
              }
            }
          }

          @Override
          public void onTimeout(AsyncEvent asyncEvent) throws IOException {
            if (finalSpan != null) {
              try (Tracer.SpanInScope sis = tracer.withSpanInScope(finalSpan)) {
                maybeLogResponse(cachedResponse, logExchange);
              }
            }
          }

          @Override
          public void onError(AsyncEvent asyncEvent) throws IOException {
            if (finalSpan != null) {
              try (Tracer.SpanInScope sis = tracer.withSpanInScope(finalSpan)) {
                maybeLogResponse(cachedResponse, logExchange);
              }
            }
          }

          @Override
          public void onStartAsync(AsyncEvent asyncEvent) throws IOException {
            if (finalSpan != null) {
              try (Tracer.SpanInScope sis = tracer.withSpanInScope(finalSpan)) {
                maybeLogResponse(cachedResponse, logExchange);
              }
            }
          }
        });
      } else {
        maybeLogResponse(cachedResponse, logExchange);
      }
    }
  }

  private void maybeLogResponse(ContentCachingResponseWrapper cachedResponse, boolean logExchange) throws IOException {
    if (log.isInfoEnabled() && logExchange) {
      HttpMessage message = new HttpMessage(
        Message.Type.OUTBOUND,
        String.format("HTTP %s", cachedResponse.getStatus()),
        null,
        getResponseHeaders(cachedResponse),
        new String(cachedResponse.getContentAsByteArray(), Charset.defaultCharset())
      );
      log.info("{}", messageFormatter.format(message));
    }
    cachedResponse.copyBodyToResponse();
  }

  @Override
  public void destroy() {

  }

  @Override
  public void init(FilterConfig filterConfig) {

  }

}