package com.chuzzl.springboot.logging.web.webclient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.client.reactive.ClientHttpConnector;
import com.chuzzl.springboot.logging.api.MessageFormatter;
import com.chuzzl.springboot.logging.api.OneLineFormatter;

public class LoggingConnectorFactory {

  @Autowired(required = false)
  @Qualifier("webClientHttpMessageFormatter")
  private MessageFormatter messageFormatter = new OneLineFormatter();

  public ClientHttpConnector decorate(ClientHttpConnector connector) {
    return new LoggingHttpConnector(connector, messageFormatter);
  }
}
