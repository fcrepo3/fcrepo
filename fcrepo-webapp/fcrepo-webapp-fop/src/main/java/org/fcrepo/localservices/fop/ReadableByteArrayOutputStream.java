package org.fcrepo.localservices.fop;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;


public class ReadableByteArrayOutputStream extends ByteArrayOutputStream {

    boolean closed;
    
    public ReadableByteArrayOutputStream() {
        super();
        closed = false;
    }
    
    public ReadableByteArrayOutputStream(int size) {
        super(size);
        closed = false;
    }
    
    @Override
    public void write(byte[] b, int off, int len) {
        if (closed) {
            throw new IllegalStateException("ReadableByteArrayOutputStream has been closed for reading");
        }
        super.write(b, off, len);
    }
    
    @Override
    public void write(int b) {
        if (closed) {
            throw new IllegalStateException("ReadableByteArrayOutputStream has been closed for reading");
        }
        super.write(b);
    }
    
    @Override
    public void close() {
        closed = true;
    }
    
    public int length() {
        return count;
    }
    
    public void writeAllTo(OutputStream out) throws IOException {
        out.write(buf, 0, count);
    }
    
    public ByteArrayInputStream toInputStream() {
        closed = true;
        return new ByteArrayInputStream(buf, 0, count);
    }
    
    public String getString(Charset cs) {
        closed = true;
        return new String(buf, 0, count, cs);
    }
    
}
