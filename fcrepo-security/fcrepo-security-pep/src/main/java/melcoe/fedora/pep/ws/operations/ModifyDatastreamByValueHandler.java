/*
 * File: ModifyDatastreamByValueHandler.java
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
public class ModifyDatastreamByValueHandler
        extends AbstractOperationHandler {

    private static Logger log =
            Logger.getLogger(ModifyDatastreamByValueHandler.class.getName());

    public ModifyDatastreamByValueHandler()
            throws PEPException {
        super();
    }

    public RequestCtx handleResponse(MessageContext context)
            throws OperationHandlerException {
        return null;
    }

    public RequestCtx handleRequest(MessageContext context)
            throws OperationHandlerException {
        log.debug("ModifyDatastreamByValueHandler/handleRequest!");

        RequestCtx req = null;
        List<Object> oMap = null;

        String pid = null;
        String dsID = null;
        // String[] altIDs = null;
        // String dsLabel = null;
        String mimeType = null;
        String formatURI = null;
        String checksumType = null;
        String checksum = null;
        // String logMessage = null;
        // Boolean force = null;

        try {
            oMap = getSOAPRequestObjects(context);
            log.debug("Retrieved SOAP Request Objects");
        } catch (AxisFault af) {
            log.error("Error obtaining SOAP Request Objects", af);
            throw new OperationHandlerException("Error obtaining SOAP Request Objects",
                                                af);
        } catch (Exception e) {
            log.error("Error obtaining parameters", e);
            throw new OperationHandlerException("Error obtaining parameters.",
                                                e);
        }

        log.debug("Extracted SOAP Request Objects");

        try {
            pid = (String) oMap.get(0);
            dsID = (String) oMap.get(1);
            // altIDs = (String[]) oMap.get(2);
            // dsLabel = (String) oMap.get(3);
            mimeType = (String) oMap.get(4);
            formatURI = (String) oMap.get(5);
            // param 7 is dsContent
            checksumType = (String) oMap.get(7);
            checksum = (String) oMap.get(8);
            // logMessage = (String) oMap.get(9);
            // force = (Boolean) oMap.get(10);
        } catch (Exception e) {
            log.error("Error obtaining parameters", e);
            throw new OperationHandlerException("Error obtaining parameters.",
                                                e);
        }

        log.debug("Extracted SOAP Request Objects");

        Map<URI, AttributeValue> actions = new HashMap<URI, AttributeValue>();
        Map<URI, AttributeValue> resAttr = new HashMap<URI, AttributeValue>();

        try {
            if (pid != null && !"".equals(pid)) {
                resAttr.put(Constants.OBJECT.PID.getURI(),
                            new StringAttribute(pid));
            }
            if (pid != null && !"".equals(pid)) {
                resAttr.put(new URI(XACML_RESOURCE_ID),
                            new AnyURIAttribute(new URI(pid)));
            }
            if (dsID != null && !"".equals(dsID)) {
                resAttr.put(Constants.DATASTREAM.ID.getURI(),
                            new StringAttribute(dsID));
            }
            if (mimeType != null && !"".equals(mimeType)) {
                resAttr.put(Constants.DATASTREAM.NEW_MIME_TYPE.getURI(),
                            new StringAttribute(mimeType));
            }
            if (formatURI != null && !"".equals(formatURI)) {
                resAttr.put(Constants.DATASTREAM.NEW_FORMAT_URI.getURI(),
                            new AnyURIAttribute(new URI(formatURI)));
            }
            if (checksumType != null && !"".equals(checksumType)) {
                resAttr.put(Constants.DATASTREAM.NEW_CHECKSUM_TYPE.getURI(),
                            new StringAttribute(checksumType));
            }
            if (checksum != null && !"".equals(checksum)) {
                resAttr.put(Constants.DATASTREAM.NEW_CHECKSUM.getURI(),
                            new StringAttribute(checksum));
            }

            actions
                    .put(Constants.ACTION.ID.getURI(),
                         new StringAttribute(Constants.ACTION.MODIFY_DATASTREAM_BY_VALUE
                                 .getURI().toASCIIString()));
            actions.put(Constants.ACTION.API.getURI(),
                        new StringAttribute(Constants.ACTION.APIM.getURI()
                                .toASCIIString()));

            req =
                    getContextHandler().buildRequest(getSubjects(context),
                                                     actions,
                                                     resAttr,
                                                     getEnvironment(context));

            LogUtil.statLog(context.getUsername(),
                            Constants.ACTION.MODIFY_DATASTREAM_BY_VALUE
                                    .getURI().toASCIIString(),
                            pid,
                            dsID);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new OperationHandlerException(e.getMessage(), e);
        }

        return req;
    }
}
