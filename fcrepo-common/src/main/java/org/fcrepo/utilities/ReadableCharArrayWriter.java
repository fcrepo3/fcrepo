package org.fcrepo.utilities;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;


public class ReadableCharArrayWriter extends CharArrayWriter {
    
    private boolean closed = false;
    
    public ReadableCharArrayWriter() {
        super();
    }
    
    public ReadableCharArrayWriter(int size) {
        super(size);
    }
    
    @Override
    public void write(char[] b, int off, int len) {
        if (closed) {
            throw new IllegalStateException("ReadableCharArrayWriter has been closed for reading");
        }
        super.write(b, off, len);
    }
    
    @Override
    public void write(int b) {
        if (closed) {
            throw new IllegalStateException("ReadableCharArrayWriter has been closed for reading");
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
    
    public void writeAllTo(Writer writer) throws IOException {
        writer.write(buf, 0, count);
    }
    
    public CharArrayReader toReader() {
        closed = true;
        return new CharArrayReader(buf, 0, count);
    }
    
    public CharBuffer toBuffer() {
        closed = true;
        return CharBuffer.wrap(buf, 0, count).asReadOnlyBuffer();
    }
    
    public String getString() {
        closed = true;
        return new String(buf, 0, count);
    }

}
