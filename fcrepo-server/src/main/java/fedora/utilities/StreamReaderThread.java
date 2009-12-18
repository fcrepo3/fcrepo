/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities;

import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author Edwin Shin
 */
public class StreamReaderThread
        extends Thread {

    StringBuffer mOut;

    InputStreamReader mIn;

    public StreamReaderThread(InputStream in, StringBuffer out) {
        mOut = out;
        mIn = new InputStreamReader(in);
    }

    @Override
    public void run() {
        int ch;
        try {
            while (-1 != (ch = mIn.read())) {
                mOut.append((char) ch);
            }
        } catch (Exception e) {
            mOut.append("\nRead error:" + e.getMessage());
        }
    }
}
