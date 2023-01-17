package com.chuzzl.springboot.logging.web.webclient;

import com.chuzzl.springboot.logging.web.HttpMessage;
import io.netty.buffer.ByteBufAllocator;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpRequestDecorator;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.client.reactive.ClientHttpResponseDecorator;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.chuzzl.springboot.logging.api.Message;
import com.chuzzl.springboot.logging.api.MessageFormatter;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public class LoggingHttpConnector implements ClientHttpConnector {
  private static final Logger logger = LoggerFactory.getLogger(LoggingHttpConnector.class);
  private final ClientHttpConnector connector;
  private final MessageFormatter messageFormatter;

  public LoggingHttpConnector(ClientHttpConnector connector, MessageFormatter messageFormatter) {
    this.connector = connector;
    this.messageFormatter = messageFormatter;
  }

  @Override
  public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri, Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {
    return connector.connect(method, uri, clientHttpRequest -> requestCallback.apply(new LoggingHttpRequestDecorator(clientHttpRequest)))
      .flatMap(clientHttpResponse ->
        collectToSingleBuffer(Flux.from(clientHttpResponse.getBody()))
          .map(bodyData -> {
            logInboundResponse(clientHttpResponse, bodyData);
            return new ClientHttpResponseDecorator(clientHttpResponse) {
              @Override
              public Flux<DataBuffer> getBody() {
                return Flux.just(bodyData);
              }
            };
          })
      );
  }


  private Mono<DataBuffer> collectToSingleBuffer(Flux<DataBuffer> body) {
    return body
      .collect(
        () -> {
          NettyDataBufferFactory nettyDataBufferFactory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
          return nettyDataBufferFactory.allocateBuffer();
        },
        (target, source) -> {
          target.write(source);
          DataBufferUtils.release(source);
        }
      );
  }

  private class LoggingHttpRequestDecorator extends ClientHttpRequestDecorator {

    public LoggingHttpRequestDecorator(ClientHttpRequest clientHttpRequest) {
      super(clientHttpRequest);
    }
    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
      return collectToSingleBuffer(Flux.from(body))
        .doOnNext(b -> logOutboundRequest(getDelegate(), b))
        .flatMap(b -> super.writeWith(Mono.just(b)));
    }
  }

  private void logOutboundRequest(ClientHttpRequest clientRequest, DataBuffer body) {
    Charset charset = Optional.ofNullable(clientRequest.getHeaders().getContentType())
      .map(MimeType::getCharset)
      .orElse(StandardCharsets.UTF_8);
    String content = body.toString(charset);

    HttpMessage request = new HttpMessage(
      Message.Type.OUTBOUND,
      String.format(
        "%s %s",
        clientRequest.getMethod().name(),
        clientRequest.getURI()
      ),
      null,
      clientRequest.getHeaders()
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
    logger.info("{}", messageFormatter.format(request));
  }

  private void logInboundResponse(ClientHttpResponse clientHttpResponse, DataBuffer bodyData) {
    String body = bodyData.toString(
      Optional.ofNullable(clientHttpResponse.getHeaders().getContentType())
        .map(MimeType::getCharset)
        .orElse(StandardCharsets.UTF_8));

    HttpMessage response = new HttpMessage(
      Message.Type.INBOUND,
      String.format("HTTP %s", clientHttpResponse.getStatusCode().value()),
      null,
      clientHttpResponse.getHeaders()
        .entrySet()
        .stream()
        .collect(
          toMap(
            Map.Entry::getKey,
            e -> String.join(", ", e.getValue())
          )
        ),
      body
    );
    logger.info("{}", messageFormatter.format(response));
  }
}
