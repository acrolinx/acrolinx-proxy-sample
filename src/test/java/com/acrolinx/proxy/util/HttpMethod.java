/* Copyright (c) 2024 Acrolinx GmbH */
package com.acrolinx.proxy.util;

import com.acrolinx.proxy.AcrolinxProxyHttpServlet;
import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.NotImplementedException;

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
    AcrolinxProxyHttpServlet acrolinxProxy = new AcrolinxProxyHttpServlet();
    acrolinxProxy.init(servletConfig);

    switch (this) {
      case DELETE:
        acrolinxProxy.doDelete(httpServletRequest, httpServletResponse);
        break;
      case GET:
        acrolinxProxy.doGet(httpServletRequest, httpServletResponse);
        break;
      case POST:
        acrolinxProxy.doPost(httpServletRequest, httpServletResponse);
        break;
      case PUT:
        acrolinxProxy.doPut(httpServletRequest, httpServletResponse);
        break;
      default:
        throw new IllegalArgumentException("Unknown HTTP method: " + this);
    }
  }
}
