package com.chuzzl.springboot.logging.web;

import com.chuzzl.springboot.logging.api.Message;
import com.chuzzl.springboot.logging.api.MessageFormatter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HttpMessageFormatter implements MessageFormatter {
  private final List<String> forbiddenHeaders;

  public HttpMessageFormatter(List<String> forbiddenHeaders) {
    this.forbiddenHeaders = forbiddenHeaders;
  }

  @Override
  public String format(Message message) {
    if (!(message instanceof HttpMessage)) {
      throw new IllegalArgumentException("Can process only http messages");
    }
    StringBuilder sb = new StringBuilder();
    sb.append("{ \"direction\"").append(": \"").append(message.type().value).append("\", ");
    sb.append("\"type\"").append(": \"HTTP\", ");
    if (message.label().startsWith("HTTP")) {
      // label format: <'HTTP'><whitespace><STATUS_CODE>
      String statusCode = message.label().substring(5);
      sb.append("\"httpRespCode\"").append(": \"").append(statusCode).append("\", ");
    } else {
      //label format: <HTTP_METHOD><whitespace><URL>
      int separatorIndex = message.label().indexOf(" ");
      String method = message.label().substring(0, separatorIndex);
      String url = message.label().substring(separatorIndex + 1);
      sb.append("\"method\": ").append("\"").append(method).append("\", ");
      sb.append("\"uri\": ").append("\"").append(url).append("\", ");
    }

    sb.append(
      maskValues(message.properties())
        .entrySet()
        .stream()
        .map(e -> String.format("\"%s\": \"%s\"", e.getKey(), e.getValue()))
        .collect(Collectors.joining(", ", "\"headers\": {", "}, "))
    );

    sb.append("\"body\": ").append(noLineBreaks(String.valueOf(message.body()))).append(" }");

    return sb.toString();
  }

  private Map<String, String> maskValues(Map<String, String> data) {
    if (data == null) {
      return null;
    }
    return data.entrySet().stream().collect(Collectors.toMap(
      Map.Entry::getKey,
      e -> {
        if (forbiddenHeaders.stream().anyMatch(e.getKey()::equalsIgnoreCase)) {
          return "***";
        } else {
          return e.getValue();
        }
      }
    ));
  }

  private static String noLineBreaks(String string) {
    if (string == null) {
      return null;
    } else {
      StringBuilder sb = new StringBuilder();
      IntStream.range(0, string.length()).forEach((i) -> {
        if (string.charAt(i) != '\n' && string.charAt(i) != '\r') {
          sb.append(string.charAt(i));
        }
      });
      return sb.toString();
    }
  }
}
