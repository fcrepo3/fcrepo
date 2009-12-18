/*
 * File: FindObjectsHandler.java
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import melcoe.fedora.pep.PEPException;
import melcoe.fedora.util.LogUtil;

import org.apache.axis.AxisFault;
import org.apache.axis.Constants;
import org.apache.axis.MessageContext;
import org.apache.axis.message.RPCParam;
import org.apache.axis.types.NonNegativeInteger;
import org.apache.log4j.Logger;

import com.sun.xacml.ctx.RequestCtx;

import fedora.server.types.gen.FieldSearchQuery;

/**
 * @author nishen@melcoe.mq.edu.au
 */
public class FindObjectsHandler
        extends AbstractOperationHandler {

    private static Logger log =
            Logger.getLogger(FindObjectsHandler.class.getName());

    private FieldSearchResultHandler resultHandler = null;

    public FindObjectsHandler()
            throws PEPException {
        super();
        resultHandler = new FieldSearchResultHandler();
    }

    public RequestCtx handleResponse(MessageContext context)
            throws OperationHandlerException {
        if (log.isDebugEnabled()) {
            log.debug("FindObjectsHandler/handleResponse!");
        }
        return resultHandler.handleResponse(context);
    }

    public RequestCtx handleRequest(MessageContext context)
            throws OperationHandlerException {
        if (log.isDebugEnabled()) {
            log.debug("FindObjectsHandler/handleRequest!");
        }

        // Ensuring that there is always a PID present in a request.
        List<Object> oMap = null;

        try {
            oMap = getSOAPRequestObjects(context);
            String[] resultFields = (String[]) oMap.get(0);
            NonNegativeInteger maxResults = (NonNegativeInteger) oMap.get(1);
            FieldSearchQuery fieldSearchQuery = (FieldSearchQuery) oMap.get(2);

            List<String> resultFieldsList =
                    new ArrayList<String>(Arrays.asList(resultFields));

            if (!resultFieldsList.contains("pid")) {
                resultFieldsList.add("pid");
            }
            String[] newResultFields =
                    resultFieldsList
                            .toArray(new String[resultFieldsList.size()]);

            List<RPCParam> params = new ArrayList<RPCParam>();
            params
                    .add(new RPCParam(new QName("http://www.fedora.info/definitions/1/0/types/#FieldSearchResult"),
                                      newResultFields));
            params.add(new RPCParam(Constants.XSD_NONNEGATIVEINTEGER,
                                    maxResults));
            params
                    .add(new RPCParam(new QName("http://www.fedora.info/definitions/1/0/types/#FieldSearchQuery"),
                                      fieldSearchQuery));
            setSOAPRequestObjects(context, params);

            LogUtil.statLog(context.getUsername(),
                            fedora.common.Constants.ACTION.FIND_OBJECTS
                                    .getURI().toASCIIString(),
                            "FedoraRepository",
                            null);
        } catch (AxisFault af) {
            throw new OperationHandlerException("Error filtering objects.", af);
        }

        return resultHandler.handleRequest(context);
    }
}
