package org.fcrepo.server.security.xacml.pep.ws.operations;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.apache.cxf.binding.soap.SoapFault;
import org.fcrepo.common.Constants;
import org.fcrepo.server.security.xacml.pep.PEPException;
import org.fcrepo.server.security.xacml.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.DateTimeAttribute;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.RequestCtx;


public class ValidateHandler extends AbstractOperationHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(ValidateHandler.class);

    public ValidateHandler() throws PEPException {
        super();
    }
    @Override
    public RequestCtx handleRequest(SOAPMessageContext context)
            throws OperationHandlerException {
        logger.debug("GetObjectProfileHandler/handleRequest!");

        RequestCtx req = null;
        Object oMap = null;

        String pid = null;
        String asOfDateTime = null;

        try {
            oMap = getSOAPRequestObjects(context);
            logger.debug("Retrieved SOAP Request Objects");
        } catch (SoapFault af) {
            logger.error("Error obtaining SOAP Request Objects", af);
            throw new OperationHandlerException("Error obtaining SOAP Request Objects",
                                                af);
        }

        try {
            pid = (String) callGetter("getPid",oMap);
            asOfDateTime = (String) callGetter("getAsOfDateTime", oMap);
        } catch (Exception e) {
            logger.error("Error obtaining parameters", e);
            throw new OperationHandlerException("Error obtaining parameters.",
                                                e);
        }

        Map<URI, AttributeValue> actions = new HashMap<URI, AttributeValue>();
        Map<URI, AttributeValue> resAttr = new HashMap<URI, AttributeValue>();

        try {
            if (pid != null && !"".equals(pid)) {
                resAttr.put(Constants.OBJECT.PID.getURI(),
                            new StringAttribute(pid));
                resAttr.put(new URI(XACML_RESOURCE_ID),
                        new AnyURIAttribute(new URI(pid)));
            }

            if (asOfDateTime != null && !"".equals(asOfDateTime)) {
                resAttr.put(Constants.DATASTREAM.AS_OF_DATETIME.getURI(),
                            DateTimeAttribute.getInstance(asOfDateTime));
            }

            actions.put(Constants.ACTION.ID.getURI(),
                        new StringAttribute(Constants.ACTION.VALIDATE
                                .getURI().toASCIIString()));
            actions.put(Constants.ACTION.API.getURI(),
                        new StringAttribute(Constants.ACTION.APIM.getURI()
                                .toASCIIString()));

            req =
                    getContextHandler().buildRequest(getSubjects(context),
                                                     actions,
                                                     resAttr,
                                                     getEnvironment(context));

            LogUtil.statLog(getUser(context),
                            Constants.ACTION.VALIDATE.getURI()
                                    .toASCIIString(),
                            pid,
                            null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new OperationHandlerException(e.getMessage(), e);
        }
        return req;
    }

    @Override
    public RequestCtx handleResponse(SOAPMessageContext context)
            throws OperationHandlerException {
        return null;
    }

}
