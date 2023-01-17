package com.chuzzl.springboot.logging.web.webflux;

import io.netty.buffer.ByteBufAllocator;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.sleuth.instrument.web.TraceWebFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.util.MimeType;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.chuzzl.springboot.logging.api.Message;
import com.chuzzl.springboot.logging.api.MessageFormatter;
import com.chuzzl.springboot.logging.api.OneLineFormatter;
import com.chuzzl.springboot.logging.web.HttpMessage;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;


public class LoggingWebFilter implements WebFilter, Ordered {
  private static final Logger logger = LoggerFactory.getLogger(LoggingWebFilter.class);

  @Autowired
  private HttpRequestsFilter disabledLoggingRequestsFilter;
  @Autowired(required = false)
  @Qualifier("webFluxHttpMessageFormatter")
  private MessageFormatter messageFormatter = new OneLineFormatter();

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    return Mono.subscriberContext().flatMap(context -> {
      Mono<ServerWebExchange> targetExchange;
      if (!disabledLoggingRequestsFilter.match(exchange.getRequest())) {
        ServerHttpRequest originalRequest = exchange.getRequest();
        ServerHttpResponse newResponse = new LoggingServerHttpResponseDecorator(exchange.getResponse());

        targetExchange = collectToSingleBuffer(originalRequest.getBody())
          .doOnNext(body -> logInboundRequest(originalRequest, body))
          .map(body -> new ServerHttpRequestDecorator(originalRequest) {
              @Override
              public Flux<DataBuffer> getBody() {
                return Flux.just(body);
              }
            }
          )
          .map(newRequest ->
            exchange.mutate().request(newRequest).response(newResponse).build()
          );
      } else {
        targetExchange = Mono.just(exchange);
      }
      return targetExchange.flatMap(chain::filter);
    });
  }

  @Override
  public int getOrder() {
    return TraceWebFilter.ORDER + 1;
  }

  private void logInboundRequest(ServerHttpRequest request, DataBuffer data) {
    String body = data.toString(
      Optional.ofNullable(request.getHeaders().getContentType())
        .map(MimeType::getCharset)
        .orElse(StandardCharsets.UTF_8));

    HttpMessage requests = new HttpMessage(
      Message.Type.INBOUND,
      String.format(
        "%s %s",
        Optional.ofNullable(request.getMethod()).map(Enum::name).orElse("UNDEFINED"),
        request.getPath().pathWithinApplication()
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
      body
    );
    logger.info("{}", messageFormatter.format(requests));
  }

  private void logOutboundResponse(ServerHttpResponse serverHttpResponse, DataBuffer singleBody) {
    Charset charset = Optional.ofNullable(serverHttpResponse.getHeaders().getContentType())
      .map(MimeType::getCharset)
      .orElse(StandardCharsets.UTF_8);
    String content = singleBody.toString(charset);

    HttpMessage response = new HttpMessage(
      Message.Type.OUTBOUND,
      String.format("HTTP %s", serverHttpResponse.getStatusCode().value()),
      null,
      serverHttpResponse.getHeaders()
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
    logger.info("{}", messageFormatter.format(response));
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

  private class LoggingServerHttpResponseDecorator extends ServerHttpResponseDecorator {

    public LoggingServerHttpResponseDecorator(ServerHttpResponse response) {
      super(response);
    }

    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
      return collectToSingleBuffer(Flux.from(body))
        .doOnNext(singleBody -> logOutboundResponse(getDelegate(), singleBody))
        .flatMap(b -> super.writeWith(Mono.just(b)));
    }
  }
}
