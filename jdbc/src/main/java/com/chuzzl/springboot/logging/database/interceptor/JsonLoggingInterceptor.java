package com.chuzzl.springboot.logging.database.interceptor;


import com.chuzzl.springboot.logging.database.JsonMessageFormatter;

public class JsonLoggingInterceptor extends LoggingInterceptor {

  public JsonLoggingInterceptor() {
    super(new JsonMessageFormatter());
  }
}
