/* Copyright (c) 2024 Acrolinx GmbH */
package com.acrolinx.proxy.util;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

public final class WireMockServerWrapper implements AutoCloseable {
  public static WireMockServerWrapper startOnRandomHttpPort() {
    return createAndStart(createWireMockConfiguration().dynamicPort());
  }

  public static WireMockServerWrapper startOnRandomHttpsPort() {
    return createAndStart(createWireMockConfiguration().dynamicHttpsPort().httpDisabled(true));
  }

  private static WireMockServerWrapper createAndStart(Options options) {
    WireMockServer wireMockServer = new WireMockServer(options);
    wireMockServer.start();

    return new WireMockServerWrapper(wireMockServer);
  }

  private static WireMockConfiguration createWireMockConfiguration() {
    return new WireMockConfiguration().notifier(new Slf4jNotifier(false));
  }

  private final WireMockServer wireMockServer;

  private WireMockServerWrapper(WireMockServer wireMockServer) {
    this.wireMockServer = wireMockServer;
  }

  @Override
  public void close() {
    wireMockServer.stop();
  }

  public WireMockServer getWireMockServer() {
    return wireMockServer;
  }
}
