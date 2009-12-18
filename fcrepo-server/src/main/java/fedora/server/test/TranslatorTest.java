/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import fedora.common.Constants;

import fedora.server.Server;
import fedora.server.storage.translation.DOTranslationUtility;
import fedora.server.storage.translation.DOTranslator;
import fedora.server.storage.types.BasicDigitalObject;
import fedora.server.storage.types.DigitalObject;

/**
 * Tests the configured DOTranslator instance, deserializing, then 
 * re-serializing and printing the bytes from the file whose name is passed in.
 * 
 * <p>Since DOTranslator is a Module, it's more appropriate to test it by 
 * starting up the configured server instance.
 * 
 * @author Chris Wilper
 */
public class TranslatorTest {

    public static void main(String args[]) {
        FileInputStream in = null;
        try {
            if (args.length != 3) {
                throw new IOException("*Three* parameters needed, filename, format, and encoding.");
            }
            if (Constants.FEDORA_HOME == null) {
                throw new IOException("FEDORA_HOME is not set. Try using -Dfedora.home=path/to/fedorahome");
            }
            in = new FileInputStream(new File(args[0]));
            Server server;
            server = Server.getInstance(new File(Constants.FEDORA_HOME));
            DOTranslator trans =
                    (DOTranslator) server
                            .getModule("fedora.server.storage.translation.DOTranslator");
            if (trans == null) {
                throw new IOException("DOTranslator module not found via getModule");
            }
            DigitalObject obj = new BasicDigitalObject();
            System.out.println("Deserializing...");
            trans.deserialize(in,
                              obj,
                              args[1],
                              args[2],
                              DOTranslationUtility.DESERIALIZE_INSTANCE);
            System.out.println("Done.");
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            System.out.println("Re-serializing...");
            trans.serialize(obj,
                            outStream,
                            args[1],
                            args[2],
                            DOTranslationUtility.SERIALIZE_STORAGE_INTERNAL);
            System.out.println("Done. Here it is:");
            System.out.println(outStream.toString(args[2]));
            server.shutdown(null); //fixup for xacml
        } catch (Exception e) {
            System.out.println("Error: " + e.getClass().getName() + " "
                    + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }
}
