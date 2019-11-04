package com.acrolinx;

import javax.servlet.ServletOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class StubServletOutputStream extends ServletOutputStream {
    public ByteArrayOutputStream os = new ByteArrayOutputStream();

    public void write(int i) throws IOException {
        os.write(i);
    }
}
