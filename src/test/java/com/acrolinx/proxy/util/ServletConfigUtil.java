/* Copyright (c) 2024 Acrolinx GmbH */
package com.acrolinx.proxy.util;

import javax.servlet.ServletConfig;
import org.mockito.Mockito;

class ServletConfigUtil {
  static void stubServletConfigBase(ServletConfig servletConfig, String acrolinxUrl) {
    Mockito.when(servletConfig.getInitParameter("acrolinxURL")).thenReturn(acrolinxUrl);
    Mockito.when(servletConfig.getInitParameter("genericToken")).thenReturn("token");
    Mockito.when(servletConfig.getInitParameter("username")).thenReturn("username");
  }

  static void stubServletConfigConnectTimeout(
      ServletConfig servletConfig, String connectTimeoutInMillis) {
    Mockito.when(servletConfig.getInitParameter("connectTimeoutInMillis"))
        .thenReturn(connectTimeoutInMillis);
  }

  static void stubServletConfigSocketTimeout(
      ServletConfig servletConfig, String socketTimeoutInMillis) {
    Mockito.when(servletConfig.getInitParameter("socketTimeoutInMillis"))
        .thenReturn(socketTimeoutInMillis);
  }
}
