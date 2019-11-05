package com.acrolinx;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class StubServletInputStream extends ServletInputStream {

    ByteArrayInputStream byteArrayInputStream;

    StubServletInputStream(String postData)
    {
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

    }

    @Override
    public int read() throws IOException {
        return this.byteArrayInputStream.read();
    }
}
