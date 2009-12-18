/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * A PrintStream that sends its output to Administrator.WATCH_AREA, the
 * JTextArea of the Tools->Advanced->STDOUT/STDERR window. This is used for
 * redirecting System.out/err output to the UI.
 * 
 * @author Chris Wilper
 */
public class WatchPrintStream
        extends PrintStream {

    /** Output is buffered here until a call to println(String) */
    private final ByteArrayOutputStream m_out;

    public WatchPrintStream(ByteArrayOutputStream out) {
        super(out);
        m_out = out;
    }

    /**
     * Every time this is called, the buffer is cleared an output is sent to the
     * JTextArea.
     */
    @Override
    public void println(String str) {
        super.println(str);
        if (Administrator.WATCH_AREA != null) {
            String buf = m_out.toString();
            m_out.reset();
            Administrator.WATCH_AREA.append(buf);
        }
    }

}