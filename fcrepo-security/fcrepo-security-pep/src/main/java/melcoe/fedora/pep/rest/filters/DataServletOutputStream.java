/*
 * File: DataServletOutputStream.java
 *
 * Copyright 2007 Macquarie E-Learning Centre Of Excellence
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package melcoe.fedora.pep.rest.filters;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletOutputStream;

/**
 * Output stream class that is required when creating a HttpResponseWrapper.
 * 
 * @author nishen@melcoe.mq.edu.au
 */
public class DataServletOutputStream
        extends ServletOutputStream {

    private DataOutputStream stream = null;

    /**
     * Contructor that sets the output stream.
     * 
     * @param out
     *        the output stream to capture
     */
    public DataServletOutputStream(OutputStream out) {
        stream = new DataOutputStream(out);
    }

    /*
     * (non-Javadoc)
     * @see java.io.OutputStream#write(int)
     */
    @Override
    public void write(int i) throws IOException {
        stream.write(i);
    }

    /*
     * (non-Javadoc)
     * @see java.io.OutputStream#write(byte[])
     */
    @Override
    public void write(byte[] b) throws IOException {
        stream.write(b);
    }

    /*
     * (non-Javadoc)
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        stream.write(b, off, len);
    }
}
