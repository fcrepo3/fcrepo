/*
 * File: IngestHandler.java
 *
 * Copyright 2007 Macquarie E-Learning Centre Of Excellence
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package melcoe.fedora.pep.ws.operations;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import melcoe.fedora.pep.PEPException;
import melcoe.fedora.util.LogUtil;

import org.apache.axis.AxisFault;
import org.apache.axis.MessageContext;
import org.apache.log4j.Logger;

import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.RequestCtx;

import fedora.common.Constants;

/**
 * @author nishen@melcoe.mq.edu.au
 */
public class IngestHandler
        extends AbstractOperationHandler {

    private static Logger log = Logger.getLogger(IngestHandler.class.getName());

    public IngestHandler()
            throws PEPException {
        super();
    }

    public RequestCtx handleResponse(MessageContext context)
            throws OperationHandlerException {
        return null;
    }

    public RequestCtx handleRequest(MessageContext context)
            throws OperationHandlerException {
        log.debug("IngestHandler/handleRequest!");

        RequestCtx req = null;
        List<Object> oMap = null;

        String format = null;

        try {
            oMap = getSOAPRequestObjects(context);
            log.debug("Retrieved SOAP Request Objects");
        } catch (AxisFault af) {
            log.error("Error obtaining SOAP Request Objects", af);
            throw new OperationHandlerException("Error obtaining SOAP Request Objects",
                                                af);
        }

        try {
            format = (String) oMap.get(1);
        } catch (Exception e) {
            log.error("Error obtaining parameters", e);
            throw new OperationHandlerException("Error obtaining parameters.",
                                                e);
        }

        log.debug("Extracted SOAP Request Objects");

        Map<URI, AttributeValue> actions = new HashMap<URI, AttributeValue>();
        Map<URI, AttributeValue> resAttr = new HashMap<URI, AttributeValue>();

        try {
            resAttr.put(Constants.OBJECT.PID.getURI(),
                        new StringAttribute("FedoraRepository"));
            resAttr.put(new URI(XACML_RESOURCE_ID),
                        new AnyURIAttribute(new URI("FedoraRepository")));
            if (format != null && !"".equals(format)) {
                resAttr.put(Constants.OBJECT.FORMAT_URI.getURI(),
                            new StringAttribute(format));
            }

            actions.put(Constants.ACTION.ID.getURI(),
                        new StringAttribute(Constants.ACTION.INGEST.getURI()
                                .toASCIIString()));
            actions.put(Constants.ACTION.API.getURI(),
                        new StringAttribute(Constants.ACTION.APIM.getURI()
                                .toASCIIString()));

            req =
                    getContextHandler().buildRequest(getSubjects(context),
                                                     actions,
                                                     resAttr,
                                                     getEnvironment(context));

            LogUtil.statLog(context.getUsername(), Constants.ACTION.INGEST
                    .getURI().toASCIIString(), "FedoraRepository", null);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new OperationHandlerException(e.getMessage(), e);
        }

        return req;
    }
}
