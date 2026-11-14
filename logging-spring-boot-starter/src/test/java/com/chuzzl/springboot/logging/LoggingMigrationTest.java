package com.chuzzl.springboot.logging;

import com.chuzzl.springboot.logging.web.servlet.ServletLoggingAutoConfiguration;
import com.chuzzl.springboot.logging.web.webflux.WebfluxLoggingAutoConfiguration;
import com.chuzzl.springboot.logging.web.webclient.WebClientLoggingAutoConfiguration;
import com.chuzzl.springboot.logging.web.resttemplate.RestTemplateLoggingAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.reactive.function.client.WebClient;
import jakarta.servlet.Filter;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingMigrationTest {
  @Test
  void servletExclusionsPreserveRequestAndResponseBodies() {
    new WebApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(ServletLoggingAutoConfiguration.class))
      .withPropertyValues("logging.servlet.paths.disable=/skip")
      .run(context -> {
        assertThat(context).hasNotFailed();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/skip");
        request.setContent("payload".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        context.getBean(Filter.class).doFilter(request, response, (req, res) -> {
          assertThat(new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("payload");
          res.getWriter().write("response");
        });
        assertThat(response.getContentAsString()).isEqualTo("response");
      });
  }

  @Test
  void servletLoggingPreservesRequestAndResponseBodies() {
    new WebApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(ServletLoggingAutoConfiguration.class))
      .run(context -> {
        assertThat(context).hasNotFailed();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test");
        request.setContent("payload".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        context.getBean(Filter.class).doFilter(request, response, (req, res) -> {
          assertThat(new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("payload");
          res.getOutputStream().write("response".getBytes(StandardCharsets.UTF_8));
        });
        assertThat(response.getContentAsString()).isEqualTo("response");
      });
  }

  @Test
  void reactiveConfigurationStarts() {
    new ReactiveWebApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(WebfluxLoggingAutoConfiguration.class))
      .run(context -> assertThat(context).hasNotFailed());
  }

  @Test
  void clientConfigurationsStartWithDefaultWebClient() {
    new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(WebClientLoggingAutoConfiguration.class, RestTemplateLoggingAutoConfiguration.class))
      .withBean(WebClient.class, () -> WebClient.builder().build())
      .run(context -> {
        assertThat(context).hasNotFailed();
        assertThat(context.getBean(WebClient.class)).isNotNull();
      });
  }
}
