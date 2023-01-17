package com.chuzzl.springboot.logging.database.interceptor;


import com.chuzzl.springboot.logging.api.MessageFormatter;
import com.chuzzl.springboot.logging.api.OneLineFormatter;
import com.chuzzl.springboot.logging.database.LoggingJDBCEventListener;
import com.p6spy.engine.event.JdbcEventListener;
import com.p6spy.engine.logging.P6LogOptions;
import com.p6spy.engine.spy.P6Factory;
import com.p6spy.engine.spy.P6LoadableOptions;
import com.p6spy.engine.spy.option.P6OptionsRepository;

public class LoggingInterceptor implements P6Factory {
  private final MessageFormatter messageFormatter;

  public LoggingInterceptor() {
    messageFormatter = new OneLineFormatter();
  }

  public LoggingInterceptor(MessageFormatter messageFormatter) {
    this.messageFormatter = messageFormatter;
  }

  public P6LoadableOptions getOptions(final P6OptionsRepository p6OptionsRepository) {

    return new P6LogOptions(p6OptionsRepository);
  }

  public JdbcEventListener getJdbcEventListener() {
    return new LoggingJDBCEventListener(messageFormatter);
  }
}
