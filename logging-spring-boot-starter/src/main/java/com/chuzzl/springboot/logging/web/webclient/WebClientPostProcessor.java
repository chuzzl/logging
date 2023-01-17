package com.chuzzl.springboot.logging.web.webclient;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Field;

public class WebClientPostProcessor implements BeanPostProcessor {
  @Autowired
  LoggingConnectorFactory factory;

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    if (bean instanceof WebClient) {
      WebClient client = ((WebClient) bean);
      WebClient.Builder builder = client.mutate();
      Field connectorField = ReflectionUtils.findField(builder.getClass(), "connector");
      connectorField.setAccessible(true);
      ClientHttpConnector connector = (ClientHttpConnector) ReflectionUtils.getField(connectorField, builder);

      return client.mutate().clientConnector(factory.decorate(connector)).build();
    }
    return bean;
  }
}
