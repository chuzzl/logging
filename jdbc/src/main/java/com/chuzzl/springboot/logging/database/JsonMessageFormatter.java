package com.chuzzl.springboot.logging.database;


import com.chuzzl.springboot.logging.api.Message;
import com.chuzzl.springboot.logging.api.MessageFormatter;

import java.util.stream.Collectors;

/**
 * @author Artem Demyansky
 */
public class JsonMessageFormatter implements MessageFormatter {

  @Override
  public String format(Message message) {
    if (!(message instanceof DatabaseMessage)) {
      throw new IllegalArgumentException("Can process only database messages");
    }

    StringBuilder sb = new StringBuilder();
    sb.append("{ \"direction\"").append(": \"").append(message.type().value).append("\", ");
    sb.append("\"type\"").append(": \"JDBC\", ");

    if (message.meta() != null) {
      sb.append(
        message.meta()
          .entrySet()
          .stream()
          .map(e -> String.format("\"%s\": \"%s\"", e.getKey(), e.getValue()))
          .collect(Collectors.joining(", ", "\"meta\": {", "}, "))
      );
    }

    sb.append("\"body\": \"").append(message.body().replaceAll("\"", "\\\"")).append("\" }");

    return sb.toString();
  }
}
