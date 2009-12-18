/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.messaging;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.apache.log4j.Logger;
import org.dom4j.DocumentException;
import org.trippi.RDFFormat;
import org.trippi.TrippiException;

import fedora.server.storage.types.TupleArrayTripleIterator;
import fedora.common.Constants;
import fedora.server.errors.MessagingException;
import fedora.server.storage.types.RelationshipTuple;
import fedora.server.utilities.DateUtility;

/**
 * Representation of an API-M method call as an Atom entry.
 * <ul>
 * <li>atom:title corresponds to the method name, e.g. ingest</li>
 * <li>Each atom:category corresponds to a method's argument:
 * <ul>
 * <li>The scheme indicates the argument name</li>
 * <li>The term indicates the argument value. However, null values are
 * indicated as "null", and non-null <code>xsd:base64Binary</code> values are
 * indicated as "[OMITTED]".</li>
 * <li>The label indicates the argument datatype</li>
 * </ul>
 * </li>
 * <li>atom:content corresponds to the textual representation of the method's
 * return value, noting the following:
 * <ul>
 * <li>Null values are represented as "null".</li>
 * <li><code>fedora-types:ArrayOfString</code> values are represented as a
 * comma-separated list, e.g. "value1, value2, value3".</li>
 * <li>Non-null <code>xsd:base64Binary</code> values are not returned, and
 * only indicated as "[OMITTED]".</li>
 * <li>Non-null <code>fedora-types:Datastream</code> values are not returned,
 * and only indicated as "[OMITTED]".</li>
 * <li><code>fedora-types:RelationshipTuple</code> values are represented in
 * Notation3 (N3).</li>
 * </ul>
 * </li>
 * <li>atom:uri element of atom:author corresponds to the baseURL of the Fedora
 * repository, e.g. http://localhost:8080/fedora.</li>
 * <li>atom:summary corresponds to the PID of the method, if applicable.</li>
 * </ul>
 * 
 * @see <a href="http://atomenabled.org/developers/syndication/atom-format-spec.php">The Atom Syndication Format</a>
 * 
 * @author Edwin Shin
 * @since 3.0
 * @version $Id$
 */
