package com.acrolinx.proxy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

class StubServletInputStream extends ServletInputStream {
  private final ByteArrayInputStream byteArrayInputStream;

  StubServletInputStream(String postData) {
    byteArrayInputStream = new ByteArrayInputStream(postData.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public boolean isFinished() {
    return false;
  }

  @Override
  public boolean isReady() {
    return false;
  }

  @Override
  public void setReadListener(ReadListener readListener) {
    // do nothing
  }

  @Override
  public int read() {
    return this.byteArrayInputStream.read();
  }
}
