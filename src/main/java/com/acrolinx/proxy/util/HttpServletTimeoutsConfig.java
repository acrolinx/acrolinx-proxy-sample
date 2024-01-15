/* Copyright (c) 2024 Acrolinx GmbH */
package com.acrolinx.proxy.util;

public class HttpServletTimeoutsConfig {
  private final int connectTimeoutInMillis;
  private final int socketTimeoutInMillis;

  public HttpServletTimeoutsConfig() {
    this.connectTimeoutInMillis = -1;
    this.socketTimeoutInMillis = -1;
  }

  public int getConnectTimeoutInMillis() {
    return connectTimeoutInMillis;
  }

  public int getSocketTimeoutInMillis() {
    return socketTimeoutInMillis;
  }
}
