package com.chuzzl.springboot.logging.web.resttemplate;

import com.chuzzl.springboot.logging.web.HttpMessageFormatter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;
import com.chuzzl.springboot.logging.api.MessageFormatter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Configuration
@ConditionalOnClass(RestTemplate.class)
public class RestTemplateLoggingAutoConfiguration {

  @Bean(name = "restTemplateHttpMessageFormatter")
  @ConditionalOnMissingBean(name = "restTemplateHttpMessageFormatter")
  MessageFormatter restTemplateHttpMessageFormatter(@Qualifier("messageFormatter") Optional<MessageFormatter> maybeGlobalFormatter) {
    return maybeGlobalFormatter.orElse(new HttpMessageFormatter(Arrays.asList("Authorization")));
  }

  @Bean
  LoggingInterceptor restTemplateLoggingInterceptor(@Qualifier("restTemplateHttpMessageFormatter") MessageFormatter messageFormatter) {
    return new LoggingInterceptor(messageFormatter);
  }

  @Bean
  RestTemplateCustomizer loggingCustomizer(LoggingInterceptor restTemplateLoggingInterceptor) {
    return restTemplate -> {
      List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
      interceptors.add(restTemplateLoggingInterceptor);
      restTemplate.setInterceptors(interceptors);
    };
  }
}
