package com.chuzzl.springboot.logging.web.resttemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.MimeType;
import org.springframework.util.StreamUtils;
import com.chuzzl.springboot.logging.api.Message;
import com.chuzzl.springboot.logging.api.MessageFormatter;
import com.chuzzl.springboot.logging.web.HttpMessage;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;

public class LoggingInterceptor implements ClientHttpRequestInterceptor {
  private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);
  private MessageFormatter formatter;

  public LoggingInterceptor(MessageFormatter formatter) {
    this.formatter = formatter;
  }

  @Override
  public ClientHttpResponse intercept(
    HttpRequest httpRequest,
    byte[] bytes,
    ClientHttpRequestExecution clientHttpRequestExecution
  ) throws IOException {
    logRequest(httpRequest, bytes);
    ClientHttpResponse response = new BufferingHttpResponseWrapper(clientHttpRequestExecution.execute(httpRequest, bytes));
    logResponse(response);
    return response;
  }

  private void logResponse(ClientHttpResponse response) throws IOException {
    Charset charset = Optional.ofNullable(response.getHeaders().getContentType())
      .map(MimeType::getCharset)
      .orElse(StandardCharsets.UTF_8);
    byte[] body = StreamUtils.copyToByteArray(response.getBody());
    String content = new String(body, charset);
    HttpMessage message = new HttpMessage(
      Message.Type.INBOUND,
      String.format("HTTP %s", response.getStatusCode().value()),
      null,
      response.getHeaders()
        .entrySet()
        .stream()
        .collect(
          toMap(
            Map.Entry::getKey,
            e -> String.join(", ", e.getValue())
          )
        ),
      content
    );
    log.info("{}", formatter.format(message));
  }

  private void logRequest(HttpRequest request, byte[] body) {
    Charset charset = Optional.ofNullable(request.getHeaders().getContentType())
      .map(MimeType::getCharset)
      .orElse(StandardCharsets.UTF_8);
    String content = new String(body, charset);
    HttpMessage message = new HttpMessage(
      Message.Type.OUTBOUND,
      String.format(
        "%s %s",
        request.getMethod().name(),
        request.getURI()
      ),
      null,
      request.getHeaders()
        .entrySet()
        .stream()
        .collect(
          toMap(
            Map.Entry::getKey,
            e -> String.join(", ", e.getValue())
          )
        ),
      content
    );
    log.info("{}", formatter.format(message));
  }
}
