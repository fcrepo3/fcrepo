/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.service;

import java.io.File;
import java.io.FileInputStream;

import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import fedora.server.errors.GeneralException;
import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.RepositoryConfigurationException;
import fedora.server.storage.types.DeploymentDSBindSpec;
import fedora.server.storage.types.MethodDef;
import fedora.server.storage.types.MethodDefOperationBind;
import fedora.server.storage.types.MethodParmDef;

/**
 * Controller class for parsing the various kinds of inline metadata datastreams
 * found in service objects. The intent of this class is to initiate parsing of
 * these datastreams so that information about a service can be
 * instantiated in Fedora.
 * </p>
 * 
 * @author Sandy Payette
 * @version $Id$
 */
public class ServiceMapper {

    private WSDLParser wsdlHandler;

    private MmapParser methodMapHandler;

    private DSInputSpecParser dsInputSpecHandler;

    private final String parentPID;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err
                    .println("usage: java ServiceMapper wsdlLocation methodMapLocation "
                            + "\n"
                            + "  wsdlLocation: the file path of the wsdl to be parsed"
                            + "\n"
                            + "  methodMapLocation: the file path of the method map to be parsed."
                            + "\n"
                            + "  dsInputSpecLocation: the file path of the datastream input spec to be parsed."
                            + "  pid: the PID of the sDef or sDep object for the above files.");
            System.exit(1);
        }
        try {
            ServiceMapper mapper = new ServiceMapper(args[3]);
            InputSource wsdl =
                    new InputSource(new FileInputStream(new File(args[0])));
            InputSource mmap =
                    new InputSource(new FileInputStream(new File(args[1])));
            InputSource dsSpec =
                    new InputSource(new FileInputStream(new File(args[2])));
            mapper.getMethodDefs(mmap);
            mapper.getMethodDefBindings(wsdl, mmap);
            mapper.getDSInputSpec(dsSpec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(1);

    }

    public ServiceMapper(String behaviorObjectPID) {
        parentPID = behaviorObjectPID;
    }

    /**
     * getMethodDefs: creates an array of abstract method definitions in the
     * form of an array of Fedora MethodDef objects. The creation of a MethodDef
     * object requires information from a Fedora Method Map.
     * 
     * @param methodMapSource :
     *        Fedora Method Map definition for methods
     * @return MethodDef[] : an array of abstract method definitions
     * @throws ObjectIntegrityException
     * @throws RepositoryConfigurationException
     * @throws GeneralException
     */
    public MethodDef[] getMethodDefs(InputSource methodMapSource)
            throws ObjectIntegrityException, RepositoryConfigurationException,
            GeneralException {
        Mmap methodMap = getMethodMap(methodMapSource);
        return methodMap.mmapMethods;
    }

    /**
     * getMethodDefBindings: creates an array of operation bindings in the form
     * of an array of Fedora MethodDefOperationBind objects. The creation of a
     * MethodDefOperationBind object requires information from a WSDL service
     * definition and a related Fedora Method Map. The Fedora Method Map is
     * merged with the WSDL to provide a Fedora-specific view of the WSDL.
     * 
     * @param wsdlSource :
     *        WSDL service definition for methods
     * @param methodMapSource :
     *        Fedora Method Map definition for methods
     * @return MethodDefOperationBind[] : an array of method bindings
     * @throws ObjectIntegrityException
     * @throws RepositoryConfigurationException
     * @throws GeneralException
     */
    public MethodDefOperationBind[] getMethodDefBindings(InputSource wsdlSource,
                                                         InputSource methodMapSource)
            throws ObjectIntegrityException, RepositoryConfigurationException,
            GeneralException {
        return merge(getService(wsdlSource), getMethodMap(methodMapSource));
    }

    public DeploymentDSBindSpec getDSInputSpec(InputSource dsInputSpecSource)
            throws ObjectIntegrityException, RepositoryConfigurationException,
            GeneralException {
        if (dsInputSpecHandler == null) {
            dsInputSpecHandler =
                    (DSInputSpecParser) parse(dsInputSpecSource,
                                              new DSInputSpecParser(parentPID));
        }
        return dsInputSpecHandler.getServiceDSInputSpec();
    }

    private Mmap getMethodMap(InputSource methodMapSource)
            throws ObjectIntegrityException, RepositoryConfigurationException,
            GeneralException {
        if (methodMapHandler == null) {
            methodMapHandler =
                    (MmapParser) parse(methodMapSource,
                                       new MmapParser(parentPID));
        }
        return methodMapHandler.getMethodMap();
    }

    private Service getService(InputSource wsdlSource)
            throws ObjectIntegrityException, RepositoryConfigurationException,
            GeneralException {
        if (wsdlHandler == null) {
            wsdlHandler = (WSDLParser) parse(wsdlSource, new WSDLParser());
        }
        return wsdlHandler.getService();
    }

    private DefaultHandler parse(InputSource xml, DefaultHandler eventHandler)
            throws ObjectIntegrityException, RepositoryConfigurationException,
            GeneralException {
        try {
            // XMLSchema validation via SAX parser
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            spf.setValidating(false);
            SAXParser sp = spf.newSAXParser();
            DefaultHandler handler = eventHandler;
            XMLReader xmlreader = sp.getXMLReader();
            xmlreader.setContentHandler(handler);
            xmlreader.parse(xml);
            return handler;
        } catch (ParserConfigurationException e) {
            String msg =
                    "ServiceMapper returned parser error. "
                            + "The underlying exception was a "
                            + e.getClass().getName() + ".  "
                            + "The message was " + "\"" + e.getMessage() + "\"";
            throw new RepositoryConfigurationException(msg);
        } catch (SAXException e) {
            String msg =
                    "ServiceMapper returned SAXException. "
                            + "The underlying exception was a "
                            + e.getClass().getName() + ".  "
                            + "The message was " + "\"" + e.getMessage() + "\"";
            throw new ObjectIntegrityException(msg);
        } catch (Exception e) {
            String msg =
                    "ServiceMapper returned error. "
                            + "The underlying error was a "
                            + e.getClass().getName() + ".  "
                            + "The message was " + "\"" + e.getMessage() + "\"";
            e.printStackTrace();
            throw new GeneralException(msg);
        }
    }

    private MethodDefOperationBind[] merge(Service service, Mmap methodMap)
            throws ObjectIntegrityException, GeneralException {
        Port port = null;
        Binding binding = null;
        MethodDefOperationBind[] fedoraMethodDefBindings =
                new MethodDefOperationBind[0];

        // If the WSDL Service defines multiple Ports (with Binding) for the abstract operations
        // we must CHOOSE ONE binding for Fedora to work with.
        if (service.ports.length > 1) {
            port = choosePort(service);
        } else {
            port = service.ports[0];
        }

        binding = port.binding;

        // Reflect on the type of binding we are dealing with, then Fedora-ize things.
        if (binding.getClass().getName()
                .equalsIgnoreCase("fedora.server.storage.service.HTTPBinding")) {
            // Initialize the array to hold the Fedora-ized operation bindings.
            fedoraMethodDefBindings =
                    new MethodDefOperationBind[((HTTPBinding) binding).operations.length];

            for (int i = 0; i < ((HTTPBinding) binding).operations.length; i++) {
                // From methodMap which was previously created by parsing Fedora method map metadata
                // which provides a Fedora overlay on the service WSDL.
                MmapMethodDef methodDef =
                        (MmapMethodDef) methodMap.wsdlOperationToMethodDef
                                .get(((HTTPBinding) binding).operations[i].operationName);
                fedoraMethodDefBindings[i] = new MethodDefOperationBind();
                fedoraMethodDefBindings[i].methodName = methodDef.methodName;
                fedoraMethodDefBindings[i].methodLabel = methodDef.methodLabel;
                fedoraMethodDefBindings[i].methodParms = methodDef.methodParms;

                // From WSDL Port found in Service object
                fedoraMethodDefBindings[i].serviceBindingAddress =
                        port.portBaseURL;

                // From WSDL Binding found in Service object
                fedoraMethodDefBindings[i].protocolType =
                        MethodDefOperationBind.HTTP_MESSAGE_PROTOCOL;
                fedoraMethodDefBindings[i].operationLocation =
                        ((HTTPBinding) binding).operations[i].operationLocation;
                fedoraMethodDefBindings[i].operationURL =
                        fedoraMethodDefBindings[i].serviceBindingAddress
                                .concat(fedoraMethodDefBindings[i].operationLocation);

                // Get the list of datastream input binding keys that pertain to the particular
                // operation binding.  From the WSDL perspective, the datastream input keys
                // are WSDL message parts that, according the the Fedora Method Map, are
                // datastream inputs to the operation.

                MmapMethodParmDef[] parms = methodDef.wsdlMsgParts;
                Vector<String> tmp_dsInputKeys = new Vector<String>();
                for (MmapMethodParmDef element : parms) {
                    if (element.parmType
                            .equalsIgnoreCase(MethodParmDef.DATASTREAM_INPUT)) {
                        tmp_dsInputKeys.add(element.parmName);
                    }
                }
                fedoraMethodDefBindings[i].dsBindingKeys =
                        (String[]) tmp_dsInputKeys.toArray(new String[0]);

                // Set the outputMIMETypes from the operation's output binding, if any
                HTTPOperationInOut oBind =
                        ((HTTPBinding) binding).operations[i].outputBinding;
                if (oBind != null) {
                    Vector<String> tmp_outputMIMETypes = new Vector<String>();
                    for (MIMEContent element : oBind.ioMIMEContent) {
                        tmp_outputMIMETypes.add(element.mimeType);
                    }
                    fedoraMethodDefBindings[i].outputMIMETypes =
                            (String[]) tmp_outputMIMETypes
                                    .toArray(new String[0]);
                }

            }
        } else if (binding.getClass().getName()
                .equalsIgnoreCase("fedora.server.storage.service.SOAPBinding")) {
            // FIXIT!!  Implement this!
        }
        return fedoraMethodDefBindings;
    }

    private Port choosePort(Service service) {
        // If there is an HTTP binding, this will be preferred.
        for (Port element : service.ports) {
            Binding binding = element.binding;
            if (binding
                    .getClass()
                    .getName()
                    .equalsIgnoreCase("fedora.server.storage.service.HTTPBinding")) {
                return element;
            }
        }
        // Otherwise, just return the first port for binding
        return service.ports[0];
    }
}
