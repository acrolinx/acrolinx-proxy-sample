/* Copyright (c) 2024 Acrolinx GmbH */
package com.acrolinx.proxy.util;

import com.acrolinx.proxy.AcrolinxProxyHttpServlet;
import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public enum HttpMethod {
  DELETE,

  GET,

  POST,

  PUT;

  public void callAcrolinxProxyMethod(
      HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse,
      ServletConfig servletConfig)
      throws IllegalArgumentException, ServletException, IOException {
    AcrolinxProxyHttpServlet acrolinxProxyHttpServlet = new AcrolinxProxyHttpServlet();
    acrolinxProxyHttpServlet.init(servletConfig);

    switch (this) {
      case DELETE:
        acrolinxProxyHttpServlet.doDelete(httpServletRequest, httpServletResponse);
        break;
      case GET:
        acrolinxProxyHttpServlet.doGet(httpServletRequest, httpServletResponse);
        break;
      case POST:
        acrolinxProxyHttpServlet.doPost(httpServletRequest, httpServletResponse);
        break;
      case PUT:
        acrolinxProxyHttpServlet.doPut(httpServletRequest, httpServletResponse);
        break;
      default:
        throw new IllegalArgumentException("Unknown HTTP method: " + this);
    }
  }
}
