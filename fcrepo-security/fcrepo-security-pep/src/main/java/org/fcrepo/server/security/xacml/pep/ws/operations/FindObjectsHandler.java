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

package org.fcrepo.server.security.xacml.pep.ws.operations;

import java.math.BigInteger;
import java.util.List;

import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.apache.cxf.binding.soap.SoapFault;
import org.fcrepo.server.security.xacml.pep.ContextHandler;
import org.fcrepo.server.security.xacml.pep.PEPException;
import org.fcrepo.server.security.xacml.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.ctx.RequestCtx;

/**
 * @author nishen@melcoe.mq.edu.au
 */
public class FindObjectsHandler
        extends FieldSearchResultHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(FindObjectsHandler.class);

    public FindObjectsHandler(ContextHandler contextHandler)
            throws PEPException {
        super(contextHandler);
    }

    @Override
    public RequestCtx handleResponse(SOAPMessageContext context)
            throws OperationHandlerException {
        logger.debug("FindObjectsHandler/handleResponse!");
        return super.handleResponse(context);
    }

    @Override
    public RequestCtx handleRequest(SOAPMessageContext context)
            throws OperationHandlerException {
        logger.debug("FindObjectsHandler/handleRequest!");

        // Ensuring that there is always a PID present in a request.
        Object oMap = null;

        try {
            oMap = getSOAPRequestObjects(context);
            org.fcrepo.server.types.gen.ArrayOfString resultFields =
                (org.fcrepo.server.types.gen.ArrayOfString) callGetter("getResultFields",oMap);
            BigInteger maxResults = (BigInteger) callGetter("getMaxResults",oMap);
            org.fcrepo.server.types.gen.FieldSearchQuery fieldSearchQuery =
                (org.fcrepo.server.types.gen.FieldSearchQuery) callGetter("getQuery",oMap);

            List<String> resultFieldsList =
                    resultFields.getItem();

            if (!resultFieldsList.contains("pid")) {
                resultFieldsList.add("pid");
            }
            // todo: fix
            // barmintor: How? Why would we set defaults here rather than in the actual module?

//            List<RPCParam> params = new ArrayList<RPCParam>();
//            params
//                    .add(new RPCParam(new QName("http://www.fedora.info/definitions/1/0/types/#FieldSearchResult"),
//                                      newResultFields));
//            params.add(new RPCParam(Constants.XSD_NONNEGATIVEINTEGER,
//                                    maxResults));
//            params
//                    .add(new RPCParam(new QName("http://www.fedora.info/definitions/1/0/types/#FieldSearchQuery"),
//                                      fieldSearchQuery));
//            setSOAPRequestObjects(context, params);

            LogUtil.statLog(getUser(context),
                            org.fcrepo.common.Constants.ACTION.FIND_OBJECTS
                                    .getURI().toASCIIString(),
                            "FedoraRepository",
                            null);
        } catch (SoapFault af) {
            throw new OperationHandlerException("Error filtering objects.", af);
        } catch (Throwable t) {
            throw new OperationHandlerException("Error filtering objects.", t);
        }

        return super.handleRequest(context);
    }
}
