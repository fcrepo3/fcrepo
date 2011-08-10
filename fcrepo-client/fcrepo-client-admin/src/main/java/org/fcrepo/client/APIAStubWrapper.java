
package org.fcrepo.client;

import java.awt.Dimension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.math.BigInteger;

import org.fcrepo.server.types.mtom.gen.DatastreamDef;
import org.fcrepo.server.types.mtom.gen.FieldSearchResult;
import org.fcrepo.server.types.mtom.gen.MIMETypedStream;
import org.fcrepo.server.types.mtom.gen.ObjectMethodsDef;
import org.fcrepo.server.types.mtom.gen.ObjectProfile;
import org.fcrepo.server.types.mtom.gen.RepositoryInfo;

public class APIAStubWrapper
        implements org.fcrepo.server.access.FedoraAPIAMTOM {

    /** The wrapped instance */
    private final org.fcrepo.server.access.FedoraAPIAMTOM m_instance;

    public APIAStubWrapper(org.fcrepo.server.access.FedoraAPIAMTOM instance) {
        m_instance = instance;
    }

    @Override
    public RepositoryInfo describeRepository() {
        String METHOD_NAME = "describeRepository";
        HashMap PARMS = new HashMap();
        // Run the method in a SwingWorker thread
        SwingWorker worker = new SwingWorker(PARMS) {

            @Override
            public Object construct() {

                // call wrapped method
                return m_instance.describeRepository();

            }
        };
        worker.start();
        // The following code will run in the (safe)
        // Swing event dispatcher thread.
        int ms = 0;
        Dimension d = Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words = new ArrayList();
        StringBuffer word = new StringBuffer();
        boolean lastWasCaps = true;
        for (int i = 0; i < METHOD_NAME.length(); i++) {
            char c = METHOD_NAME.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                // char is caps
                if (!lastWasCaps) {
                    // new word
                    words.add(word.toString());
                    word = new StringBuffer();
                }
                word.append(c);
                lastWasCaps = true;
            } else {
                // char is lowercase
                word.append(c);
                lastWasCaps = false;
            }
        }
        words.add(word.toString());
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < words.size(); i++) {
            String lcWord = ((String) words.get(i)).toLowerCase();
            if (i == 0) {
                String firstChar = lcWord.substring(0, 1).toUpperCase();
                char lastChar = lcWord.charAt(lcWord.length() - 1);
                String middle = lcWord.substring(1, lcWord.length() - 1);
                buf.append(firstChar);
                buf.append(middle);
                buf.append(lastChar);
                buf.append(" ");
            } else {
                buf.append(lcWord + " ");
            }
        }
        Administrator.PROGRESS.setString(buf.toString() + ". . .");
        while (!worker.done) {
            try {
                Administrator.PROGRESS.setValue(ms);
                Administrator.PROGRESS
                        .paintImmediately(0,
                                          0,
                                          (int) d.getWidth() - 1,
                                          (int) d.getHeight() - 1);
                Thread.sleep(100);
                ms = ms + 100;
                if (ms >= 2000) ms = 200;
            } catch (InterruptedException ie) {
            }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0,
                                                0,
                                                (int) d.getWidth() - 1,
                                                (int) d.getHeight() - 1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {
        }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");

        // Otherwise, get the value from the
        // worker (returning it if applicable)
        return (RepositoryInfo) worker.get();
    }

    @Override
    public ObjectProfile getObjectProfile(java.lang.String pid,
                                          java.lang.String asOfDateTime) {
        String METHOD_NAME = "getObjectProfile";
        HashMap PARMS = new HashMap();
        PARMS.put("pid", pid);
        PARMS.put("asOfDateTime", asOfDateTime);
        // Run the method in a SwingWorker thread
        SwingWorker worker = new SwingWorker(PARMS) {

            @Override
            public Object construct() {

                // call wrapped method
                return m_instance.getObjectProfile((java.lang.String) parms
                        .get("pid"), (java.lang.String) parms
                        .get("asOfDateTime"));

            }
        };
        worker.start();
        // The following code will run in the (safe)
        // Swing event dispatcher thread.
        int ms = 0;
        Dimension d = Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words = new ArrayList();
        StringBuffer word = new StringBuffer();
        boolean lastWasCaps = true;
        for (int i = 0; i < METHOD_NAME.length(); i++) {
            char c = METHOD_NAME.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                // char is caps
                if (!lastWasCaps) {
                    // new word
                    words.add(word.toString());
                    word = new StringBuffer();
                }
                word.append(c);
                lastWasCaps = true;
            } else {
                // char is lowercase
                word.append(c);
                lastWasCaps = false;
            }
        }
        words.add(word.toString());
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < words.size(); i++) {
            String lcWord = ((String) words.get(i)).toLowerCase();
            if (i == 0) {
                String firstChar = lcWord.substring(0, 1).toUpperCase();
                char lastChar = lcWord.charAt(lcWord.length() - 1);
                String middle = lcWord.substring(1, lcWord.length() - 1);
                buf.append(firstChar);
                buf.append(middle);
                buf.append(lastChar);
                buf.append(" ");
            } else {
                buf.append(lcWord + " ");
            }
        }
        Administrator.PROGRESS.setString(buf.toString() + ". . .");
        while (!worker.done) {
            try {
                Administrator.PROGRESS.setValue(ms);
                Administrator.PROGRESS
                        .paintImmediately(0,
                                          0,
                                          (int) d.getWidth() - 1,
                                          (int) d.getHeight() - 1);
                Thread.sleep(100);
                ms = ms + 100;
                if (ms >= 2000) ms = 200;
            } catch (InterruptedException ie) {
            }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0,
                                                0,
                                                (int) d.getWidth() - 1,
                                                (int) d.getHeight() - 1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {
        }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");
        // Otherwise, get the value from the
        // worker (returning it if applicable)
        return (ObjectProfile) worker.get();
    }

    @Override
    public List<ObjectMethodsDef> listMethods(java.lang.String pid,
                                              java.lang.String asOfDateTime) {
        String METHOD_NAME = "listMethods";
        HashMap PARMS = new HashMap();
        PARMS.put("pid", pid);
        PARMS.put("asOfDateTime", asOfDateTime);
        // Run the method in a SwingWorker thread
        SwingWorker worker = new SwingWorker(PARMS) {

            @Override
            public Object construct() {
                // call wrapped method
                return m_instance.listMethods((java.lang.String) parms
                        .get("pid"), (java.lang.String) parms
                        .get("asOfDateTime"));
            }
        };
        worker.start();
        // The following code will run in the (safe)
        // Swing event dispatcher thread.
        int ms = 0;
        Dimension d = Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words = new ArrayList();
        StringBuffer word = new StringBuffer();
        boolean lastWasCaps = true;
        for (int i = 0; i < METHOD_NAME.length(); i++) {
            char c = METHOD_NAME.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                // char is caps
                if (!lastWasCaps) {
                    // new word
                    words.add(word.toString());
                    word = new StringBuffer();
                }
                word.append(c);
                lastWasCaps = true;
            } else {
                // char is lowercase
                word.append(c);
                lastWasCaps = false;
            }
        }
        words.add(word.toString());
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < words.size(); i++) {
            String lcWord = ((String) words.get(i)).toLowerCase();
            if (i == 0) {
                String firstChar = lcWord.substring(0, 1).toUpperCase();
                char lastChar = lcWord.charAt(lcWord.length() - 1);
                String middle = lcWord.substring(1, lcWord.length() - 1);
                buf.append(firstChar);
                buf.append(middle);
                buf.append(lastChar);
                buf.append(" ");
            } else {
                buf.append(lcWord + " ");
            }
        }
        Administrator.PROGRESS.setString(buf.toString() + ". . .");
        while (!worker.done) {
            try {
                Administrator.PROGRESS.setValue(ms);
                Administrator.PROGRESS
                        .paintImmediately(0,
                                          0,
                                          (int) d.getWidth() - 1,
                                          (int) d.getHeight() - 1);
                Thread.sleep(100);
                ms = ms + 100;
                if (ms >= 2000) ms = 200;
            } catch (InterruptedException ie) {
            }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0,
                                                0,
                                                (int) d.getWidth() - 1,
                                                (int) d.getHeight() - 1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {
        }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");

        // Otherwise, get the value from the
        // worker (returning it if applicable)
        return (List<ObjectMethodsDef>) worker.get();
    }

    @Override
    public List<DatastreamDef> listDatastreams(java.lang.String pid,
                                               java.lang.String asOfDateTime) {
        String METHOD_NAME = "listDatastreams";
        HashMap PARMS = new HashMap();
        PARMS.put("pid", pid);
        PARMS.put("asOfDateTime", asOfDateTime);
        // Run the method in a SwingWorker thread
        SwingWorker worker = new SwingWorker(PARMS) {

            @Override
            public Object construct() {
                // call wrapped method
                return m_instance.listDatastreams((java.lang.String) parms
                        .get("pid"), (java.lang.String) parms
                        .get("asOfDateTime"));
            }
        };
        worker.start();
        // The following code will run in the (safe)
        // Swing event dispatcher thread.
        int ms = 0;
        Dimension d = Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words = new ArrayList();
        StringBuffer word = new StringBuffer();
        boolean lastWasCaps = true;
        for (int i = 0; i < METHOD_NAME.length(); i++) {
            char c = METHOD_NAME.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                // char is caps
                if (!lastWasCaps) {
                    // new word
                    words.add(word.toString());
                    word = new StringBuffer();
                }
                word.append(c);
                lastWasCaps = true;
            } else {
                // char is lowercase
                word.append(c);
                lastWasCaps = false;
            }
        }
        words.add(word.toString());
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < words.size(); i++) {
            String lcWord = ((String) words.get(i)).toLowerCase();
            if (i == 0) {
                String firstChar = lcWord.substring(0, 1).toUpperCase();
                char lastChar = lcWord.charAt(lcWord.length() - 1);
                String middle = lcWord.substring(1, lcWord.length() - 1);
                buf.append(firstChar);
                buf.append(middle);
                buf.append(lastChar);
                buf.append(" ");
            } else {
                buf.append(lcWord + " ");
            }
        }
        Administrator.PROGRESS.setString(buf.toString() + ". . .");
        while (!worker.done) {
            try {
                Administrator.PROGRESS.setValue(ms);
                Administrator.PROGRESS
                        .paintImmediately(0,
                                          0,
                                          (int) d.getWidth() - 1,
                                          (int) d.getHeight() - 1);
                Thread.sleep(100);
                ms = ms + 100;
                if (ms >= 2000) ms = 200;
            } catch (InterruptedException ie) {
            }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0,
                                                0,
                                                (int) d.getWidth() - 1,
                                                (int) d.getHeight() - 1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {
        }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");
        // Otherwise, get the value from the
        // worker (returning it if applicable)
        return (List<DatastreamDef>) worker.get();
    }

    @Override
    public MIMETypedStream getDatastreamDissemination(java.lang.String pid,
                                                      java.lang.String dsID,
                                                      java.lang.String asOfDateTime) {
        String METHOD_NAME = "getDatastreamDissemination";
        HashMap PARMS = new HashMap();
        PARMS.put("pid", pid);
        PARMS.put("dsID", dsID);
        PARMS.put("asOfDateTime", asOfDateTime);
        // Run the method in a SwingWorker thread
        SwingWorker worker = new SwingWorker(PARMS) {

            @Override
            public Object construct() {
                // call wrapped method
                return m_instance
                        .getDatastreamDissemination((java.lang.String) parms
                                .get("pid"), (java.lang.String) parms
                                .get("dsID"), (java.lang.String) parms
                                .get("asOfDateTime"));
            }
        };
        worker.start();
        // The following code will run in the (safe)
        // Swing event dispatcher thread.
        int ms = 0;
        Dimension d = Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words = new ArrayList();
        StringBuffer word = new StringBuffer();
        boolean lastWasCaps = true;
        for (int i = 0; i < METHOD_NAME.length(); i++) {
            char c = METHOD_NAME.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                // char is caps
                if (!lastWasCaps) {
                    // new word
                    words.add(word.toString());
                    word = new StringBuffer();
                }
                word.append(c);
                lastWasCaps = true;
            } else {
                // char is lowercase
                word.append(c);
                lastWasCaps = false;
            }
        }
        words.add(word.toString());
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < words.size(); i++) {
            String lcWord = ((String) words.get(i)).toLowerCase();
            if (i == 0) {
                String firstChar = lcWord.substring(0, 1).toUpperCase();
                char lastChar = lcWord.charAt(lcWord.length() - 1);
                String middle = lcWord.substring(1, lcWord.length() - 1);
                buf.append(firstChar);
                buf.append(middle);
                buf.append(lastChar);
                buf.append(" ");
            } else {
                buf.append(lcWord + " ");
            }
        }
        Administrator.PROGRESS.setString(buf.toString() + ". . .");
        while (!worker.done) {
            try {
                Administrator.PROGRESS.setValue(ms);
                Administrator.PROGRESS
                        .paintImmediately(0,
                                          0,
                                          (int) d.getWidth() - 1,
                                          (int) d.getHeight() - 1);
                Thread.sleep(100);
                ms = ms + 100;
                if (ms >= 2000) ms = 200;
            } catch (InterruptedException ie) {
            }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0,
                                                0,
                                                (int) d.getWidth() - 1,
                                                (int) d.getHeight() - 1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {
        }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");
        // Otherwise, get the value from the
        // worker (returning it if applicable)
        return (MIMETypedStream) worker.get();
    }

    @Override
    public MIMETypedStream getDissemination(java.lang.String pid,
                                            java.lang.String serviceDefinitionPid,
                                            java.lang.String methodName,
                                            org.fcrepo.server.types.mtom.gen.GetDissemination.Parameters parameters,
                                            java.lang.String asOfDateTime) {
        String METHOD_NAME = "getDissemination";
        HashMap PARMS = new HashMap();
        PARMS.put("pid", pid);
        PARMS.put("serviceDefinitionPid", serviceDefinitionPid);
        PARMS.put("methodName", methodName);
        PARMS.put("parameters", parameters);
        PARMS.put("asOfDateTime", asOfDateTime);
        // Run the method in a SwingWorker thread
        SwingWorker worker = new SwingWorker(PARMS) {

            @Override
            public Object construct() {
                // call wrapped method
                return m_instance
                        .getDissemination((java.lang.String) parms.get("pid"),
                                          (java.lang.String) parms
                                                  .get("serviceDefinitionPid"),
                                          (java.lang.String) parms
                                                  .get("methodName"),
                                          (org.fcrepo.server.types.mtom.gen.GetDissemination.Parameters) parms
                                                  .get("parameters"),
                                          (java.lang.String) parms
                                                  .get("asOfDateTime"));
            }
        };
        worker.start();
        // The following code will run in the (safe)
        // Swing event dispatcher thread.
        int ms = 0;
        Dimension d = Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words = new ArrayList();
        StringBuffer word = new StringBuffer();
        boolean lastWasCaps = true;
        for (int i = 0; i < METHOD_NAME.length(); i++) {
            char c = METHOD_NAME.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                // char is caps
                if (!lastWasCaps) {
                    // new word
                    words.add(word.toString());
                    word = new StringBuffer();
                }
                word.append(c);
                lastWasCaps = true;
            } else {
                // char is lowercase
                word.append(c);
                lastWasCaps = false;
            }
        }
        words.add(word.toString());
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < words.size(); i++) {
            String lcWord = ((String) words.get(i)).toLowerCase();
            if (i == 0) {
                String firstChar = lcWord.substring(0, 1).toUpperCase();
                char lastChar = lcWord.charAt(lcWord.length() - 1);
                String middle = lcWord.substring(1, lcWord.length() - 1);
                buf.append(firstChar);
                buf.append(middle);
                buf.append(lastChar);
                buf.append(" ");
            } else {
                buf.append(lcWord + " ");
            }
        }
        Administrator.PROGRESS.setString(buf.toString() + ". . .");
        while (!worker.done) {
            try {
                Administrator.PROGRESS.setValue(ms);
                Administrator.PROGRESS
                        .paintImmediately(0,
                                          0,
                                          (int) d.getWidth() - 1,
                                          (int) d.getHeight() - 1);
                Thread.sleep(100);
                ms = ms + 100;
                if (ms >= 2000) ms = 200;
            } catch (InterruptedException ie) {
            }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0,
                                                0,
                                                (int) d.getWidth() - 1,
                                                (int) d.getHeight() - 1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {
        }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");
        // Otherwise, get the value from the
        // worker (returning it if applicable)
        return (MIMETypedStream) worker.get();
    }

    @Override
    public FieldSearchResult findObjects(org.fcrepo.server.types.mtom.gen.ArrayOfString resultFields,
                                         BigInteger maxResults,
                                         org.fcrepo.server.types.mtom.gen.FieldSearchQuery query) {
        String METHOD_NAME = "findObjects";
        HashMap PARMS = new HashMap();
        PARMS.put("resultFields", resultFields);
        PARMS.put("maxResults", maxResults);
        PARMS.put("query", query);
        // Run the method in a SwingWorker thread
        SwingWorker worker = new SwingWorker(PARMS) {

            @Override
            public Object construct() {
                // call wrapped method
                return m_instance
                        .findObjects((org.fcrepo.server.types.mtom.gen.ArrayOfString) parms
                                             .get("resultFields"),
                                     (BigInteger) parms.get("maxResults"),
                                     (org.fcrepo.server.types.mtom.gen.FieldSearchQuery) parms
                                             .get("query"));
            }
        };
        worker.start();
        // The following code will run in the (safe)
        // Swing event dispatcher thread.
        int ms = 0;
        Dimension d = Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words = new ArrayList();
        StringBuffer word = new StringBuffer();
        boolean lastWasCaps = true;
        for (int i = 0; i < METHOD_NAME.length(); i++) {
            char c = METHOD_NAME.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                // char is caps
                if (!lastWasCaps) {
                    // new word
                    words.add(word.toString());
                    word = new StringBuffer();
                }
                word.append(c);
                lastWasCaps = true;
            } else {
                // char is lowercase
                word.append(c);
                lastWasCaps = false;
            }
        }
        words.add(word.toString());
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < words.size(); i++) {
            String lcWord = ((String) words.get(i)).toLowerCase();
            if (i == 0) {
                String firstChar = lcWord.substring(0, 1).toUpperCase();
                char lastChar = lcWord.charAt(lcWord.length() - 1);
                String middle = lcWord.substring(1, lcWord.length() - 1);
                buf.append(firstChar);
                buf.append(middle);
                buf.append(lastChar);
                buf.append(" ");
            } else {
                buf.append(lcWord + " ");
            }
        }
        Administrator.PROGRESS.setString(buf.toString() + ". . .");
        while (!worker.done) {
            try {
                Administrator.PROGRESS.setValue(ms);
                Administrator.PROGRESS
                        .paintImmediately(0,
                                          0,
                                          (int) d.getWidth() - 1,
                                          (int) d.getHeight() - 1);
                Thread.sleep(100);
                ms = ms + 100;
                if (ms >= 2000) ms = 200;
            } catch (InterruptedException ie) {
            }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0,
                                                0,
                                                (int) d.getWidth() - 1,
                                                (int) d.getHeight() - 1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {
        }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");
        // Otherwise, get the value from the
        // worker (returning it if applicable)
        return (FieldSearchResult) worker.get();
    }

    @Override
    public FieldSearchResult resumeFindObjects(java.lang.String sessionToken) {
        String METHOD_NAME = "resumeFindObjects";
        HashMap PARMS = new HashMap();
        PARMS.put("sessionToken", sessionToken);
        // Run the method in a SwingWorker thread
        SwingWorker worker = new SwingWorker(PARMS) {

            @Override
            public Object construct() {
                // call wrapped method
                return m_instance.resumeFindObjects((java.lang.String) parms
                        .get("sessionToken"));
            }
        };
        worker.start();
        // The following code will run in the (safe)
        // Swing event dispatcher thread.
        int ms = 0;
        Dimension d = Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words = new ArrayList();
        StringBuffer word = new StringBuffer();
        boolean lastWasCaps = true;
        for (int i = 0; i < METHOD_NAME.length(); i++) {
            char c = METHOD_NAME.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                // char is caps
                if (!lastWasCaps) {
                    // new word
                    words.add(word.toString());
                    word = new StringBuffer();
                }
                word.append(c);
                lastWasCaps = true;
            } else {
                // char is lowercase
                word.append(c);
                lastWasCaps = false;
            }
        }
        words.add(word.toString());
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < words.size(); i++) {
            String lcWord = ((String) words.get(i)).toLowerCase();
            if (i == 0) {
                String firstChar = lcWord.substring(0, 1).toUpperCase();
                char lastChar = lcWord.charAt(lcWord.length() - 1);
                String middle = lcWord.substring(1, lcWord.length() - 1);
                buf.append(firstChar);
                buf.append(middle);
                buf.append(lastChar);
                buf.append(" ");
            } else {
                buf.append(lcWord + " ");
            }
        }
        Administrator.PROGRESS.setString(buf.toString() + ". . .");
        while (!worker.done) {
            try {
                Administrator.PROGRESS.setValue(ms);
                Administrator.PROGRESS
                        .paintImmediately(0,
                                          0,
                                          (int) d.getWidth() - 1,
                                          (int) d.getHeight() - 1);
                Thread.sleep(100);
                ms = ms + 100;
                if (ms >= 2000) ms = 200;
            } catch (InterruptedException ie) {
            }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0,
                                                0,
                                                (int) d.getWidth() - 1,
                                                (int) d.getHeight() - 1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {
        }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");
        // Otherwise, get the value from the
        // worker (returning it if applicable)
        return (FieldSearchResult) worker.get();
    }

    @Override
    public List<java.lang.String> getObjectHistory(java.lang.String pid) {
        String METHOD_NAME = "getObjectHistory";
        HashMap PARMS = new HashMap();
        PARMS.put("pid", pid);
        // Run the method in a SwingWorker thread
        SwingWorker worker = new SwingWorker(PARMS) {

            @Override
            public Object construct() {
                // call wrapped method
                return m_instance.getObjectHistory((java.lang.String) parms
                        .get("pid"));
            }
        };
        worker.start();
        // The following code will run in the (safe)
        // Swing event dispatcher thread.
        int ms = 0;
        Dimension d = Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words = new ArrayList();
        StringBuffer word = new StringBuffer();
        boolean lastWasCaps = true;
        for (int i = 0; i < METHOD_NAME.length(); i++) {
            char c = METHOD_NAME.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                // char is caps
                if (!lastWasCaps) {
                    // new word
                    words.add(word.toString());
                    word = new StringBuffer();
                }
                word.append(c);
                lastWasCaps = true;
            } else {
                // char is lowercase
                word.append(c);
                lastWasCaps = false;
            }
        }
        words.add(word.toString());
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < words.size(); i++) {
            String lcWord = ((String) words.get(i)).toLowerCase();
            if (i == 0) {
                String firstChar = lcWord.substring(0, 1).toUpperCase();
                char lastChar = lcWord.charAt(lcWord.length() - 1);
                String middle = lcWord.substring(1, lcWord.length() - 1);
                buf.append(firstChar);
                buf.append(middle);
                buf.append(lastChar);
                buf.append(" ");
            } else {
                buf.append(lcWord + " ");
            }
        }
        Administrator.PROGRESS.setString(buf.toString() + ". . .");
        while (!worker.done) {
            try {
                Administrator.PROGRESS.setValue(ms);
                Administrator.PROGRESS
                        .paintImmediately(0,
                                          0,
                                          (int) d.getWidth() - 1,
                                          (int) d.getHeight() - 1);
                Thread.sleep(100);
                ms = ms + 100;
                if (ms >= 2000) ms = 200;
            } catch (InterruptedException ie) {
            }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0,
                                                0,
                                                (int) d.getWidth() - 1,
                                                (int) d.getHeight() - 1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {
        }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");

        // Otherwise, get the value from the
        // worker (returning it if applicable)
        return (List<java.lang.String>) worker.get();
    }

}
