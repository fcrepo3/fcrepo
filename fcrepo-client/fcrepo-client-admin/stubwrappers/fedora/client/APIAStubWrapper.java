package fedora.client;

import java.util.HashMap; // needed by generated code

import java.awt.Dimension;
import java.rmi.RemoteException;
import java.util.ArrayList;

public class APIAStubWrapper
        implements fedora.server.access.FedoraAPIA {

    /** The wrapped instance */
    private fedora.server.access.FedoraAPIA m_instance;

    public APIAStubWrapper(fedora.server.access.FedoraAPIA instance) {
        m_instance=instance;
    }

    public fedora.server.types.gen.RepositoryInfo describeRepository() throws java.rmi.RemoteException {
        String METHOD_NAME="describeRepository";
        HashMap PARMS=new HashMap();
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.describeRepository();

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
        return (fedora.server.types.gen.RepositoryInfo) worker.get();
    }

    public fedora.server.types.gen.ObjectProfile getObjectProfile(java.lang.String pid, java.lang.String asOfDateTime) throws java.rmi.RemoteException {
        String METHOD_NAME="getObjectProfile";
        HashMap PARMS=new HashMap();
        PARMS.put("pid", pid);
        PARMS.put("asOfDateTime", asOfDateTime);
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.getObjectProfile((java.lang.String) parms.get("pid"), (java.lang.String) parms.get("asOfDateTime"));

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
        return (fedora.server.types.gen.ObjectProfile) worker.get();
    }

    public fedora.server.types.gen.ObjectMethodsDef[] listMethods(java.lang.String pid, java.lang.String asOfDateTime) throws java.rmi.RemoteException {
        String METHOD_NAME="listMethods";
        HashMap PARMS=new HashMap();
        PARMS.put("pid", pid);
        PARMS.put("asOfDateTime", asOfDateTime);
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.listMethods((java.lang.String) parms.get("pid"), (java.lang.String) parms.get("asOfDateTime"));

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
        return (fedora.server.types.gen.ObjectMethodsDef[]) worker.get();
    }

    public fedora.server.types.gen.DatastreamDef[] listDatastreams(java.lang.String pid, java.lang.String asOfDateTime) throws java.rmi.RemoteException {
        String METHOD_NAME="listDatastreams";
        HashMap PARMS=new HashMap();
        PARMS.put("pid", pid);
        PARMS.put("asOfDateTime", asOfDateTime);
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.listDatastreams((java.lang.String) parms.get("pid"), (java.lang.String) parms.get("asOfDateTime"));

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
        return (fedora.server.types.gen.DatastreamDef[]) worker.get();
    }

    public fedora.server.types.gen.MIMETypedStream getDatastreamDissemination(java.lang.String pid, java.lang.String dsID, java.lang.String asOfDateTime) throws java.rmi.RemoteException {
        String METHOD_NAME="getDatastreamDissemination";
        HashMap PARMS=new HashMap();
        PARMS.put("pid", pid);
        PARMS.put("dsID", dsID);
        PARMS.put("asOfDateTime", asOfDateTime);
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.getDatastreamDissemination((java.lang.String) parms.get("pid"), (java.lang.String) parms.get("dsID"), (java.lang.String) parms.get("asOfDateTime"));

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
        return (fedora.server.types.gen.MIMETypedStream) worker.get();
    }

    public fedora.server.types.gen.MIMETypedStream getDissemination(java.lang.String pid, java.lang.String serviceDefinitionPid, java.lang.String methodName, fedora.server.types.gen.Property[] parameters, java.lang.String asOfDateTime) throws java.rmi.RemoteException {
        String METHOD_NAME="getDissemination";
        HashMap PARMS=new HashMap();
        PARMS.put("pid", pid);
        PARMS.put("serviceDefinitionPid", serviceDefinitionPid);
        PARMS.put("methodName", methodName);
        PARMS.put("parameters", parameters);
        PARMS.put("asOfDateTime", asOfDateTime);
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.getDissemination((java.lang.String) parms.get("pid"), (java.lang.String) parms.get("serviceDefinitionPid"), (java.lang.String) parms.get("methodName"), (fedora.server.types.gen.Property[]) parms.get("parameters"), (java.lang.String) parms.get("asOfDateTime"));

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
        return (fedora.server.types.gen.MIMETypedStream) worker.get();
    }

    public fedora.server.types.gen.FieldSearchResult findObjects(java.lang.String[] resultFields, org.apache.axis.types.NonNegativeInteger maxResults, fedora.server.types.gen.FieldSearchQuery query) throws java.rmi.RemoteException {
        String METHOD_NAME="findObjects";
        HashMap PARMS=new HashMap();
        PARMS.put("resultFields", resultFields);
        PARMS.put("maxResults", maxResults);
        PARMS.put("query", query);
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.findObjects((java.lang.String[]) parms.get("resultFields"), (org.apache.axis.types.NonNegativeInteger) parms.get("maxResults"), (fedora.server.types.gen.FieldSearchQuery) parms.get("query"));

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
        return (fedora.server.types.gen.FieldSearchResult) worker.get();
    }

    public fedora.server.types.gen.FieldSearchResult resumeFindObjects(java.lang.String sessionToken) throws java.rmi.RemoteException {
        String METHOD_NAME="resumeFindObjects";
        HashMap PARMS=new HashMap();
        PARMS.put("sessionToken", sessionToken);
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.resumeFindObjects((java.lang.String) parms.get("sessionToken"));

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
        return (fedora.server.types.gen.FieldSearchResult) worker.get();
    }

    public java.lang.String[] getObjectHistory(java.lang.String pid) throws java.rmi.RemoteException {
        String METHOD_NAME="getObjectHistory";
        HashMap PARMS=new HashMap();
        PARMS.put("pid", pid);
        // Run the method in a SwingWorker thread
        SwingWorker worker=new SwingWorker(PARMS) {
            public Object construct() {
                try {

// call wrapped method
return m_instance.getObjectHistory((java.lang.String) parms.get("pid"));

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
}
