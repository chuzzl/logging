package com.chuzzl.springboot.logging.web.webclient;

import com.chuzzl.springboot.logging.web.HttpMessageFormatter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import com.chuzzl.springboot.logging.api.MessageFormatter;

import java.util.Arrays;
import java.util.Optional;

@Configuration
@ConditionalOnClass(WebClient.class)
public class WebClientLoggingAutoConfiguration {

  @Bean
  WebClientPostProcessor webClientPostProcessor() {
    return new WebClientPostProcessor();
  }

  @Bean(name = "webClientHttpMessageFormatter")
  @ConditionalOnMissingBean(name = "webClientHttpMessageFormatter")
  MessageFormatter webClientHttpMessageFormatter(@Qualifier("messageFormatter") Optional<MessageFormatter> maybeGlobalFormatter) {
    return maybeGlobalFormatter.orElse(new HttpMessageFormatter(Arrays.asList("Authorization")));
  }

  @Bean
  LoggingConnectorFactory loggingConnectorFactory() {
    return new LoggingConnectorFactory();
  }
}
