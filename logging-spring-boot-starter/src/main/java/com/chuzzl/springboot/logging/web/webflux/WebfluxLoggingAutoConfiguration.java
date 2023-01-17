package com.chuzzl.springboot.logging.web.webflux;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.sleuth.instrument.web.TraceWebAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.WebFilter;
import com.chuzzl.springboot.logging.api.MessageFormatter;
import com.chuzzl.springboot.logging.web.HttpMessageFormatter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;


@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@AutoConfigureAfter(TraceWebAutoConfiguration.class)
public class WebfluxLoggingAutoConfiguration {
  @Bean
  WebFilter loggingWebFilter() {
    return new LoggingWebFilter();
  }

  @Bean(name = "disabledLoggingRequestsFilter")
  @ConditionalOnProperty("logging.webflux.paths.disable")
  @ConditionalOnMissingBean
  HttpRequestsFilter disabledLoggingRequestsFilter(@Value("${logging.webflux.paths.disable}") List<String> paths) {
    @SuppressWarnings("unchecked") Predicate<ServerHttpRequest>[] predicates = paths.stream()
      .<Predicate<ServerHttpRequest>>map(path -> req -> req.getPath().pathWithinApplication().value().startsWith(path))
      .toArray(Predicate[]::new);
    return new HttpRequestsFilter(predicates);
  }

  @Bean(name = "disabledLoggingRequestsFilter")
  @ConditionalOnMissingBean
  HttpRequestsFilter defaultDisabledLoggingRequestsFilter() {
    return new HttpRequestsFilter(
      req -> req.getPath().pathWithinApplication().value().startsWith("/health"),
      req -> req.getPath().pathWithinApplication().value().startsWith("/metrics")
    );
  }

  @Bean(name = "webFluxHttpMessageFormatter")
  @ConditionalOnMissingBean(name = "webFluxHttpMessageFormatter")
  MessageFormatter webFluxHttpMessageFormatter(@Qualifier("messageFormatter") Optional<MessageFormatter> maybeGlobalFormatter) {
    return maybeGlobalFormatter.orElse(new HttpMessageFormatter(Arrays.asList("Authorization")));
  }
}
