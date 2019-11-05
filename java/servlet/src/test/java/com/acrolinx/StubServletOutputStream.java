package com.acrolinx;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class StubServletOutputStream extends ServletOutputStream {
    public ByteArrayOutputStream os = new ByteArrayOutputStream();

    public void write(int i) throws IOException {
        os.write(i);
    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {

    }
}
