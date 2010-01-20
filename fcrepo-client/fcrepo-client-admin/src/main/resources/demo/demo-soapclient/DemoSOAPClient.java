/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package demo.soapclient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.StringTokenizer;
import java.util.HashMap;

import javax.xml.rpc.ServiceException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import org.fcrepo.client.FedoraClient;

import org.fcrepo.common.Constants;

import org.fcrepo.server.management.FedoraAPIM;
import org.fcrepo.server.access.FedoraAPIA;
import org.fcrepo.server.types.gen.DatastreamDef;
import org.fcrepo.server.types.gen.MethodParmDef;
import org.fcrepo.server.types.gen.MIMETypedStream;
import org.fcrepo.server.types.gen.ObjectMethodsDef;
import org.fcrepo.server.types.gen.ObjectProfile;
import org.fcrepo.server.types.gen.RepositoryInfo;
import org.fcrepo.server.types.gen.Property;

/**
 * A simple example of a SOAP client that makes calls to the Fedora SOAP 
 * interfaces (API-A and API-M).
 *
 * NOTE: 
 * This class is outdated and uses demo objects that no longer exist.
 * Future releases will not include this class.
 * 
 * @deprecated as of release 3.3
 * @author Sandy Payette
 */
public class DemoSOAPClient
        implements Constants {

    private static FedoraAPIM APIM;
    private static FedoraAPIA APIA;
    private static HashMap s_repoInfo=new HashMap();

    public DemoSOAPClient(String protocol, String host, int port, String user, String pass, String context)
            throws Exception {
                
        // Use the FedoraClient utility to get SOAP stubs.
        // These SOAP stubs enable the client to connect to a Fedora repository
        // via the API-A and API-M web service interfaces.

        String baseURL = protocol + "://" + host + ":" + port + "/" + context;
        FedoraClient fc = new FedoraClient(baseURL, user, pass);
        APIA=fc.getAPIA();
        APIM=fc.getAPIM();
    }
    

    
    public RepositoryInfo describeRepository() 
        throws RemoteException {
            
        // make the SOAP call on API-A using the connection stub
        RepositoryInfo repoinfo = APIA.describeRepository();
        
        // print results
        System.out.println("SOAP Request: describeRepository...");
        System.out.println("SOAP Response: repository version = " + repoinfo.getRepositoryVersion());
        System.out.println("SOAP Response: repository name = " + repoinfo.getRepositoryName());
        System.out.println("SOAP Response: repository pid namespace = " + repoinfo.getRepositoryPIDNamespace());    
        System.out.println("SOAP Response: repository default export = " + repoinfo.getDefaultExportFormat());
        System.out.println("SOAP Response: repository base URL = " + repoinfo.getRepositoryBaseURL());    
        System.out.println("SOAP Response: repository OAI namespace = " + repoinfo.getOAINamespace());
        System.out.println("SOAP Response: repository sample OAI identifier = " + repoinfo.getSampleOAIIdentifier());
        System.out.println("SOAP Response: repository sample OAI URL = " + repoinfo.getSampleOAIURL());
        System.out.println("SOAP Response: repository sample access URL = " + repoinfo.getSampleAccessURL());
        System.out.println("SOAP Response: repository sample search URL = " + repoinfo.getSampleSearchURL());        
        System.out.println("SOAP Response: repository sample PID = " + repoinfo.getSamplePID());
        return repoinfo;                        
    }
    
    public String ingest(InputStream ingestStream, String ingestFormat, String logMessage)
        throws RemoteException, IOException {
        
        // prep         
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        pipeStream(ingestStream, out, 4096);
        
        // make the SOAP call on API-M using the connection stub
        String pid = APIM.ingest(out.toByteArray(), ingestFormat, logMessage);
        
        System.out.println("SOAP Request: ingest...");
        System.out.println("SOAP Response: pid = " + pid);
        return pid;
    }
    public String addDatastream(String pid, String dsID, String[] altIDs, String dsLabel, 
        boolean versionable, String dsMIME, String formatURI, 
        String dsLocation, String dsControlGroup, String dsState, 
        String checksumType, String checksum, String logMessage)
        throws RemoteException {

            // make the SOAP call on API-M using the connection stub            
            String datastreamID = APIM.addDatastream(
                pid, dsID, altIDs, dsLabel, versionable, dsMIME, formatURI,
                dsLocation, dsControlGroup, dsState, checksumType, checksum, logMessage);
                
            System.out.println("SOAP Request: addDatastream...");
            System.out.println("SOAP Response: datastreamID = " + datastreamID);                
            return datastreamID;
        }
        
    public String modifyDatastreamByReference(String pid, String dsID, String[] altIDs, String dsLabel, 
        String dsMIME, String formatURI, String dsLocation,
        String checksumType, String checksum,
        String logMessage, boolean force)
        throws RemoteException {

            // make the SOAP call on API-M using the connection stub
            String datastreamID = APIM.modifyDatastreamByReference(
                pid, dsID, altIDs, dsLabel, dsMIME,
                formatURI, dsLocation, checksumType, checksum, logMessage, force);            
                
            System.out.println("SOAP Request: modifyDatastreamByReference...");
            System.out.println("SOAP Response: datastreamID = " + datastreamID);                
            return datastreamID;
        }
        
    public String[] purgeDatastream(String pid, String dsID, String startDate, String endDate, 
        String logMessage, boolean force)
        throws RemoteException {

            // make the SOAP call on API-M using the connection stub
            String[] dateTimeStamps = APIM.purgeDatastream(
                pid, dsID, startDate, endDate, logMessage, force);            
                
            System.out.println("SOAP Request: purgeDatastream...");                
            return dateTimeStamps;
        }
        
    public String purgeObject(String pid, String logMessage, boolean force)
        throws RemoteException {

            // make the SOAP call on API-M using the connection stub
            String purgeDateTime = APIM.purgeObject(pid, logMessage, force);            
                
            System.out.println("SOAP Request: purgeObject...");
            System.out.println("SOAP Response: purge dateTime = " + purgeDateTime);                
            return purgeDateTime;
        }
        
    public byte[] export(String pid, String format, String exportContext, OutputStream outStream) 
        throws RemoteException, IOException {
                    
        // make the SOAP call on API-M
        byte[] objectXML = APIM.export(pid, format, exportContext);
        
        // serialize the object XML to the specified output stream
        try {                        
            // use Xerces to pretty print the xml, assuming it's well formed
            DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder=factory.newDocumentBuilder();
            Document doc=builder.parse(new ByteArrayInputStream(objectXML));
            OutputFormat fmt=new OutputFormat("XML", "UTF-8", true);
            fmt.setIndent(2);
            fmt.setLineWidth(120);
            fmt.setPreserveSpace(false);
            XMLSerializer ser=new XMLSerializer(outStream, fmt);
            ser.serialize(doc);
        } catch (Exception e) {
            System.out.println("Error on export while serializing object XML." + 
                e.getClass().getName() + " : " + e.getMessage());
        } finally {
          outStream.close();
        }
        
        // print results
        System.out.println("SOAP Request: export...");
        System.out.println("SOAP Response: see result serialized in XML export file.");    
        return objectXML;                        
    }

    public byte[] getObjectXML(String pid) 
        throws RemoteException {
            
        // make the SOAP call on API-M    
        byte[] objectXML = APIM.getObjectXML(pid);
        
        // print results

        return objectXML;                        
    }
    
    /**
     * Copies the contents of an InputStream to an OutputStream, then closes
     * both.  
     *
     * @param in The source stream.
     * @param out The target stram.
     * @param bufSize Number of bytes to attempt to copy at a time.
     * @throws IOException If any sort of read/write error occurs on either
     *         stream.
     */
    public static void pipeStream(InputStream in, OutputStream out, int bufSize)
            throws IOException {
        try {
            byte[] buf = new byte[bufSize];
            int len;
            while ( ( len = in.read( buf ) ) > 0 ) {
                out.write( buf, 0, len );
            }
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException e) {
                System.err.println("WARNING: Could not close stream.");
            }
        }
    }
    
    public static void main(String[] args) {
        try {
            if (args.length==5 || args.length==6) {              
                  if (!args[0].equals("http") && !args[0].equals("https")) {
                      throw new Exception("Protocol must be either \"http\" or \"https\". Value specified was: \""+args[0]+"\".");
                  }
                  System.out.println("\n");
                  System.out.println("Protocol: " + args[0]);
                  System.out.println("Host: " + args[1]);
                  System.out.println("Port: " + args[2]);
                  System.out.println("Username: " + args[3]);
                  System.out.println("Password: " + args[4]);
                  String context = Constants.FEDORA_DEFAULT_APP_CONTEXT;

                  if (args.length == 6){
                      System.out.println("Context:  " + args[5] + "\n");
                      context = args[5];
                  }
                  
                  // Instantiate the demo client.
                  // This will set up connection stubs for making SOAP requests on API-A and API-M              
              
                DemoSOAPClient caller = new DemoSOAPClient(args[0], 
                                args[1], new Integer(args[2]).intValue(),
                                args[3], args[4], context);

                //**************************************************************                                
                //******** STEP 1 : get info about the repository
                //**************************************************************    
                System.out.println("\nTest describeRepository..........................................");
                RepositoryInfo repoinfo = caller.describeRepository();
                
                //**************************************************************                        
                // ******** STEP 2  purge test objects if they already exist
                //**************************************************************    
                String purgeDate=null;
                try {
                    purgeDate = caller.purgeObject(
                        "test:100", // the object pid
                        "purge object", // an optional log message about the change
                         false);  // do not force changes that break ref integrity
                } catch (Exception e) {
                        System.out.println("Hack...just ignore failures since objects may not exist yet." + e.getMessage());
                }
                try {                     
                    purgeDate = caller.purgeObject(
                        "test:28", // the object pid
                        "purge object", // an optional log message about the change
                         false);  // do not force changes that break ref integrity
                } catch (Exception e) {
                        System.out.println("Hack...just ignore failures since objects may not exist yet." + e.getMessage());
                }
                try {                         
                    purgeDate = caller.purgeObject(
                        "test:27", // the object pid
                        "purge object", // an optional log message about the change
                         false);  // do not force changes that break ref integrity
                } catch (Exception e) {
                        System.out.println("Hack...just ignore failures since objects may not exist yet." + e.getMessage());
                }


                //**************************************************************                    
                //******** STEP 3: ingest the test objects
                //**************************************************************    
                FileInputStream inStream=null;
                String ingestPID=null;
                
                System.out.println("\nTest ingest......................................................");
                File ingestFile=new File("TestIngestFiles/sdef_test_27.xml");        
                try {
                    inStream=new FileInputStream(ingestFile);
                } catch (IOException ioe) {
                        System.out.println("Error on ingest file inputstream: " + ioe.getMessage());
                        ioe.printStackTrace();
                }
                ingestPID = caller.ingest(inStream, FOXML1_1.uri, "ingest of test sdef");
                System.out.println("Finished test ingest of sdef object: " + ingestPID);
                
                System.out.println("\nTest ingest......................................................");
                ingestFile=new File("TestIngestFiles/sdep_test_28.xml");        
                inStream=null;
                try {
                    inStream=new FileInputStream(ingestFile);
                } catch (IOException ioe) {
                        System.out.println("Error on ingest file inputstream: " + ioe.getMessage());
                        ioe.printStackTrace();
                }
                ingestPID = caller.ingest(inStream, FOXML1_1.uri, "ingest of test deployment");
                System.out.println("Finished test ingest of deployment object: " + ingestPID);
                
                System.out.println("\nTest ingest......................................................");
                ingestFile=new File("TestIngestFiles/obj_test_100.xml");        
                inStream=null;
                try {
                    inStream=new FileInputStream(ingestFile);
                } catch (IOException ioe) {
                        System.out.println("Error on ingest file inputstream: " + ioe.getMessage());
                        ioe.printStackTrace();
                }
                ingestPID = caller.ingest(inStream, FOXML1_1.uri, "ingest of test object");
                System.out.println("Finished test ingest of data object: " + ingestPID);
                
                System.out.println("\nTest ingest......................................................");
                ingestFile=new File("TestIngestFiles/test_UVA_STD_IMAGE.xml");        
                inStream=null;
                try {
                    inStream=new FileInputStream(ingestFile);
                } catch (IOException ioe) {
                        System.out.println("Error on ingest file inputstream: " + ioe.getMessage());
                        ioe.printStackTrace();
                }
                ingestPID = caller.ingest(inStream, FOXML1_1.uri, "ingest of test object");
                System.out.println("Finished test ingest of cmodel object: " + ingestPID);                

                //**************************************************************                    
                //******** STEP 4: add a datastream to the object
                //**************************************************************    
                System.out.println("\nTest add datastream..............................................");
                String[] altIDs = new String[] {"id1", "id2", "id3"};
                String datastreamID = caller.addDatastream(
                    ingestPID, // the object pid
                    "MY-DS",   // user-assigned datastream name or id
                    altIDs,
                    "Add my test datastream",  // user-assigned label
                    true, // in version 2.0 always set datastream versioning to true
                    "image/gif", // mime type of the datastream content
                    "info:fedora/format/myformat", // an optional format URI
                    "http://www.cs.cornell.edu/payette/images/sjcomp.gif", // URL for content
                    "E",  // type E for External Referenced Datastream
                    "A",  // datastream state is A for Active
                    null,  // datastream checksumType
                    null, // datastream checksum
                    "added new datastream MY-DS");  // log message
                    
                    
                //**************************************************************                
                //******** STEP 5: modify a datastream 
                //**************************************************************                        
                // modify the datastream using null to indicate which attributes should stay the same.
                System.out.println("\nFirst test of modify datastream .................................");
                String modDSID = caller.modifyDatastreamByReference(
                    ingestPID, // the object pid
                    "MY-DS",   // user-assigned datastream name or id
                    null, // altIDs (no change)
                    "modify-1 of my test datastream",  // new user-assigned label
                    null, // MIME type (no change)
                    null, // new formatURI (no change)
                    null, // new URL for content (no change)
                    null, // new checksumType
                    null, // new checksum
                    "first modify to change label only", // an optional log message about the change
                     false);  // do not force changes that break ref integrity

                //**************************************************************                
                //******** STEP 6: modify a datastream again
                //**************************************************************        
                // again, modify the datastream and test setting attributes to empty strings.
                // NOTE:  attempt to set system required attribute to empty will default to no change.
                System.out.println("\nSecond test of modify datastream.................................");
                modDSID = caller.modifyDatastreamByReference(
                    ingestPID, // the object pid
                    "MY-DS",   // user-assigned datastream name or id
                    new String[0], // altIDs (empty array)
                    "",  // new user-assigned label
                    "", // MIME type (empty)
                    "", // new formatURI (empty)
                    "", // new URL for content (no change since required field cannot be emptied)
                    null, // new checksumType
                    null, // new checksum
                    "second modify to empty all non-required fields", // an optional log message about the change
                     false);  // do not force changes that break ref integrity
                
                //**************************************************************                
                //******** STEP 7: purge a datastream 
                //**************************************************************    
                System.out.println("\nTest of purge datastream.........................................");
                String[] dateTimeStamps = caller.purgeDatastream(
                    ingestPID, // the object pid
                    "MY-DS",   // user-assigned datastream name or id
                    "",
                    "",  // end date to purge versions before (null/empty to purge all versions)
                    "purge datastream", // an optional log message about the change
                     false);  // do not force changes that break ref integrity
                
                //**************************************************************                
                //******** STEP 8: export the demo object
                //**************************************************************    
                System.out.println("\nTest of export object............................................");
                File exportFile = new File("demo-export.xml");
                FileOutputStream outStream = null;
                try {
                    outStream = new FileOutputStream(exportFile);
                } catch (IOException ioe) {
                        System.out.println("Error on export output stream: " + ioe.getMessage());
                        ioe.printStackTrace();
                }        
                byte[] objectXML = caller.export(ingestPID, FOXML1_1.uri, null, outStream);
                
                //**************************************************************
                //******** NOW TEST API-A METHODS 
                //**************************************************************
                
                //**************************************************************                
                //******** STEP 9: listDatastreams for demo object demo:11
                //**************************************************************
                System.out.println("\nTest of listDatastream...........................................");
                listDatastreams();
                
                //**************************************************************                
                //******** STEP 10: listMethods for demo object demo:11
                //**************************************************************
                System.out.println("\nTest of listMethods..............................................");
                listMethods();
                
                //**************************************************************                
                //******** STEP 11: get the object profile for demo object demo:11
                //**************************************************************
                System.out.println("\nTest of getObjectProfile.........................................");
                getObjectProfile();
                
                //**************************************************************                
                //******** STEP 12: get several datastreams from various demo objects
                //**************************************************************
                System.out.println("\nTest of getDatastreamDissemination...............................");
                getDatastreamDissemination();
                
                //**************************************************************                
                //******** STEP 13: get several disseminations from various demo objects
                //**************************************************************
                System.out.println("\nTest of getDissemination.........................................");
                getDissemination();
                
                //**************************************************************                
                //******** STEP 14: get object history the demo object demo:11
                //**************************************************************
                System.out.println("\nTest of getObjectHistory.........................................");
                getObjectHistory();
                              
              } else {
                  System.out.println("Number of arguments must be equal to 5.");
                  System.out.println("Usage: run-demo-soapclient protocol host port username password");
                  System.out.println("Demo soapclient requires that demo objects are already ingested in repository \n");
              }
        } catch (Exception e) {
            System.out.println("Exception in main: " +  e.getMessage());
            e.printStackTrace();
        }          
    }
    
    public static void listDatastreams() throws Exception {
        
        DatastreamDef[] dsDefs = APIA.listDatastreams("demo:11", null);
        System.out.println("SOAP Request: listDatastreams...");
        System.out.println("SOAP Response: see results below.");            
        verifyDatastreamDefs(dsDefs, "SOAP Response: listDatastream: ");
    }    
    
    public static void listMethods() throws Exception {
        
        ObjectMethodsDef[] methodDefs = APIA.listMethods("demo:11", null);
        System.out.println("SOAP Request: listMethods...");
        System.out.println("SOAP Response: see results below.");
        verifyObjectMethods(methodDefs, "SOAP Response: listMethods: ");
    }    
    
    public static void getDatastreamDissemination() throws Exception {
        
        // test for DC datastream
        MIMETypedStream ds = null;
        ds = APIA.getDatastreamDissemination("demo:11", "DC", null);
        System.out.println("SOAP Request: getDatastreamDissemination for DC datastream of demo object demo:11...");        
        String dsXML = new String(ds.getStream(), "UTF-8");
        System.out.println("SOAP Response: GetDatastreamDissemination Object:demo:11 Datastream:DC succeeded.");
        System.out.println("SOAP Response: DC datastream contents: \n"+dsXML);
        
        // test for type X datastream         
        ds = APIA.getDatastreamDissemination("demo:11", "TECH1", null);
        System.out.println("\nSOAP Request: getDatastreamDissemination for TECH1 datastream of demo object demo:11...");        
        dsXML = new String(ds.getStream(), "UTF-8");
        System.out.println("SOAP Response: GetDatastreamDissemination Object:demo:11 Datastream:TECH1 succeeded.");
        System.out.println("SOAP Response: TECH1 datastream contents: \n"+dsXML);        

        // test for type E datastream             
        ds = APIA.getDatastreamDissemination("demo:11", "MRSID", null);    
        System.out.println("\nSOAP Request: getDatastreamDissemination for MRSID datastream of demo object demo:11...");            
        System.out.println("SOAP Response: GetDatastreamDissemination Object:demo:11 Datastream:MRSID succeeded.");
        System.out.println("SOAP Response: MRSID datastream contents: BINARY DATA "+ds);
        
        // test for type R datastream             
        ds = APIA.getDatastreamDissemination("demo:30", "THUMBRES_IMG", null);
        System.out.println("\nSOAP Request: getDatastreamDissemination for THUMBRES_IMG datastream of demo object demo:30...");            
        System.out.println("SOAP Response: GetDatastreamDissemination Object:demo:30 Datastream:THUMBRES_IMG succeeded.");
        System.out.println("SOAP Response: THUMBRES_IMG datastream contents: BINARY DATA "+ds);
        
        // test for type M datastream             
        ds = APIA.getDatastreamDissemination("demo:5", "THUMBRES_IMG", null);
        System.out.println("\nSOAP Request: getDatastreamDissemination for THUMBRES_IMG datastream of demo object demo:5...");            
        System.out.println("SOAP Response: GetDatastreamDissemination Object:demo:5 Datastream:THUMBRES_IMG succeeded.");
        System.out.println("SOAP Response: THUMBRES_IMG datastream contents: BINARY DATA "+ds);
        
    }    
    

    public static void getObjectProfile() throws Exception {
        ObjectProfile profile = APIA.getObjectProfile("demo:11", null);
        System.out.println("SOAP Request: getObjectProfile for demo object demo:11...");            
        System.out.println("SOAP Response: PID: "+profile.getPid());
        System.out.println("SOAP Response: ObjectLabel: "+profile.getObjLabel());
        System.out.println("SOAP Response: CreateDate: "+profile.getObjCreateDate());
        System.out.println("SOAP Response: LastModDate: "+profile.getObjLastModDate());
        System.out.println("SOAP Response: DissIndexViewURL: "+profile.getObjDissIndexViewURL());
        System.out.println("SOAP Response: ItemIndexViewURL: "+profile.getObjItemIndexViewURL());

    }

    public static void getObjectHistory() throws Exception {
        String[] timestamps = APIA.getObjectHistory("demo:11");
        System.out.println("SOAP Request: getObjectHistory for demo object demo:11...");
        for (int i=0; i<timestamps.length; i++) {
            System.out.println("SOAP Response: object:demo:11 changeDate["+i+"]: "+timestamps[i]);
        }
    }

    public static void getDissemination() throws Exception {
    
    // test dissemination of the Default Disseminator
    MIMETypedStream diss = null;
    diss = APIA.getDissemination("demo:5", "fedora-system:3", "viewDublinCore", new Property[0], null);
    System.out.println("SOAP Request: getDissemination for method viewDublinCore of demo object demo:11...");    
    String dsXML = new String(diss.getStream(), "UTF-8");
    System.out.println("SOAP Response: GetDissemination Object:demo:11 Method:viewDublinCore succeeded.");
    System.out.println("SOAP Response: Dissemination results: \n"+dsXML);    
    
    // test dissemination of getThumb method with no parameters
    diss = APIA.getDissemination("demo:5", "demo:1", "getThumbnail", new Property[0], null);
    System.out.println("\nSOAP Request: getDissemination for method getThumbnail of demo object demo:5...");            
    System.out.println("SOAP Response: GetDissemination Object:demo:5 Method:getThumbnail succeeded.");
    System.out.println("SOAP Response: Dissemination results: BINARY DATA "+diss);    
    
    // test dissemination using resizeImage method with parameters        
    Property[] parms = new Property[4];
    Property p = new Property();
    p.setName("width");
    p.setValue("100");
    parms[0] = p;
    Property p2 = new Property();
    p2.setName("height");
    p2.setValue("100");
    parms[1] = p2;
    Property p3 = new Property();
    p3.setName("x");
    p3.setValue("100");
    parms[2] = p3;
    Property p4 = new Property();
    p4.setName("y");
    p4.setValue("100");
    parms[3] = p4;    
    diss = APIA.getDissemination("demo:29", "demo:27", "cropImage", parms, null);
    System.out.println("\nSOAP Request: getDissemination for method cropImage of demo object demo:29...");        
    System.out.println("SOAP Response: GetDissemination Object:demo:29 Method:cropImage succeeded.");
    System.out.println("SOAP Response: Dissemination results: BINARY DATA "+diss);

    }    
    
    public static void verifyDatastreamDefs(DatastreamDef[] dsDefArray, String msg) throws Exception {
        
        String dsID = null;
        String label = null;
        String mimeType = null;
        DatastreamDef dsDef = null;
            
        for (int i=0; i<dsDefArray.length; i++) {
            dsDef = dsDefArray[i];
            dsID = dsDef.getID();
            label = dsDef.getLabel();
            mimeType = dsDef.getMIMEType();
            System.out.println(msg + " datastreamDef["+i+"] "
                 + "dsID: "+dsID);
            System.out.println(msg + " datastreamDef["+i+"] "
                 + "label: '"+label+"'");
            System.out.println(msg + " datastreamDef["+i+"] "
                 + "mimeType: "+mimeType);
        }
            
    }

    public static void verifyObjectMethods(ObjectMethodsDef[] methodDefsArray, String msg) throws Exception {
    
        String sDefPID = null;
        String methodName = null;
        MethodParmDef[] parms = null;
        ObjectMethodsDef methodDef = null;
            
        for (int i=0; i<methodDefsArray.length; i++) {
            methodDef = methodDefsArray[i];
            sDefPID = methodDef.getServiceDefinitionPID();
            methodName = methodDef.getMethodName();
            parms = methodDef.getMethodParmDefs();
            System.out.println(msg + " methodDef["+i+"] "
                 + "sDefPID: "+sDefPID);
            System.out.println(msg + " methodDef["+i+"] "
                 + "methodName: '"+methodName+"'");
            for (int j=0; j<parms.length; j++) {
                MethodParmDef p = parms[j];    
                System.out.println(msg + " methodDef["+i+"] "
                     + "parmName["+j+"] "+p.getParmName());
            }
        }
            
    }
    
}
