/* Copyright (c) 2024 Acrolinx GmbH */
package com.acrolinx.proxy.util;

import java.nio.file.Path;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.Tomcat;

public final class TomcatWrapper implements AutoCloseable {
  public static TomcatWrapper startOnRandomHttpPort(Path tempDirectory) throws LifecycleException {
    final Tomcat tomcat = createTomcat(tempDirectory);
    tomcat.start();

    return new TomcatWrapper(tomcat);
  }

  private static Connector createConnector() {
    Connector connector = new Connector();
    connector.setPort(0);
    return connector;
  }

  private static StandardHost createHost() {
    StandardHost standardHost = new StandardHost();
    standardHost.setName("localhost");
    standardHost.setUnpackWARs(false);
    return standardHost;
  }

  private static Tomcat createTomcat(Path tempDirectory) {
    Tomcat tomcat = new Tomcat();

    tomcat.setBaseDir(tempDirectory.toString());
    tomcat.setConnector(createConnector());
    tomcat.setHost(createHost());

    return tomcat;
  }

  private final Tomcat tomcat;

  private TomcatWrapper(Tomcat tomcat) {
    this.tomcat = tomcat;
  }

  @Override
  public void close() throws LifecycleException {
    tomcat.stop();
  }

  public Tomcat getTomcat() {
    return tomcat;
  }
}
