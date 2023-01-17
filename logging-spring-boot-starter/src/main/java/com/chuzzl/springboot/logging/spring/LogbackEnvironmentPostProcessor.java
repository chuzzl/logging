package com.chuzzl.springboot.logging.spring;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Arrays;
import java.util.HashMap;

public class LogbackEnvironmentPostProcessor implements EnvironmentPostProcessor {
  private static final String CONFIG_PROPERTY = "logging.config";

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    if (environment.getProperty(CONFIG_PROPERTY) == null && Arrays.asList(environment.getActiveProfiles()).contains("production")) {
      application.setBannerMode(Banner.Mode.OFF);
      HashMap<String, Object> properties = new HashMap<>();
      properties.put(CONFIG_PROPERTY, "classpath:com/chuzzl/springboot/logging/logback-production.xml");
      MapPropertySource source = new MapPropertySource("logging-spring-boot-starter", properties);
      environment.getPropertySources().addFirst(source);
    }
  }
}
