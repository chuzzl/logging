package com.chuzzl.springboot.logging.web;

import com.chuzzl.springboot.logging.api.Message;

import java.util.Map;

public class HttpMessage implements Message {
  private final Type type;
  private final String label;
  private final Map<String, String> meta;
  private final Map<String, String> properties;
  private final String body;

  public HttpMessage(Type type, String label, Map<String, String> meta, Map<String, String> properties, String body) {
    this.type = type;
    this.label = label;
    this.meta = meta;
    this.properties = properties;
    this.body = body;
  }

  @Override
  public Type type() {
    return type;
  }

  @Override
  public String label() {
    return label;
  }

  @Override
  public Map<String, String> meta() {
    return meta;
  }

  @Override
  public Map<String, String> properties() {
    return properties;
  }

  @Override
  public String body() {
    return body;
  }
}
