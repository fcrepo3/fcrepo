/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client;

import java.io.PrintStream;

import java.util.Properties;

import javax.swing.JTextArea;

import fedora.client.batch.BatchTool;

/**
 * @author Bill Niebel
 */
public class BatchThread
        extends Thread {

    private String leadText = "";

    private Properties properties = null;

    private final Properties nullProperties = null;

    private BatchOutput batchOutput = null;

    private JTextArea jTextArea = null;

    private PrintStream originalOut = null;

    private PrintStream originalErr = null;

    private PrintStream printStream = null;

    public BatchThread(BatchOutput batchOutput,
                       JTextArea jTextArea,
                       String leadText)
            throws Exception {
        this.batchOutput = batchOutput;
        this.jTextArea = jTextArea;
        this.leadText = leadText;
        BatchOutputCatcher batchOutputCatcher =
                new BatchOutputCatcher(jTextArea);
        printStream = new PrintStream(batchOutputCatcher, true);
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public void run() {
        try {
            jTextArea.setText(leadText + "\n");
            originalOut = System.out;
            originalErr = System.err;
            System.setOut(printStream);
            System.setErr(printStream);
            batchOutput.setVisible(true);
            BatchTool batchTool =
                    new BatchTool(properties, nullProperties, nullProperties);
            batchTool.prep();
            batchTool.process();
        } catch (Exception e) {
        } finally {
            System.setOut(originalOut);
            originalOut = null;
            System.setErr(originalErr);
            originalErr = null;
            batchOutput.flush2file(); //2003.12.03 niebel -- duplicate output to file
        }
    }
}
