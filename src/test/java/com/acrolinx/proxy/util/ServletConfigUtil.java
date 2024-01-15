/* Copyright (c) 2024 Acrolinx GmbH */
package com.acrolinx.proxy.util;

import javax.servlet.ServletConfig;
import org.mockito.Mockito;

class ServletConfigUtil {
  static void stubServletConfig(ServletConfig servletConfig, String acrolinxUrl) {
    Mockito.when(servletConfig.getInitParameter("acrolinxURL")).thenReturn(acrolinxUrl);
    Mockito.when(servletConfig.getInitParameter("genericToken")).thenReturn("token");
    Mockito.when(servletConfig.getInitParameter("username")).thenReturn("username");
  }
}
