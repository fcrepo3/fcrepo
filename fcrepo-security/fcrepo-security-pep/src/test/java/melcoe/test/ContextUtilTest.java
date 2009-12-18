
package melcoe.test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import melcoe.fedora.pep.PDPClient;
import melcoe.fedora.pep.WebServicesPDPClient;
import melcoe.xacml.util.ContextUtil;
import melcoe.xacml.util.RelationshipResolverImpl;

import org.apache.axis.AxisFault;

import com.sun.xacml.Indenter;
import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.RequestCtx;

import fedora.common.Constants;

public class ContextUtilTest {

    public static void main(String[] args) throws Exception {
        // ContextUtil contextUtil = new ContextUtil(new RelationshipResolverTrippiImpl());
        Map<String, String> options = new HashMap<String, String>();
        options.put("url", "http://localhost:8080/fedora/melcoerisearch");
        options.put("username", "");
        options.put("password", "");
        ContextUtil contextUtil =
                new ContextUtil(new RelationshipResolverImpl(options));
        RequestCtx req =
                contextUtil.buildRequest(getSubjects("public"),
                                         getActions(),
                                         getResources().get(0),
                                         getEnvironment());

        req.encode(System.out, new Indenter());

        options = new HashMap<String, String>();
        options.put("ServiceEndpoint",
                    "http://localhost:8080/axis2/services/MelcoePDP");
        PDPClient client = new WebServicesPDPClient(options);
        String res = client.evaluate(contextUtil.makeRequestCtx(req));
        System.out.println(res);
    }

    private static List<Map<URI, AttributeValue>> getResources()
            throws URISyntaxException {
        Map<URI, AttributeValue> resAttr = null;
        List<Map<URI, AttributeValue>> resList =
                new ArrayList<Map<URI, AttributeValue>>();

        resAttr = new HashMap<URI, AttributeValue>();
        resAttr.put(Constants.OBJECT.PID.getURI(),
                    new StringAttribute("demo:SmileyBeerGlass"));
        resAttr
                .put(new URI("urn:oasis:names:tc:xacml:1.0:resource:resource-id"),
                     new AnyURIAttribute(new URI("demo:SmileyBeerGlass")));
        // resAttr.put(Constants.DATASTREAM.ID.getURI(), new AnyURIAttribute(new URI("RELS-EXT")));
        resAttr.put(Constants.DATASTREAM.MIME_TYPE.getURI(),
                    new StringAttribute("text/xml"));
        resAttr.put(Constants.DATASTREAM.FORMAT_URI.getURI(),
                    new AnyURIAttribute(new URI("some:format:or:the:other")));
        resAttr.put(Constants.DATASTREAM.LOCATION.getURI(),
                    new StringAttribute("http://www.whipitgood.com"));
        resAttr.put(Constants.DATASTREAM.CONTROL_GROUP.getURI(),
                    new StringAttribute("E"));
        resAttr.put(Constants.DATASTREAM.STATE.getURI(),
                    new StringAttribute("ACTIVE"));
        resList.add(resAttr);

        return resList;
    }

    private static Map<URI, AttributeValue> getActions()
            throws URISyntaxException {
        Map<URI, AttributeValue> actions = new HashMap<URI, AttributeValue>();
        actions.put(Constants.ACTION.API.getURI(),
                    new StringAttribute(Constants.ACTION.APIM.getURI()
                            .toASCIIString()));
        actions.put(Constants.ACTION.ID.getURI(),
                    new StringAttribute(Constants.ACTION.LIST_DATASTREAMS
                            .getURI().toASCIIString()));
        actions.put(new URI("urn:oasis:names:tc:xacml:1.0:action:action-id"),
                    new StringAttribute(Constants.ACTION.ADD_DATASTREAM
                            .getURI().toASCIIString()));
        return actions;
    }

    private static List<Map<URI, List<AttributeValue>>> getSubjects(String uid)
            throws AxisFault {
        // setup the id and value for the requesting subject
        Map<URI, List<AttributeValue>> subAttr =
                new HashMap<URI, List<AttributeValue>>();
        List<AttributeValue> attrList = null;
        try {
            attrList = new ArrayList<AttributeValue>();
            attrList.add(new StringAttribute(uid));
            subAttr.put(Constants.SUBJECT.LOGIN_ID.getURI(), attrList);

            attrList = new ArrayList<AttributeValue>();
            attrList.add(new StringAttribute(uid));
            subAttr
                    .put(new URI("urn:oasis:names:tc:xacml:1.0:subject:subject-id"),
                         attrList);
        } catch (URISyntaxException use) {
            throw AxisFault.makeFault(use);
        }

        List<Map<URI, List<AttributeValue>>> subjects =
                new ArrayList<Map<URI, List<AttributeValue>>>();
        subjects.add(subAttr);

        return subjects;
    }

    private static Map<URI, AttributeValue> getEnvironment() {
        return new HashMap<URI, AttributeValue>();
    }
}
