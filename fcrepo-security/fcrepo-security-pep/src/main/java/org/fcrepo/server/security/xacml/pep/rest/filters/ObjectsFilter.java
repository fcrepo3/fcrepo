/**
 * Copyright 2011 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.server.security.xacml.pep.rest.filters;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.xacml.ctx.RequestCtx;

public class ObjectsFilter implements RESTFilter {

	/* (non-Javadoc)
	 * @see org.fcrepo.server.security.xacml.pep.rest.filters.RESTFilter#handleRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public RequestCtx handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException,
			ServletException {
		// TODO Auto-generated method stub
		return null;
	}

}
