/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.generate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Builds a wrapper around a generated Axis client stub, which includes imports
 * for the class and code that should run before and after each method
 * invocation. This is a cheap way of writing specialized API-A and API-M client
 * classes that need to do something extra (the same thing) for each method
 * call. Like, for instance, run it in a SwingWorker thread because it's being
 * called from a GUI. This is cleaner than writing the SwingWorker code for each
 * piece in the code where API-A or API-M methods need to be called.
 * 
 * @author Chris Wilper
 */
public class BuildAxisStubWrapper {

    private final BufferedWriter m_writer;

    private static String N = System.getProperty("line.separator");

    public BuildAxisStubWrapper(File stubFile,
                                File templateFile,
                                String wrapperPackage,
                                String wrapperClass,
                                File wrapperFile)
            throws Exception {
        System.out.println("Dynamically generating sourcecode for "
                + wrapperPackage + "." + wrapperClass);
        // first read the template, splitting by ##SPLITTER##, into 3 buffers
        StringBuffer importBuf = new StringBuffer();
        StringBuffer tsBuf = new StringBuffer();
        StringBuffer tfBuf = new StringBuffer();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(new FileInputStream(templateFile)));
        boolean sawSplitter = false;
        String line = "first";
        int i;
        boolean sawImports = false;
        while (line != null) {
            if (sawImports && line.equals("##SPLITTER##")) {
                sawSplitter = true;
            } else {
                if (!line.equals("first")) {
                    if (!sawImports) {
                        int x = 0;
                        while (!line.equals("##SPLITTER##")) {
                            importBuf.append(line + N);
                            line = reader.readLine();
                            x++;
                            if (x == 2000) {
                                throw new IOException("Template file must contain two ##SPLITTER## lines.");
                            }
                        }
                        sawImports = true;
                    } else {
                        if (sawSplitter) {
                            tfBuf.append("        " + line + N);
                        } else {
                            tsBuf.append("        " + line + N);
                        }
                    }
                }
            }
            line = reader.readLine();
        }
        reader.close();
        String methodStart = tsBuf.toString();
        String methodFinish = tfBuf.toString();
        if (!sawSplitter) {
            throw new IOException("Bad template... does not contain ##SPLITTER## line.");
        }
        // then start writing the output.. 
        m_writer = new BufferedWriter(new FileWriter(wrapperFile));
        println("package " + wrapperPackage + ";" + N + N
                + "import java.util.HashMap; // needed by generated code" + N);
        println(importBuf.toString());
        println("public class " + wrapperClass);
        // do the rest as we scan the stub file
        reader =
                new BufferedReader(new InputStreamReader(new FileInputStream(stubFile)));
        line = "";
        while (line != null) {
            i = line.indexOf("implements");
            if (i != -1) {
                String endPart = line.substring(i + 11);
                String interfaceClassName =
                        endPart.substring(0, endPart.indexOf(" "));
                println("        implements " + interfaceClassName + " {");
                println("");
                println("    /** The wrapped instance */");
                println("    private " + interfaceClassName + " m_instance;");
                println("");
                println("    public " + wrapperClass + "(" + interfaceClassName
                        + " instance) {");
                println("        m_instance=instance;");
                println("    }");
            } else {
                if (line.indexOf("public") != -1
                        && line.indexOf("throws java.rmi.RemoteException") != -1) {
                    println("");
                    println(line);
                    i = line.indexOf("(");
                    String beforeParams = line.substring(0, i);
                    int j = beforeParams.lastIndexOf(" ");
                    String methodName = beforeParams.substring(j + 1);
                    println("        String METHOD_NAME=\"" + methodName
                            + "\";");
                    println("        HashMap PARMS=new HashMap();");
                    String thisMethodStart;
                    String thisMethodFinish;

                    if (line.indexOf("public boolean ") != -1) {
                        thisMethodStart =
                                methodStart.replaceAll("##RETURN_TYPE##",
                                                       "Object");
                        thisMethodFinish =
                                methodFinish.replaceAll("##RETURN_TYPE##",
                                                        "Object");
                        thisMethodStart =
                                thisMethodStart
                                        .replaceAll("##RETURN##",
                                                    "return (Boolean)worker.get();// ");
                        thisMethodFinish =
                                thisMethodFinish
                                        .replaceAll("##RETURN##",
                                                    "return (Boolean)worker.get();// ");
                    } else if (line.indexOf(" void ") == -1) {
                        String afterPublic =
                                line.substring(line.indexOf("public") + 7);
                        String returnType =
                                afterPublic.substring(0, afterPublic
                                        .indexOf(" "));
                        thisMethodStart =
                                methodStart.replaceAll("##RETURN_TYPE##",
                                                       returnType);
                        thisMethodFinish =
                                methodFinish.replaceAll("##RETURN_TYPE##",
                                                        returnType);
                        thisMethodStart =
                                thisMethodStart.replaceAll("##RETURN##",
                                                           "return ");
                        thisMethodFinish =
                                thisMethodFinish.replaceAll("##RETURN##",
                                                            "return ");
                    } else {
                        thisMethodStart =
                                methodStart.replaceAll("##RETURN_TYPE##",
                                                       "Object");
                        thisMethodFinish =
                                methodFinish.replaceAll("##RETURN_TYPE##",
                                                        "Object");
                        thisMethodStart =
                                thisMethodStart.replaceAll("##RETURN##", "// ");
                        thisMethodFinish =
                                thisMethodFinish
                                        .replaceAll("##RETURN##", "// ");
                    }

                    StringBuffer callBuf = new StringBuffer();
                    if (line.indexOf(" void ") == -1) {
                        callBuf.append("return ");
                    }
                    callBuf.append("m_instance." + methodName + "(");
                    // get the names of the parameters
                    String parmsAndEnd = line.substring(i + 1);
                    String justParms =
                            parmsAndEnd.substring(0, parmsAndEnd.indexOf(")"));
                    String[] sigs = justParms.split(" ");
                    if (sigs.length > 1) {
                        // at least one parm
                        if (sigs.length == 2) {
                            // one parm
                            doParm(sigs[0], sigs[1], callBuf);
                        } else {
                            // multiple parms
                            for (int z = 0; z < sigs.length; z++) {
                                if (sigs[z].indexOf(",") != -1) {
                                    //callBuf.append(sigs[z].substring(0, sigs[z].length()-1) + ", ");
                                    doParm(sigs[z - 1],
                                           sigs[z].substring(0, sigs[z]
                                                   .length() - 1),
                                           callBuf);
                                    callBuf.append(", ");
                                } else {
                                    // last parm
                                    if (z == sigs.length - 1) {
                                        doParm(sigs[z - 1], sigs[z], callBuf);
                                        // callBuf.append(sigs[z]);
                                    }
                                }
                            }
                        }
                    }
                    callBuf.append(");");
                    println(thisMethodStart);
                    println("// call wrapped method" + N + callBuf.toString()
                            + N);
                    println(thisMethodFinish + "    }");
                }
            }
            line = reader.readLine();
        }
        reader.close();
        println("}");
        // close when finished
        m_writer.close();

    }

    private void doParm(String t, String n, StringBuffer buf) throws Exception {
        if (t.equals("boolean")) {
            println("        PARMS.put(\"" + n + "\", new Boolean(" + n + "));");
            buf.append("((Boolean) parms.get(\"" + n + "\")).booleanValue()");
        } else if (t.equals("int")) {
            println("        PARMS.put(\"" + n + "\", new Integer(" + n + "));");
            buf.append("((Integer) parms.get(\"" + n + "\")).intValue()");
        } else {
            println("        PARMS.put(\"" + n + "\", " + n + ");");
            buf.append("(" + t + ") parms.get(\"" + n + "\")");
        }
    }

    private void println(String line) throws Exception {
        m_writer.write(line, 0, line.length());
        m_writer.newLine();
    }

    public static void main(String[] args) {
        int argCount = 5;
        try {
            if (args.length == argCount) {
                File stub = new File(args[0]);
                File template = new File(args[1]);
                String pkg = args[2];
                String cls = args[3];
                File wrapper = new File(args[4]);
                new BuildAxisStubWrapper(stub, template, pkg, cls, wrapper);
            } else {
                throw new IOException("Must supply " + argCount + " arguments.");
            }
        } catch (Exception e) {
            if (e.getMessage() != null) {
                System.err.println(e.getMessage());
            } else {
                e.printStackTrace();
            }
            System.err
                    .println("Usage: BuildAxisStubWrapper stubFile templateFile "
                            + "wrapperPackage wrapperClass wrapperFile");
        }
    }

}
