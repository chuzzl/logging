package com.chuzzl.springboot.logging.web.httpclient;

import com.chuzzl.springboot.logging.web.HttpMessageFormatter;
import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import com.chuzzl.springboot.logging.api.Message;
import com.chuzzl.springboot.logging.api.MessageFormatter;
import com.chuzzl.springboot.logging.web.HttpMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toMap;

/**
 * @author Artem Demyansky
 */
public class HttpClientLoggingInterceptor implements HttpRequestInterceptor, HttpResponseInterceptor {
  private static Logger log = Logger.getLogger(HttpClientLoggingInterceptor.class.getName());
  private MessageFormatter formatter = new HttpMessageFormatter(List.of("Authorization"));

  @Override
  public void process(HttpRequest request, HttpContext httpContext) throws IOException {
    String content = null;
    if (request instanceof HttpEntityEnclosingRequest) {
      HttpEntityEnclosingRequest requestWrapper = (HttpEntityEnclosingRequest) request;
      ByteArrayOutputStream bf = new ByteArrayOutputStream();
      requestWrapper.getEntity().writeTo(bf);
      content = bf.toString(StandardCharsets.UTF_8);
    }

    HttpMessage message = new HttpMessage(
      Message.Type.OUTBOUND,
      String.format(
        "%s %s",
        request.getRequestLine().getMethod(),
        request.getRequestLine().getUri()
      ),
      null,
      Arrays.stream(request.getAllHeaders())
        .collect(
          toMap(
            Header::getName,
            Header::getValue,
            (s, s2) -> s
          )
        ),
      content
    );
    log.info(formatter.format(message));

  }

  @Override
  public void process(HttpResponse response, HttpContext httpContext) throws IOException {

    byte[] body = EntityUtils.toByteArray(response.getEntity());
    String content = new String(body, StandardCharsets.UTF_8);

    response.setEntity(
      new ByteArrayEntity(
        body,
        ContentType.get(response.getEntity())
      )
    );

    HttpMessage message = new HttpMessage(
      Message.Type.INBOUND,
      String.format("HTTP %s", response.getStatusLine().getStatusCode()),
      null,
      Arrays.stream(response.getAllHeaders())
        .collect(
          toMap(
            Header::getName,
            Header::getValue,
            (s, s2) -> s
          )
        ),
      content
    );
    log.info(formatter.format(message));
  }
}
