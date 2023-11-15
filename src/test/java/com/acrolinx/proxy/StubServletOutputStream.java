package com.acrolinx.proxy;

import java.io.ByteArrayOutputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

class StubServletOutputStream extends ServletOutputStream {
  final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

  @Override
  public void write(int i) {
    byteArrayOutputStream.write(i);
  }

  @Override
  public boolean isReady() {
    return false;
  }

  @Override
  public void setWriteListener(WriteListener writeListener) {
    // do nothing
  }
}
