/* Copyright (c) 2024 Acrolinx GmbH */
package com.acrolinx.proxy.util;

import jakarta.servlet.ServletConfig;
import org.mockito.Mockito;

class ServletConfigUtil {
  static void stubServletConfigBase(ServletConfig servletConfig, String acrolinxUrlString) {
    stubInitParameter(servletConfig, "acrolinxUrl", acrolinxUrlString);
    stubInitParameter(servletConfig, "genericToken", "token");
    stubInitParameter(servletConfig, "username", "username");
  }

  static void stubServletConfigConnectTimeout(
      ServletConfig servletConfig, String connectTimeoutInMillis) {
    stubInitParameter(servletConfig, "connectTimeoutInMillis", connectTimeoutInMillis);
  }

  static void stubServletConfigSocketTimeout(
      ServletConfig servletConfig, String socketTimeoutInMillis) {
    stubInitParameter(servletConfig, "socketTimeoutInMillis", socketTimeoutInMillis);
  }

  private static void stubInitParameter(
      ServletConfig servletConfig, String parameterName, String parameterValue) {
    Mockito.when(servletConfig.getInitParameter(parameterName)).thenReturn(parameterValue);
  }

  private ServletConfigUtil() {
    throw new IllegalStateException();
  }
}
