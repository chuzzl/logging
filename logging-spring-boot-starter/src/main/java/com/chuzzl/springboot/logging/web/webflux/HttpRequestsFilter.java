package com.chuzzl.springboot.logging.web.webflux;

import org.springframework.http.server.reactive.ServerHttpRequest;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class HttpRequestsFilter {
  private List<Predicate<ServerHttpRequest>> predicates;

  public HttpRequestsFilter(List<Predicate<ServerHttpRequest>> predicates) {
    this.predicates = predicates;
  }

  @SafeVarargs
  public HttpRequestsFilter(Predicate<ServerHttpRequest> ...predicates) {
    this.predicates = Arrays.asList(predicates);
  }

  public boolean match(ServerHttpRequest request) {
    return predicates.stream().anyMatch(p -> p.test(request));
  }
}
