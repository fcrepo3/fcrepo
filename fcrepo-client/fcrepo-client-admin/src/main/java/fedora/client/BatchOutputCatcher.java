/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client;

import java.io.OutputStream;

import javax.swing.JTextArea;

/**
 * @author Bill Niebel
 */
public class BatchOutputCatcher
        extends OutputStream {

    private JTextArea jTextArea = null;

    @Override
    public void write(int b) {
        byte bv = (new Integer(b)).byteValue();
        jTextArea.append(new String(new byte[] {bv}));
    }

    public BatchOutputCatcher(JTextArea jTextArea) {
        this.jTextArea = jTextArea;
    }

}
