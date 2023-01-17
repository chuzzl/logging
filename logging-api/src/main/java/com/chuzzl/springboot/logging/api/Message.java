package com.chuzzl.springboot.logging.api;

import java.util.Map;

public interface Message {
  Type type();

  String label();

  Map<String, String> meta();

  Map<String, String> properties();

  String body();

  enum Type {
    INBOUND("Inbound"), OUTBOUND("Outbound");

    public final String value;

    Type(String value) {
      this.value = value;
    }
  }
}