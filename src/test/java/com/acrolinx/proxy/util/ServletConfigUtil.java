/* Copyright (c) 2024 Acrolinx GmbH */
package com.acrolinx.proxy.util;

import jakarta.servlet.ServletConfig;
import java.time.Duration;
import org.mockito.Mockito;

class ServletConfigUtil {
  static void stubServletConfigBase(ServletConfig servletConfig, String acrolinxUrlString) {
    stubInitParameter(servletConfig, "acrolinxUrl", acrolinxUrlString);
    stubInitParameter(servletConfig, "genericToken", "token");
    stubInitParameter(servletConfig, "username", "username");
  }

  static void stubServletConfigTimeout(ServletConfig servletConfig, Duration duration) {
    stubInitParameter(servletConfig, "timeoutDuration", duration.toString());
  }

  private static void stubInitParameter(
      ServletConfig servletConfig, String parameterName, String parameterValue) {
    Mockito.when(servletConfig.getInitParameter(parameterName)).thenReturn(parameterValue);
  }

  private ServletConfigUtil() {
    throw new IllegalStateException();
  }
}
