/*
 * File: DataResponseWrapper.java
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * A response wrapper to collect the response data in the filter chain.
 * 
 * @author nishen@melcoe.mq.edu.au
 */

public class DataResponseWrapper
        extends HttpServletResponseWrapper {

    private ByteArrayOutputStream output = null;

    private String contentType = null;

    private int contentLength;

    /**
     * Default constructor that duplicates the response provided.
     * 
     * @param response
     *        the response to duplicate.
     */
    public DataResponseWrapper(HttpServletResponse response) {
        super(response);
        output = new ByteArrayOutputStream();
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.ServletResponseWrapper#getOutputStream()
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new DataServletOutputStream(output);
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.ServletResponseWrapper#getWriter()
     */
    @Override
    public PrintWriter getWriter() throws IOException {
        return new PrintWriter(new OutputStreamWriter(getOutputStream(),
                                                      "UTF-8"), true);
    }

    /*
     * (non-Javadoc)
     * @see
     * javax.servlet.ServletResponseWrapper#setContentType(java.lang.String)
     */
    @Override
    public void setContentType(String type) {
        contentType = type;
        super.setContentType(type);
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.ServletResponseWrapper#getContentType()
     */
    @Override
    public String getContentType() {
        return contentType;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.ServletResponseWrapper#setContentLength(int)
     */
    @Override
    public void setContentLength(int length) {
        contentLength = length;
        super.setContentLength(length);
    }

    /**
     * @return the content length of this response
     */
    public int getContentLength() {
        return contentLength;
    }

    /**
     * @return the body of this response
     */
    public byte[] getData() {
        return output.toByteArray();
    }

    /**
     * Sets the body of this response.
     * 
     * @param data
     *        the data to set the body of this reponse to
     * @throws IOException
     */
    public void setData(byte[] data) throws IOException {
        output = new ByteArrayOutputStream();
        output.write(data);
        output.flush();
        setContentLength(output.size());
    }
}
