package com.chuzzl.springboot.logging.web.servlet;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class HttpRequestsFilter {
  private List<Predicate<HttpServletRequest>> predicates;

  public HttpRequestsFilter(List<Predicate<HttpServletRequest>> predicates) {
    this.predicates = predicates;
  }

  @SafeVarargs
  public HttpRequestsFilter(Predicate<HttpServletRequest> ...predicates) {
    this.predicates = Arrays.asList(predicates);
  }

  public boolean match(HttpServletRequest request) {
    return predicates.stream().anyMatch(p -> p.test(request));
  }
}
