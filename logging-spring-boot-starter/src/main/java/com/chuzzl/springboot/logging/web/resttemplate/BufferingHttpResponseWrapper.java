package com.chuzzl.springboot.logging.web.resttemplate;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BufferingHttpResponseWrapper implements ClientHttpResponse {
  private final ClientHttpResponse delegate;
  private byte[] body;

  public BufferingHttpResponseWrapper(ClientHttpResponse delegate) {
    this.delegate = delegate;
  }


  @Override
  public HttpStatus getStatusCode() throws IOException {
    return delegate.getStatusCode();
  }

  @Override
  public int getRawStatusCode() throws IOException {
    return delegate.getRawStatusCode();
  }

  @Override
  public String getStatusText() throws IOException {
    return delegate.getStatusText();
  }

  @Override
  public void close() {
    delegate.close();
  }

  @Override
  public InputStream getBody() throws IOException {
    if (this.body == null) {
      this.body = StreamUtils.copyToByteArray(this.delegate.getBody());
    }
    return new ByteArrayInputStream(this.body);
  }

  @Override
  public HttpHeaders getHeaders() {
    return delegate.getHeaders();
  }
}
