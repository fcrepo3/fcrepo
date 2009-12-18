package fedora.client;

import java.util.HashMap; // needed by generated code

import java.awt.Dimension;
import java.rmi.RemoteException;
import java.util.ArrayList;

public class APIMStubWrapper
        implements fedora.server.management.FedoraAPIM {

    /** The wrapped instance */
    private fedora.server.management.FedoraAPIM m_instance;

    public APIMStubWrapper(fedora.server.management.FedoraAPIM instance) {
        m_instance=instance;
    }

    public java.lang.String ingest(byte[] objectXML, java.lang.String format, java.lang.String logMessage) throws java.rmi.RemoteException {
        String METHOD_NAME="ingest";
        HashMap PARMS=new HashMap();
        PARMS.put("objectXML", objectXML);
        PARMS.put("format", format);
        PARMS.put("logMessage", logMessage);
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.ingest((byte[]) parms.get("objectXML"), (java.lang.String) parms.get("format"), (java.lang.String) parms.get("logMessage"));

                } catch (RemoteException e) {
                    thrownException=e;
                }
                return "";
            }
        };
        worker.start();
        // The following code will run in the (safe) 
        // Swing event dispatcher thread.
        int ms=0;
        Dimension d=Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words=new ArrayList();
        StringBuffer word=new StringBuffer();
        boolean lastWasCaps=true;
        for (int i=0; i<METHOD_NAME.length(); i++) {
            char c=METHOD_NAME.charAt(i);
            if (c>='A' && c<='Z') {
               // char is caps
               if (!lastWasCaps) {
                   // new word
                   words.add(word.toString());
                   word=new StringBuffer();
               }
               word.append(c);
               lastWasCaps=true;
            } else {
               // char is lowercase
               word.append(c);
               lastWasCaps=false;
            }
        }
        words.add(word.toString());
        StringBuffer buf=new StringBuffer();
        for (int i=0; i<words.size(); i++) {
            String lcWord=((String) words.get(i)).toLowerCase();
            if (i==0) {
                String firstChar=lcWord.substring(0, 1).toUpperCase();
                char lastChar=lcWord.charAt(lcWord.length()-1);
                String middle=lcWord.substring(1, lcWord.length()-1);
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
                Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
                Thread.sleep(100);
                ms=ms+100;
                if (ms>=2000) ms=200;
            } catch (InterruptedException ie) { }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) { }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");
        
        // The worker is finished.  
        // Throw exception if caught.
        if (worker.thrownException!=null) {
            throw (RemoteException) worker.thrownException;
        }
        
        // Otherwise, get the value from the 
        // worker (returning it if applicable)
        return (java.lang.String) worker.get();
    }

    public java.lang.String modifyObject(java.lang.String pid, java.lang.String state, java.lang.String label, java.lang.String ownerId, java.lang.String logMessage) throws java.rmi.RemoteException {
        String METHOD_NAME="modifyObject";
        HashMap PARMS=new HashMap();
        PARMS.put("pid", pid);
        PARMS.put("state", state);
        PARMS.put("label", label);
        PARMS.put("ownerId", ownerId);
        PARMS.put("logMessage", logMessage);
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.modifyObject((java.lang.String) parms.get("pid"), (java.lang.String) parms.get("state"), (java.lang.String) parms.get("label"), (java.lang.String) parms.get("ownerId"), (java.lang.String) parms.get("logMessage"));

                } catch (RemoteException e) {
                    thrownException=e;
                }
                return "";
            }
        };
        worker.start();
        // The following code will run in the (safe) 
        // Swing event dispatcher thread.
        int ms=0;
        Dimension d=Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words=new ArrayList();
        StringBuffer word=new StringBuffer();
        boolean lastWasCaps=true;
        for (int i=0; i<METHOD_NAME.length(); i++) {
            char c=METHOD_NAME.charAt(i);
            if (c>='A' && c<='Z') {
               // char is caps
               if (!lastWasCaps) {
                   // new word
                   words.add(word.toString());
                   word=new StringBuffer();
               }
               word.append(c);
               lastWasCaps=true;
            } else {
               // char is lowercase
               word.append(c);
               lastWasCaps=false;
            }
        }
        words.add(word.toString());
        StringBuffer buf=new StringBuffer();
        for (int i=0; i<words.size(); i++) {
            String lcWord=((String) words.get(i)).toLowerCase();
            if (i==0) {
                String firstChar=lcWord.substring(0, 1).toUpperCase();
                char lastChar=lcWord.charAt(lcWord.length()-1);
                String middle=lcWord.substring(1, lcWord.length()-1);
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
                Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
                Thread.sleep(100);
                ms=ms+100;
                if (ms>=2000) ms=200;
            } catch (InterruptedException ie) { }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) { }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");
        
        // The worker is finished.  
        // Throw exception if caught.
        if (worker.thrownException!=null) {
            throw (RemoteException) worker.thrownException;
        }
        
        // Otherwise, get the value from the 
        // worker (returning it if applicable)
        return (java.lang.String) worker.get();
    }

    public byte[] getObjectXML(java.lang.String pid) throws java.rmi.RemoteException {
        String METHOD_NAME="getObjectXML";
        HashMap PARMS=new HashMap();
        PARMS.put("pid", pid);
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.getObjectXML((java.lang.String) parms.get("pid"));

                } catch (RemoteException e) {
                    thrownException=e;
                }
                return "";
            }
        };
        worker.start();
        // The following code will run in the (safe) 
        // Swing event dispatcher thread.
        int ms=0;
        Dimension d=Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words=new ArrayList();
        StringBuffer word=new StringBuffer();
        boolean lastWasCaps=true;
        for (int i=0; i<METHOD_NAME.length(); i++) {
            char c=METHOD_NAME.charAt(i);
            if (c>='A' && c<='Z') {
               // char is caps
               if (!lastWasCaps) {
                   // new word
                   words.add(word.toString());
                   word=new StringBuffer();
               }
               word.append(c);
               lastWasCaps=true;
            } else {
               // char is lowercase
               word.append(c);
               lastWasCaps=false;
            }
        }
        words.add(word.toString());
        StringBuffer buf=new StringBuffer();
        for (int i=0; i<words.size(); i++) {
            String lcWord=((String) words.get(i)).toLowerCase();
            if (i==0) {
                String firstChar=lcWord.substring(0, 1).toUpperCase();
                char lastChar=lcWord.charAt(lcWord.length()-1);
                String middle=lcWord.substring(1, lcWord.length()-1);
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
                Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
                Thread.sleep(100);
                ms=ms+100;
                if (ms>=2000) ms=200;
            } catch (InterruptedException ie) { }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) { }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");
        
        // The worker is finished.  
        // Throw exception if caught.
        if (worker.thrownException!=null) {
            throw (RemoteException) worker.thrownException;
        }
        
        // Otherwise, get the value from the 
        // worker (returning it if applicable)
        return (byte[]) worker.get();
    }

    public byte[] export(java.lang.String pid, java.lang.String format, java.lang.String context) throws java.rmi.RemoteException {
        String METHOD_NAME="export";
        HashMap PARMS=new HashMap();
        PARMS.put("pid", pid);
        PARMS.put("format", format);
        PARMS.put("context", context);
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.export((java.lang.String) parms.get("pid"), (java.lang.String) parms.get("format"), (java.lang.String) parms.get("context"));

                } catch (RemoteException e) {
                    thrownException=e;
                }
                return "";
            }
        };
        worker.start();
        // The following code will run in the (safe) 
        // Swing event dispatcher thread.
        int ms=0;
        Dimension d=Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words=new ArrayList();
        StringBuffer word=new StringBuffer();
        boolean lastWasCaps=true;
        for (int i=0; i<METHOD_NAME.length(); i++) {
            char c=METHOD_NAME.charAt(i);
            if (c>='A' && c<='Z') {
               // char is caps
               if (!lastWasCaps) {
                   // new word
                   words.add(word.toString());
                   word=new StringBuffer();
               }
               word.append(c);
               lastWasCaps=true;
            } else {
               // char is lowercase
               word.append(c);
               lastWasCaps=false;
            }
        }
        words.add(word.toString());
        StringBuffer buf=new StringBuffer();
        for (int i=0; i<words.size(); i++) {
            String lcWord=((String) words.get(i)).toLowerCase();
            if (i==0) {
                String firstChar=lcWord.substring(0, 1).toUpperCase();
                char lastChar=lcWord.charAt(lcWord.length()-1);
                String middle=lcWord.substring(1, lcWord.length()-1);
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
                Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
                Thread.sleep(100);
                ms=ms+100;
                if (ms>=2000) ms=200;
            } catch (InterruptedException ie) { }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) { }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");
        
        // The worker is finished.  
        // Throw exception if caught.
        if (worker.thrownException!=null) {
            throw (RemoteException) worker.thrownException;
        }
        
        // Otherwise, get the value from the 
        // worker (returning it if applicable)
        return (byte[]) worker.get();
    }

    public java.lang.String purgeObject(java.lang.String pid, java.lang.String logMessage, boolean force) throws java.rmi.RemoteException {
        String METHOD_NAME="purgeObject";
        HashMap PARMS=new HashMap();
        PARMS.put("pid", pid);
        PARMS.put("logMessage", logMessage);
        PARMS.put("force", new Boolean(force));
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.purgeObject((java.lang.String) parms.get("pid"), (java.lang.String) parms.get("logMessage"), ((Boolean) parms.get("force")).booleanValue());

                } catch (RemoteException e) {
                    thrownException=e;
                }
                return "";
            }
        };
        worker.start();
        // The following code will run in the (safe) 
        // Swing event dispatcher thread.
        int ms=0;
        Dimension d=Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words=new ArrayList();
        StringBuffer word=new StringBuffer();
        boolean lastWasCaps=true;
        for (int i=0; i<METHOD_NAME.length(); i++) {
            char c=METHOD_NAME.charAt(i);
            if (c>='A' && c<='Z') {
               // char is caps
               if (!lastWasCaps) {
                   // new word
                   words.add(word.toString());
                   word=new StringBuffer();
               }
               word.append(c);
               lastWasCaps=true;
            } else {
               // char is lowercase
               word.append(c);
               lastWasCaps=false;
            }
        }
        words.add(word.toString());
        StringBuffer buf=new StringBuffer();
        for (int i=0; i<words.size(); i++) {
            String lcWord=((String) words.get(i)).toLowerCase();
            if (i==0) {
                String firstChar=lcWord.substring(0, 1).toUpperCase();
                char lastChar=lcWord.charAt(lcWord.length()-1);
                String middle=lcWord.substring(1, lcWord.length()-1);
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
                Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
                Thread.sleep(100);
                ms=ms+100;
                if (ms>=2000) ms=200;
            } catch (InterruptedException ie) { }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) { }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");
        
        // The worker is finished.  
        // Throw exception if caught.
        if (worker.thrownException!=null) {
            throw (RemoteException) worker.thrownException;
        }
        
        // Otherwise, get the value from the 
        // worker (returning it if applicable)
        return (java.lang.String) worker.get();
    }

    public java.lang.String addDatastream(java.lang.String pid, java.lang.String dsID, java.lang.String[] altIDs, java.lang.String dsLabel, boolean versionable, java.lang.String MIMEType, java.lang.String formatURI, java.lang.String dsLocation, java.lang.String controlGroup, java.lang.String dsState, java.lang.String checksumType, java.lang.String checksum, java.lang.String logMessage) throws java.rmi.RemoteException {
        String METHOD_NAME="addDatastream";
        HashMap PARMS=new HashMap();
        PARMS.put("pid", pid);
        PARMS.put("dsID", dsID);
        PARMS.put("altIDs", altIDs);
        PARMS.put("dsLabel", dsLabel);
        PARMS.put("versionable", new Boolean(versionable));
        PARMS.put("MIMEType", MIMEType);
        PARMS.put("formatURI", formatURI);
        PARMS.put("dsLocation", dsLocation);
        PARMS.put("controlGroup", controlGroup);
        PARMS.put("dsState", dsState);
        PARMS.put("checksumType", checksumType);
        PARMS.put("checksum", checksum);
        PARMS.put("logMessage", logMessage);
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.addDatastream((java.lang.String) parms.get("pid"), (java.lang.String) parms.get("dsID"), (java.lang.String[]) parms.get("altIDs"), (java.lang.String) parms.get("dsLabel"), ((Boolean) parms.get("versionable")).booleanValue(), (java.lang.String) parms.get("MIMEType"), (java.lang.String) parms.get("formatURI"), (java.lang.String) parms.get("dsLocation"), (java.lang.String) parms.get("controlGroup"), (java.lang.String) parms.get("dsState"), (java.lang.String) parms.get("checksumType"), (java.lang.String) parms.get("checksum"), (java.lang.String) parms.get("logMessage"));

                } catch (RemoteException e) {
                    thrownException=e;
                }
                return "";
            }
        };
        worker.start();
        // The following code will run in the (safe) 
        // Swing event dispatcher thread.
        int ms=0;
        Dimension d=Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words=new ArrayList();
        StringBuffer word=new StringBuffer();
        boolean lastWasCaps=true;
        for (int i=0; i<METHOD_NAME.length(); i++) {
            char c=METHOD_NAME.charAt(i);
            if (c>='A' && c<='Z') {
               // char is caps
               if (!lastWasCaps) {
                   // new word
                   words.add(word.toString());
                   word=new StringBuffer();
               }
               word.append(c);
               lastWasCaps=true;
            } else {
               // char is lowercase
               word.append(c);
               lastWasCaps=false;
            }
        }
        words.add(word.toString());
        StringBuffer buf=new StringBuffer();
        for (int i=0; i<words.size(); i++) {
            String lcWord=((String) words.get(i)).toLowerCase();
            if (i==0) {
                String firstChar=lcWord.substring(0, 1).toUpperCase();
                char lastChar=lcWord.charAt(lcWord.length()-1);
                String middle=lcWord.substring(1, lcWord.length()-1);
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
                Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
                Thread.sleep(100);
                ms=ms+100;
                if (ms>=2000) ms=200;
            } catch (InterruptedException ie) { }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) { }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");
        
        // The worker is finished.  
        // Throw exception if caught.
        if (worker.thrownException!=null) {
            throw (RemoteException) worker.thrownException;
        }
        
        // Otherwise, get the value from the 
        // worker (returning it if applicable)
        return (java.lang.String) worker.get();
    }

    public java.lang.String modifyDatastreamByReference(java.lang.String pid, java.lang.String dsID, java.lang.String[] altIDs, java.lang.String dsLabel, java.lang.String MIMEType, java.lang.String formatURI, java.lang.String dsLocation, java.lang.String checksumType, java.lang.String checksum, java.lang.String logMessage, boolean force) throws java.rmi.RemoteException {
        String METHOD_NAME="modifyDatastreamByReference";
        HashMap PARMS=new HashMap();
        PARMS.put("pid", pid);
        PARMS.put("dsID", dsID);
        PARMS.put("altIDs", altIDs);
        PARMS.put("dsLabel", dsLabel);
        PARMS.put("MIMEType", MIMEType);
        PARMS.put("formatURI", formatURI);
        PARMS.put("dsLocation", dsLocation);
        PARMS.put("checksumType", checksumType);
        PARMS.put("checksum", checksum);
        PARMS.put("logMessage", logMessage);
        PARMS.put("force", new Boolean(force));
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.modifyDatastreamByReference((java.lang.String) parms.get("pid"), (java.lang.String) parms.get("dsID"), (java.lang.String[]) parms.get("altIDs"), (java.lang.String) parms.get("dsLabel"), (java.lang.String) parms.get("MIMEType"), (java.lang.String) parms.get("formatURI"), (java.lang.String) parms.get("dsLocation"), (java.lang.String) parms.get("checksumType"), (java.lang.String) parms.get("checksum"), (java.lang.String) parms.get("logMessage"), ((Boolean) parms.get("force")).booleanValue());

                } catch (RemoteException e) {
                    thrownException=e;
                }
                return "";
            }
        };
        worker.start();
        // The following code will run in the (safe) 
        // Swing event dispatcher thread.
        int ms=0;
        Dimension d=Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words=new ArrayList();
        StringBuffer word=new StringBuffer();
        boolean lastWasCaps=true;
        for (int i=0; i<METHOD_NAME.length(); i++) {
            char c=METHOD_NAME.charAt(i);
            if (c>='A' && c<='Z') {
               // char is caps
               if (!lastWasCaps) {
                   // new word
                   words.add(word.toString());
                   word=new StringBuffer();
               }
               word.append(c);
               lastWasCaps=true;
            } else {
               // char is lowercase
               word.append(c);
               lastWasCaps=false;
            }
        }
        words.add(word.toString());
        StringBuffer buf=new StringBuffer();
        for (int i=0; i<words.size(); i++) {
            String lcWord=((String) words.get(i)).toLowerCase();
            if (i==0) {
                String firstChar=lcWord.substring(0, 1).toUpperCase();
                char lastChar=lcWord.charAt(lcWord.length()-1);
                String middle=lcWord.substring(1, lcWord.length()-1);
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
                Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
                Thread.sleep(100);
                ms=ms+100;
                if (ms>=2000) ms=200;
            } catch (InterruptedException ie) { }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) { }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");
        
        // The worker is finished.  
        // Throw exception if caught.
        if (worker.thrownException!=null) {
            throw (RemoteException) worker.thrownException;
        }
        
        // Otherwise, get the value from the 
        // worker (returning it if applicable)
        return (java.lang.String) worker.get();
    }

    public java.lang.String modifyDatastreamByValue(java.lang.String pid, java.lang.String dsID, java.lang.String[] altIDs, java.lang.String dsLabel, java.lang.String MIMEType, java.lang.String formatURI, byte[] dsContent, java.lang.String checksumType, java.lang.String checksum, java.lang.String logMessage, boolean force) throws java.rmi.RemoteException {
        String METHOD_NAME="modifyDatastreamByValue";
        HashMap PARMS=new HashMap();
        PARMS.put("pid", pid);
        PARMS.put("dsID", dsID);
        PARMS.put("altIDs", altIDs);
        PARMS.put("dsLabel", dsLabel);
        PARMS.put("MIMEType", MIMEType);
        PARMS.put("formatURI", formatURI);
        PARMS.put("dsContent", dsContent);
        PARMS.put("checksumType", checksumType);
        PARMS.put("checksum", checksum);
        PARMS.put("logMessage", logMessage);
        PARMS.put("force", new Boolean(force));
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.modifyDatastreamByValue((java.lang.String) parms.get("pid"), (java.lang.String) parms.get("dsID"), (java.lang.String[]) parms.get("altIDs"), (java.lang.String) parms.get("dsLabel"), (java.lang.String) parms.get("MIMEType"), (java.lang.String) parms.get("formatURI"), (byte[]) parms.get("dsContent"), (java.lang.String) parms.get("checksumType"), (java.lang.String) parms.get("checksum"), (java.lang.String) parms.get("logMessage"), ((Boolean) parms.get("force")).booleanValue());

                } catch (RemoteException e) {
                    thrownException=e;
                }
                return "";
            }
        };
        worker.start();
        // The following code will run in the (safe) 
        // Swing event dispatcher thread.
        int ms=0;
        Dimension d=Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words=new ArrayList();
        StringBuffer word=new StringBuffer();
        boolean lastWasCaps=true;
        for (int i=0; i<METHOD_NAME.length(); i++) {
            char c=METHOD_NAME.charAt(i);
            if (c>='A' && c<='Z') {
               // char is caps
               if (!lastWasCaps) {
                   // new word
                   words.add(word.toString());
                   word=new StringBuffer();
               }
               word.append(c);
               lastWasCaps=true;
            } else {
               // char is lowercase
               word.append(c);
               lastWasCaps=false;
            }
        }
        words.add(word.toString());
        StringBuffer buf=new StringBuffer();
        for (int i=0; i<words.size(); i++) {
            String lcWord=((String) words.get(i)).toLowerCase();
            if (i==0) {
                String firstChar=lcWord.substring(0, 1).toUpperCase();
                char lastChar=lcWord.charAt(lcWord.length()-1);
                String middle=lcWord.substring(1, lcWord.length()-1);
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
                Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
                Thread.sleep(100);
                ms=ms+100;
                if (ms>=2000) ms=200;
            } catch (InterruptedException ie) { }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) { }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");
        
        // The worker is finished.  
        // Throw exception if caught.
        if (worker.thrownException!=null) {
            throw (RemoteException) worker.thrownException;
        }
        
        // Otherwise, get the value from the 
        // worker (returning it if applicable)
        return (java.lang.String) worker.get();
    }

    public java.lang.String setDatastreamState(java.lang.String pid, java.lang.String dsID, java.lang.String dsState, java.lang.String logMessage) throws java.rmi.RemoteException {
        String METHOD_NAME="setDatastreamState";
        HashMap PARMS=new HashMap();
        PARMS.put("pid", pid);
        PARMS.put("dsID", dsID);
        PARMS.put("dsState", dsState);
        PARMS.put("logMessage", logMessage);
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.setDatastreamState((java.lang.String) parms.get("pid"), (java.lang.String) parms.get("dsID"), (java.lang.String) parms.get("dsState"), (java.lang.String) parms.get("logMessage"));

                } catch (RemoteException e) {
                    thrownException=e;
                }
                return "";
            }
        };
        worker.start();
        // The following code will run in the (safe) 
        // Swing event dispatcher thread.
        int ms=0;
        Dimension d=Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words=new ArrayList();
        StringBuffer word=new StringBuffer();
        boolean lastWasCaps=true;
        for (int i=0; i<METHOD_NAME.length(); i++) {
            char c=METHOD_NAME.charAt(i);
            if (c>='A' && c<='Z') {
               // char is caps
               if (!lastWasCaps) {
                   // new word
                   words.add(word.toString());
                   word=new StringBuffer();
               }
               word.append(c);
               lastWasCaps=true;
            } else {
               // char is lowercase
               word.append(c);
               lastWasCaps=false;
            }
        }
        words.add(word.toString());
        StringBuffer buf=new StringBuffer();
        for (int i=0; i<words.size(); i++) {
            String lcWord=((String) words.get(i)).toLowerCase();
            if (i==0) {
                String firstChar=lcWord.substring(0, 1).toUpperCase();
                char lastChar=lcWord.charAt(lcWord.length()-1);
                String middle=lcWord.substring(1, lcWord.length()-1);
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
                Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
                Thread.sleep(100);
                ms=ms+100;
                if (ms>=2000) ms=200;
            } catch (InterruptedException ie) { }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) { }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");
        
        // The worker is finished.  
        // Throw exception if caught.
        if (worker.thrownException!=null) {
            throw (RemoteException) worker.thrownException;
        }
        
        // Otherwise, get the value from the 
        // worker (returning it if applicable)
        return (java.lang.String) worker.get();
    }

    public java.lang.String setDatastreamVersionable(java.lang.String pid, java.lang.String dsID, boolean versionable, java.lang.String logMessage) throws java.rmi.RemoteException {
        String METHOD_NAME="setDatastreamVersionable";
        HashMap PARMS=new HashMap();
        PARMS.put("pid", pid);
        PARMS.put("dsID", dsID);
        PARMS.put("versionable", new Boolean(versionable));
        PARMS.put("logMessage", logMessage);
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.setDatastreamVersionable((java.lang.String) parms.get("pid"), (java.lang.String) parms.get("dsID"), ((Boolean) parms.get("versionable")).booleanValue(), (java.lang.String) parms.get("logMessage"));

                } catch (RemoteException e) {
                    thrownException=e;
                }
                return "";
            }
        };
        worker.start();
        // The following code will run in the (safe) 
        // Swing event dispatcher thread.
        int ms=0;
        Dimension d=Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words=new ArrayList();
        StringBuffer word=new StringBuffer();
        boolean lastWasCaps=true;
        for (int i=0; i<METHOD_NAME.length(); i++) {
            char c=METHOD_NAME.charAt(i);
            if (c>='A' && c<='Z') {
               // char is caps
               if (!lastWasCaps) {
                   // new word
                   words.add(word.toString());
                   word=new StringBuffer();
               }
               word.append(c);
               lastWasCaps=true;
            } else {
               // char is lowercase
               word.append(c);
               lastWasCaps=false;
            }
        }
        words.add(word.toString());
        StringBuffer buf=new StringBuffer();
        for (int i=0; i<words.size(); i++) {
            String lcWord=((String) words.get(i)).toLowerCase();
            if (i==0) {
                String firstChar=lcWord.substring(0, 1).toUpperCase();
                char lastChar=lcWord.charAt(lcWord.length()-1);
                String middle=lcWord.substring(1, lcWord.length()-1);
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
                Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
                Thread.sleep(100);
                ms=ms+100;
                if (ms>=2000) ms=200;
            } catch (InterruptedException ie) { }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) { }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");
        
        // The worker is finished.  
        // Throw exception if caught.
        if (worker.thrownException!=null) {
            throw (RemoteException) worker.thrownException;
        }
        
        // Otherwise, get the value from the 
        // worker (returning it if applicable)
        return (java.lang.String) worker.get();
    }

    public java.lang.String compareDatastreamChecksum(java.lang.String pid, java.lang.String dsID, java.lang.String versionDate) throws java.rmi.RemoteException {
        String METHOD_NAME="compareDatastreamChecksum";
        HashMap PARMS=new HashMap();
        PARMS.put("pid", pid);
        PARMS.put("dsID", dsID);
        PARMS.put("versionDate", versionDate);
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.compareDatastreamChecksum((java.lang.String) parms.get("pid"), (java.lang.String) parms.get("dsID"), (java.lang.String) parms.get("versionDate"));

                } catch (RemoteException e) {
                    thrownException=e;
                }
                return "";
            }
        };
        worker.start();
        // The following code will run in the (safe) 
        // Swing event dispatcher thread.
        int ms=0;
        Dimension d=Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words=new ArrayList();
        StringBuffer word=new StringBuffer();
        boolean lastWasCaps=true;
        for (int i=0; i<METHOD_NAME.length(); i++) {
            char c=METHOD_NAME.charAt(i);
            if (c>='A' && c<='Z') {
               // char is caps
               if (!lastWasCaps) {
                   // new word
                   words.add(word.toString());
                   word=new StringBuffer();
               }
               word.append(c);
               lastWasCaps=true;
            } else {
               // char is lowercase
               word.append(c);
               lastWasCaps=false;
            }
        }
        words.add(word.toString());
        StringBuffer buf=new StringBuffer();
        for (int i=0; i<words.size(); i++) {
            String lcWord=((String) words.get(i)).toLowerCase();
            if (i==0) {
                String firstChar=lcWord.substring(0, 1).toUpperCase();
                char lastChar=lcWord.charAt(lcWord.length()-1);
                String middle=lcWord.substring(1, lcWord.length()-1);
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
                Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
                Thread.sleep(100);
                ms=ms+100;
                if (ms>=2000) ms=200;
            } catch (InterruptedException ie) { }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) { }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");
        
        // The worker is finished.  
        // Throw exception if caught.
        if (worker.thrownException!=null) {
            throw (RemoteException) worker.thrownException;
        }
        
        // Otherwise, get the value from the 
        // worker (returning it if applicable)
        return (java.lang.String) worker.get();
    }

    public fedora.server.types.gen.Datastream getDatastream(java.lang.String pid, java.lang.String dsID, java.lang.String asOfDateTime) throws java.rmi.RemoteException {
        String METHOD_NAME="getDatastream";
        HashMap PARMS=new HashMap();
        PARMS.put("pid", pid);
        PARMS.put("dsID", dsID);
        PARMS.put("asOfDateTime", asOfDateTime);
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.getDatastream((java.lang.String) parms.get("pid"), (java.lang.String) parms.get("dsID"), (java.lang.String) parms.get("asOfDateTime"));

                } catch (RemoteException e) {
                    thrownException=e;
                }
                return "";
            }
        };
        worker.start();
        // The following code will run in the (safe) 
        // Swing event dispatcher thread.
        int ms=0;
        Dimension d=Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words=new ArrayList();
        StringBuffer word=new StringBuffer();
        boolean lastWasCaps=true;
        for (int i=0; i<METHOD_NAME.length(); i++) {
            char c=METHOD_NAME.charAt(i);
            if (c>='A' && c<='Z') {
               // char is caps
               if (!lastWasCaps) {
                   // new word
                   words.add(word.toString());
                   word=new StringBuffer();
               }
               word.append(c);
               lastWasCaps=true;
            } else {
               // char is lowercase
               word.append(c);
               lastWasCaps=false;
            }
        }
        words.add(word.toString());
        StringBuffer buf=new StringBuffer();
        for (int i=0; i<words.size(); i++) {
            String lcWord=((String) words.get(i)).toLowerCase();
            if (i==0) {
                String firstChar=lcWord.substring(0, 1).toUpperCase();
                char lastChar=lcWord.charAt(lcWord.length()-1);
                String middle=lcWord.substring(1, lcWord.length()-1);
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
                Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
                Thread.sleep(100);
                ms=ms+100;
                if (ms>=2000) ms=200;
            } catch (InterruptedException ie) { }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) { }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");
        
        // The worker is finished.  
        // Throw exception if caught.
        if (worker.thrownException!=null) {
            throw (RemoteException) worker.thrownException;
        }
        
        // Otherwise, get the value from the 
        // worker (returning it if applicable)
        return (fedora.server.types.gen.Datastream) worker.get();
    }

    public fedora.server.types.gen.Datastream[] getDatastreams(java.lang.String pid, java.lang.String asOfDateTime, java.lang.String dsState) throws java.rmi.RemoteException {
        String METHOD_NAME="getDatastreams";
        HashMap PARMS=new HashMap();
        PARMS.put("pid", pid);
        PARMS.put("asOfDateTime", asOfDateTime);
        PARMS.put("dsState", dsState);
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.getDatastreams((java.lang.String) parms.get("pid"), (java.lang.String) parms.get("asOfDateTime"), (java.lang.String) parms.get("dsState"));

                } catch (RemoteException e) {
                    thrownException=e;
                }
                return "";
            }
        };
        worker.start();
        // The following code will run in the (safe) 
        // Swing event dispatcher thread.
        int ms=0;
        Dimension d=Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words=new ArrayList();
        StringBuffer word=new StringBuffer();
        boolean lastWasCaps=true;
        for (int i=0; i<METHOD_NAME.length(); i++) {
            char c=METHOD_NAME.charAt(i);
            if (c>='A' && c<='Z') {
               // char is caps
               if (!lastWasCaps) {
                   // new word
                   words.add(word.toString());
                   word=new StringBuffer();
               }
               word.append(c);
               lastWasCaps=true;
            } else {
               // char is lowercase
               word.append(c);
               lastWasCaps=false;
            }
        }
        words.add(word.toString());
        StringBuffer buf=new StringBuffer();
        for (int i=0; i<words.size(); i++) {
            String lcWord=((String) words.get(i)).toLowerCase();
            if (i==0) {
                String firstChar=lcWord.substring(0, 1).toUpperCase();
                char lastChar=lcWord.charAt(lcWord.length()-1);
                String middle=lcWord.substring(1, lcWord.length()-1);
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
                Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
                Thread.sleep(100);
                ms=ms+100;
                if (ms>=2000) ms=200;
            } catch (InterruptedException ie) { }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) { }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");
        
        // The worker is finished.  
        // Throw exception if caught.
        if (worker.thrownException!=null) {
            throw (RemoteException) worker.thrownException;
        }
        
        // Otherwise, get the value from the 
        // worker (returning it if applicable)
        return (fedora.server.types.gen.Datastream[]) worker.get();
    }

    public fedora.server.types.gen.Datastream[] getDatastreamHistory(java.lang.String pid, java.lang.String dsID) throws java.rmi.RemoteException {
        String METHOD_NAME="getDatastreamHistory";
        HashMap PARMS=new HashMap();
        PARMS.put("pid", pid);
        PARMS.put("dsID", dsID);
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.getDatastreamHistory((java.lang.String) parms.get("pid"), (java.lang.String) parms.get("dsID"));

                } catch (RemoteException e) {
                    thrownException=e;
                }
                return "";
            }
        };
        worker.start();
        // The following code will run in the (safe) 
        // Swing event dispatcher thread.
        int ms=0;
        Dimension d=Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words=new ArrayList();
        StringBuffer word=new StringBuffer();
        boolean lastWasCaps=true;
        for (int i=0; i<METHOD_NAME.length(); i++) {
            char c=METHOD_NAME.charAt(i);
            if (c>='A' && c<='Z') {
               // char is caps
               if (!lastWasCaps) {
                   // new word
                   words.add(word.toString());
                   word=new StringBuffer();
               }
               word.append(c);
               lastWasCaps=true;
            } else {
               // char is lowercase
               word.append(c);
               lastWasCaps=false;
            }
        }
        words.add(word.toString());
        StringBuffer buf=new StringBuffer();
        for (int i=0; i<words.size(); i++) {
            String lcWord=((String) words.get(i)).toLowerCase();
            if (i==0) {
                String firstChar=lcWord.substring(0, 1).toUpperCase();
                char lastChar=lcWord.charAt(lcWord.length()-1);
                String middle=lcWord.substring(1, lcWord.length()-1);
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
                Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
                Thread.sleep(100);
                ms=ms+100;
                if (ms>=2000) ms=200;
            } catch (InterruptedException ie) { }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) { }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");
        
        // The worker is finished.  
        // Throw exception if caught.
        if (worker.thrownException!=null) {
            throw (RemoteException) worker.thrownException;
        }
        
        // Otherwise, get the value from the 
        // worker (returning it if applicable)
        return (fedora.server.types.gen.Datastream[]) worker.get();
    }

    public java.lang.String[] purgeDatastream(java.lang.String pid, java.lang.String dsID, java.lang.String startDT, java.lang.String endDT, java.lang.String logMessage, boolean force) throws java.rmi.RemoteException {
        String METHOD_NAME="purgeDatastream";
        HashMap PARMS=new HashMap();
        PARMS.put("pid", pid);
        PARMS.put("dsID", dsID);
        PARMS.put("startDT", startDT);
        PARMS.put("endDT", endDT);
        PARMS.put("logMessage", logMessage);
        PARMS.put("force", new Boolean(force));
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.purgeDatastream((java.lang.String) parms.get("pid"), (java.lang.String) parms.get("dsID"), (java.lang.String) parms.get("startDT"), (java.lang.String) parms.get("endDT"), (java.lang.String) parms.get("logMessage"), ((Boolean) parms.get("force")).booleanValue());

                } catch (RemoteException e) {
                    thrownException=e;
                }
                return "";
            }
        };
        worker.start();
        // The following code will run in the (safe) 
        // Swing event dispatcher thread.
        int ms=0;
        Dimension d=Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words=new ArrayList();
        StringBuffer word=new StringBuffer();
        boolean lastWasCaps=true;
        for (int i=0; i<METHOD_NAME.length(); i++) {
            char c=METHOD_NAME.charAt(i);
            if (c>='A' && c<='Z') {
               // char is caps
               if (!lastWasCaps) {
                   // new word
                   words.add(word.toString());
                   word=new StringBuffer();
               }
               word.append(c);
               lastWasCaps=true;
            } else {
               // char is lowercase
               word.append(c);
               lastWasCaps=false;
            }
        }
        words.add(word.toString());
        StringBuffer buf=new StringBuffer();
        for (int i=0; i<words.size(); i++) {
            String lcWord=((String) words.get(i)).toLowerCase();
            if (i==0) {
                String firstChar=lcWord.substring(0, 1).toUpperCase();
                char lastChar=lcWord.charAt(lcWord.length()-1);
                String middle=lcWord.substring(1, lcWord.length()-1);
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
                Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
                Thread.sleep(100);
                ms=ms+100;
                if (ms>=2000) ms=200;
            } catch (InterruptedException ie) { }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) { }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");
        
        // The worker is finished.  
        // Throw exception if caught.
        if (worker.thrownException!=null) {
            throw (RemoteException) worker.thrownException;
        }
        
        // Otherwise, get the value from the 
        // worker (returning it if applicable)
        return (java.lang.String[]) worker.get();
    }

    public java.lang.String[] getNextPID(org.apache.axis.types.NonNegativeInteger numPIDs, java.lang.String pidNamespace) throws java.rmi.RemoteException {
        String METHOD_NAME="getNextPID";
        HashMap PARMS=new HashMap();
        PARMS.put("numPIDs", numPIDs);
        PARMS.put("pidNamespace", pidNamespace);
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.getNextPID((org.apache.axis.types.NonNegativeInteger) parms.get("numPIDs"), (java.lang.String) parms.get("pidNamespace"));

                } catch (RemoteException e) {
                    thrownException=e;
                }
                return "";
            }
        };
        worker.start();
        // The following code will run in the (safe) 
        // Swing event dispatcher thread.
        int ms=0;
        Dimension d=Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words=new ArrayList();
        StringBuffer word=new StringBuffer();
        boolean lastWasCaps=true;
        for (int i=0; i<METHOD_NAME.length(); i++) {
            char c=METHOD_NAME.charAt(i);
            if (c>='A' && c<='Z') {
               // char is caps
               if (!lastWasCaps) {
                   // new word
                   words.add(word.toString());
                   word=new StringBuffer();
               }
               word.append(c);
               lastWasCaps=true;
            } else {
               // char is lowercase
               word.append(c);
               lastWasCaps=false;
            }
        }
        words.add(word.toString());
        StringBuffer buf=new StringBuffer();
        for (int i=0; i<words.size(); i++) {
            String lcWord=((String) words.get(i)).toLowerCase();
            if (i==0) {
                String firstChar=lcWord.substring(0, 1).toUpperCase();
                char lastChar=lcWord.charAt(lcWord.length()-1);
                String middle=lcWord.substring(1, lcWord.length()-1);
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
                Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
                Thread.sleep(100);
                ms=ms+100;
                if (ms>=2000) ms=200;
            } catch (InterruptedException ie) { }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) { }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");
        
        // The worker is finished.  
        // Throw exception if caught.
        if (worker.thrownException!=null) {
            throw (RemoteException) worker.thrownException;
        }
        
        // Otherwise, get the value from the 
        // worker (returning it if applicable)
        return (java.lang.String[]) worker.get();
    }

    public fedora.server.types.gen.RelationshipTuple[] getRelationships(java.lang.String pid, java.lang.String relationship) throws java.rmi.RemoteException {
        String METHOD_NAME="getRelationships";
        HashMap PARMS=new HashMap();
        PARMS.put("pid", pid);
        PARMS.put("relationship", relationship);
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.getRelationships((java.lang.String) parms.get("pid"), (java.lang.String) parms.get("relationship"));

                } catch (RemoteException e) {
                    thrownException=e;
                }
                return "";
            }
        };
        worker.start();
        // The following code will run in the (safe) 
        // Swing event dispatcher thread.
        int ms=0;
        Dimension d=Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words=new ArrayList();
        StringBuffer word=new StringBuffer();
        boolean lastWasCaps=true;
        for (int i=0; i<METHOD_NAME.length(); i++) {
            char c=METHOD_NAME.charAt(i);
            if (c>='A' && c<='Z') {
               // char is caps
               if (!lastWasCaps) {
                   // new word
                   words.add(word.toString());
                   word=new StringBuffer();
               }
               word.append(c);
               lastWasCaps=true;
            } else {
               // char is lowercase
               word.append(c);
               lastWasCaps=false;
            }
        }
        words.add(word.toString());
        StringBuffer buf=new StringBuffer();
        for (int i=0; i<words.size(); i++) {
            String lcWord=((String) words.get(i)).toLowerCase();
            if (i==0) {
                String firstChar=lcWord.substring(0, 1).toUpperCase();
                char lastChar=lcWord.charAt(lcWord.length()-1);
                String middle=lcWord.substring(1, lcWord.length()-1);
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
                Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
                Thread.sleep(100);
                ms=ms+100;
                if (ms>=2000) ms=200;
            } catch (InterruptedException ie) { }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) { }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");
        
        // The worker is finished.  
        // Throw exception if caught.
        if (worker.thrownException!=null) {
            throw (RemoteException) worker.thrownException;
        }
        
        // Otherwise, get the value from the 
        // worker (returning it if applicable)
        return (fedora.server.types.gen.RelationshipTuple[]) worker.get();
    }

    public boolean addRelationship(java.lang.String pid, java.lang.String relationship, java.lang.String object, boolean isLiteral, java.lang.String datatype) throws java.rmi.RemoteException {
        String METHOD_NAME="addRelationship";
        HashMap PARMS=new HashMap();
        PARMS.put("pid", pid);
        PARMS.put("relationship", relationship);
        PARMS.put("object", object);
        PARMS.put("isLiteral", new Boolean(isLiteral));
        PARMS.put("datatype", datatype);
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.addRelationship((java.lang.String) parms.get("pid"), (java.lang.String) parms.get("relationship"), (java.lang.String) parms.get("object"), ((Boolean) parms.get("isLiteral")).booleanValue(), (java.lang.String) parms.get("datatype"));

                } catch (RemoteException e) {
                    thrownException=e;
                }
                return "";
            }
        };
        worker.start();
        // The following code will run in the (safe) 
        // Swing event dispatcher thread.
        int ms=0;
        Dimension d=Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words=new ArrayList();
        StringBuffer word=new StringBuffer();
        boolean lastWasCaps=true;
        for (int i=0; i<METHOD_NAME.length(); i++) {
            char c=METHOD_NAME.charAt(i);
            if (c>='A' && c<='Z') {
               // char is caps
               if (!lastWasCaps) {
                   // new word
                   words.add(word.toString());
                   word=new StringBuffer();
               }
               word.append(c);
               lastWasCaps=true;
            } else {
               // char is lowercase
               word.append(c);
               lastWasCaps=false;
            }
        }
        words.add(word.toString());
        StringBuffer buf=new StringBuffer();
        for (int i=0; i<words.size(); i++) {
            String lcWord=((String) words.get(i)).toLowerCase();
            if (i==0) {
                String firstChar=lcWord.substring(0, 1).toUpperCase();
                char lastChar=lcWord.charAt(lcWord.length()-1);
                String middle=lcWord.substring(1, lcWord.length()-1);
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
                Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
                Thread.sleep(100);
                ms=ms+100;
                if (ms>=2000) ms=200;
            } catch (InterruptedException ie) { }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) { }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");
        
        // The worker is finished.  
        // Throw exception if caught.
        if (worker.thrownException!=null) {
            throw (RemoteException) worker.thrownException;
        }
        
        // Otherwise, get the value from the 
        // worker (returning it if applicable)
        return (Boolean)worker.get();// (Object) worker.get();
    }

    public boolean purgeRelationship(java.lang.String pid, java.lang.String relationship, java.lang.String object, boolean isLiteral, java.lang.String datatype) throws java.rmi.RemoteException {
        String METHOD_NAME="purgeRelationship";
        HashMap PARMS=new HashMap();
        PARMS.put("pid", pid);
        PARMS.put("relationship", relationship);
        PARMS.put("object", object);
        PARMS.put("isLiteral", new Boolean(isLiteral));
        PARMS.put("datatype", datatype);
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.purgeRelationship((java.lang.String) parms.get("pid"), (java.lang.String) parms.get("relationship"), (java.lang.String) parms.get("object"), ((Boolean) parms.get("isLiteral")).booleanValue(), (java.lang.String) parms.get("datatype"));

                } catch (RemoteException e) {
                    thrownException=e;
                }
                return "";
            }
        };
        worker.start();
        // The following code will run in the (safe) 
        // Swing event dispatcher thread.
        int ms=0;
        Dimension d=Administrator.PROGRESS.getSize();
        // Devise verbage based on method name
        ArrayList words=new ArrayList();
        StringBuffer word=new StringBuffer();
        boolean lastWasCaps=true;
        for (int i=0; i<METHOD_NAME.length(); i++) {
            char c=METHOD_NAME.charAt(i);
            if (c>='A' && c<='Z') {
               // char is caps
               if (!lastWasCaps) {
                   // new word
                   words.add(word.toString());
                   word=new StringBuffer();
               }
               word.append(c);
               lastWasCaps=true;
            } else {
               // char is lowercase
               word.append(c);
               lastWasCaps=false;
            }
        }
        words.add(word.toString());
        StringBuffer buf=new StringBuffer();
        for (int i=0; i<words.size(); i++) {
            String lcWord=((String) words.get(i)).toLowerCase();
            if (i==0) {
                String firstChar=lcWord.substring(0, 1).toUpperCase();
                char lastChar=lcWord.charAt(lcWord.length()-1);
                String middle=lcWord.substring(1, lcWord.length()-1);
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
                Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
                Thread.sleep(100);
                ms=ms+100;
                if (ms>=2000) ms=200;
            } catch (InterruptedException ie) { }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) { }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");
        
        // The worker is finished.  
        // Throw exception if caught.
        if (worker.thrownException!=null) {
            throw (RemoteException) worker.thrownException;
        }
        
        // Otherwise, get the value from the 
        // worker (returning it if applicable)
        return (Boolean)worker.get();// (Object) worker.get();
    }
}
