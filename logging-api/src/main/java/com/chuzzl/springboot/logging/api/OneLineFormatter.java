package com.chuzzl.springboot.logging.api;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class OneLineFormatter implements MessageFormatter {
  @Override
  public String format(Message message) {
    StringBuilder sb = new StringBuilder();

    sb.append(message.type().value);

    Optional.ofNullable(message.label()).ifPresent(label ->
      sb.append(" ").append(label)
    );

    Optional.ofNullable(message.meta()).ifPresent(meta ->
      sb.append(" ").append("Meta [").append(
        meta.entrySet().stream().map(this::formatEntity).collect(Collectors.joining(", "))
      ).append("]")
    );

    Optional.ofNullable(message.properties()).ifPresent(props ->
      sb.append(" ").append("Props [").append(
        props.entrySet().stream().map(this::formatEntity).collect(Collectors.joining(", "))
      ).append("]")
    );

    sb.append(" ").append("Body=").append(noLineBreaks(String.valueOf(message.body())));

    return sb.toString();
  }

  private String formatEntity(Map.Entry<String, String> e) {
    return String.format("%s='%s'", e.getKey(), e.getValue());
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
