/* Copyright (c) 2016 Acrolinx GmbH */

package com.acrolinx.proxy;

import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

final class ContentLengthHeaderRemover implements HttpRequestInterceptor {
  static final HttpRequestInterceptor INSTANCE = new ContentLengthHeaderRemover();

  private ContentLengthHeaderRemover() {
    // do nothing
  }

  @Override
  public void process(final HttpRequest httpRequest, final HttpContext httpContext) {
    httpRequest.removeHeaders(HTTP.CONTENT_LEN);
  }
}