public class AtomAPIMMessage
        implements APIMMessage {

    /** Logger for this class. */
    private static Logger LOG =
            Logger.getLogger(AtomAPIMMessage.class.getName());
    
    private Abdera abdera = Abdera.getInstance();

    private final static String TYPES_NS = Constants.TYPES.uri;

    private final static String TYPES_PREFIX = "fedora-types";
    
    private final static String versionPredicate = Constants.VIEW.VERSION.uri;
    
    private final static String formatPredicate = "http://www.fedora.info/definitions/1/0/types/formatURI";
    
    private static FedoraTypes fedoraTypes;
    
    private String fedoraBaseUrl;
    
    private String serverVersion;

    private String format;

    private String methodName;

    private String pid;

    private Date date;

    private String author;

    private Method method;

    private Object[] args;

    private Object returnVal;

    private Entry entry;

    public AtomAPIMMessage(FedoraMethod method, String fedoraBaseUrl, String serverVersion, String format)
            throws MessagingException {
        if (fedoraTypes == null) {
            try {
                fedoraTypes = new FedoraTypes();
            } catch (FileNotFoundException e) {
                throw new MessagingException(e.getMessage(), e);
            } catch (DocumentException e) {
                throw new MessagingException(e.getMessage(), e);
            }
        }
        this.method = method.getMethod();
        this.args = method.getParameters();
        returnVal = method.getReturnValue();
        this.fedoraBaseUrl = fedoraBaseUrl;
        this.serverVersion = serverVersion;
        this.format = format;
        methodName = method.getName();        
        pid = method.getPID() == null ? "" : method.getPID().toString();
        date = method.getDate() == null ? new Date() : method.getDate();

        if (method.getContext() != null) {
            author =
                    method.getContext()
                            .getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri);
        } else {
            author = "unknown";
        }

        entry = abdera.getFactory().newEntry();
        entry.declareNS(Constants.XML_XSD.uri, "xsd");
        entry.declareNS(TYPES_NS, TYPES_PREFIX);

        setEntryId();
        setUpdated();
        setAuthor();
        setTitle();
        addMethodParameters();
        if (pid != null || !pid.equals("")) {
            entry.setSummary(pid);
        }
        setReturnValue();
        
        if (serverVersion != null && !serverVersion.equals(""))
            entry.addCategory(versionPredicate, serverVersion, null);
        if (format != null && !format.equals(""))
            entry.addCategory(formatPredicate, format, null);
    }

    public AtomAPIMMessage(String messageText) {
        Parser parser = abdera.getParser();
        Document<Entry> entryDoc = parser.parse(new StringReader(messageText));
        entry = entryDoc.getRoot();
        methodName = entry.getTitle();
        date = entry.getUpdated();
        author = entry.getAuthor().getName();
        fedoraBaseUrl = entry.getAuthor().getUri().toString();

        pid = entry.getSummary();
        returnVal = entry.getContent();
        
        serverVersion = getCategoryTerm(versionPredicate);
        format = getCategoryTerm(formatPredicate);
    }

    private void setEntryId() {
        entry.setId("urn:uuid:" + UUID.randomUUID().toString());
    }
    
    /**
     * Set the entry's atom:author element using author from the Context
     * if it was available. Set the author:uri to fedoraBaseUrl.
     */
    private void setAuthor() {
        entry.addAuthor(author, null, fedoraBaseUrl);
    }

    private void setTitle() {
        entry.setTitle(methodName);
    }

    private void setUpdated() {
        entry.setUpdated(date);
    }

    private void addMethodParameters() {
        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < annotations.length; i++) {
            String parameter = getParameterName(method, i);
            String datatype = fedoraTypes.getDatatype(methodName, parameter);
            if (datatype != null) {
                String scheme = TYPES_PREFIX + ":" + parameter;
                entry.addCategory(scheme, objectToString(args[i], datatype), datatype);
            } else {
                // parameters not defined in the WSDL are silently dropped (e.g. Context)
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Silently dropping parameter not defined in the WSDL: " + parameter);
                }
            }
        }
    }

    private void setReturnValue() {
        String m = methodName + "Response";
        String parameter = fedoraTypes.getResponseParameter(m);
        String datatype = fedoraTypes.getDatatype(m, parameter);
        String term = objectToString(returnVal, datatype);
        entry.setContent(term);
    }
    
    /**
     * Get the String value of an object based on its class or 
     * XML Schema Datatype.
     * 
     * @param obj
     * @param xsdType
     * @return
     */
    private String objectToString(Object obj, String xsdType) {
        if (obj == null) {
            return "null";
        }      
        String javaType = obj.getClass().getCanonicalName();
        String term;
        if (javaType.equals("java.util.Date")) {
            term = DateUtility.convertDateToXSDString((Date) obj);
        } else if (xsdType.equals("fedora-types:ArrayOfString")) {
            term = array2string(obj);
        } else if (xsdType.equals("xsd:boolean")) {
            term = obj.toString();
        } else if (xsdType.equals("xsd:nonNegativeInteger")) {
            term = obj.toString();
        } else if (xsdType.equals("fedora-types:RelationshipTuple")) {
            RelationshipTuple[] tuples = (RelationshipTuple[]) obj;
            TupleArrayTripleIterator iter =
                    new TupleArrayTripleIterator(new ArrayList<RelationshipTuple>(Arrays
                            .asList(tuples)));
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try {
                iter.toStream(os, RDFFormat.NOTATION_3, false);
            } catch (TrippiException e) {
                e.printStackTrace();
            }
            term = new String(os.toByteArray());
        } else if (javaType.equals("java.lang.String")) {
            term = (String) obj;
            term = term.replaceAll("\"", "'");
        } else {
            term = "[OMITTED]";
        }
        return term;
    }

    /**
     * Serialization of the API-M message as an Atom entry. {@inheritDoc}
     */
    public String toString() {
        Writer sWriter = new StringWriter();

        try {
            entry.writeTo("prettyxml", sWriter);
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
        return sWriter.toString();
    }

    /**
     * 
     * {@inheritDoc}
     */
    public String getBaseUrl() {
        return fedoraBaseUrl;
    }

    /**
     * 
     * {@inheritDoc}
     */
    public Date getDate() {
        return date;
    }

    /**
     * 
     * {@inheritDoc}
     */
    public String getMethodName() {
        return methodName;
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    public String getPID() {
        return pid;
    }

    public String getAuthor() {
        return author;
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    public String getFormat() {
        return format;
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    public String getServerVersion() {
        return serverVersion;
    }
    
    private static <A extends Annotation> A getParameterAnnotation(Method m,
                                                                   int paramIndex,
                                                                   Class<A> annot) {
        for (Annotation a : m.getParameterAnnotations()[paramIndex]) {
            if (annot.isInstance(a)) return annot.cast(a);
        }
        return null;
    }
    
    /**
     * Get the name of a method parameter via its <code>PName</code> annotation.
     * 
     * @param m 
     * @param paramIndex the index of the parameter array.
     * @return the parameter name or an empty string if not available.
     */
    private static String getParameterName(Method m, int paramIndex) {
        PName pName = getParameterAnnotation(m, paramIndex, PName.class);
        if (pName != null) {
            return pName.value();
        } else {
            return "";
        }
    }

    private static String array2string(Object array) {
        String nullstring = "null";
        String delimiter = ", ";
        if (array == null) {
            return nullstring;
        }

        Object obj = null;
        int length = Array.getLength(array);
        int lastItem = length - 1;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            obj = Array.get(array, i);
            if (obj != null) {
                sb.append(obj);
            } else {
                sb.append(nullstring);
            }
            if (i < lastItem) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }
    
    /**
     * Get the first atom:category term that matches the provided scheme.
     * 
     * @param scheme
     * @return the term or null if no match.
     */
    private String getCategoryTerm(String scheme) {
        List<Category> categories = entry.getCategories(scheme);
        if (categories.isEmpty()) {
            return null;
        } else {
            return categories.get(0).getTerm();
        }
    }
}
