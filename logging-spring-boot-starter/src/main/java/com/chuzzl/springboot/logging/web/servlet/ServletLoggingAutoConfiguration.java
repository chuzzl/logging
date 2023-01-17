package com.chuzzl.springboot.logging.web.servlet;

import com.chuzzl.springboot.logging.web.HttpMessageFormatter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import com.chuzzl.springboot.logging.api.MessageFormatter;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ServletLoggingAutoConfiguration {

  @Bean(name = "servletHttpMessageFormatter")
  @ConditionalOnMissingBean(name = "servletHttpMessageFormatter")
  MessageFormatter servletHttpMessageFormatter(@Qualifier("messageFormatter") Optional<MessageFormatter> maybeGlobalFormatter) {
    return maybeGlobalFormatter.orElse(new HttpMessageFormatter(Arrays.asList("Authorization")));
  }

  @Bean(name = "disabledLoggingServletRequestsFilter")
  @ConditionalOnProperty("logging.servlet.paths.disable")
  @ConditionalOnMissingBean
  HttpRequestsFilter disabledLoggingServletRequestsFilter(@Value("${logging.servlet.paths.disable}") List<String> paths) {
    @SuppressWarnings("unchecked") Predicate<HttpServletRequest>[] predicates = paths.stream()
      .<Predicate<ServerHttpRequest>>map(path -> req -> req.getPath().pathWithinApplication().value().startsWith(path))
      .toArray(Predicate[]::new);
    return new HttpRequestsFilter(predicates);
  }

  @Bean(name = "disabledLoggingServletRequestsFilter")
  @ConditionalOnMissingBean
  HttpRequestsFilter defaultDisabledLoggingServletRequestsFilter() {
    return new HttpRequestsFilter(
      req -> req.getRequestURI().startsWith("/health"),
      req -> req.getRequestURI().startsWith("/metrics")
    );
  }

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE + 10)
  Filter loggingFilter() {
    return new LoggingFilter();
  }
}
