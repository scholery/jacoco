/*******************************************************************************
 * Copyright (c) 2009, 2023 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Kevin. Zhu - initial API and implementation
 *
 *******************************************************************************/
package org.jacoco.core.trace;

import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.connector.RequestFacade;

public class WebInterceptor {

	public static void beforeRequest(Object request, Object response) {
		System.out.println(
				"=============> beforeRequest Intercepting HTTP request:"
						+ request.getClass().getName());
		String traceId = null;
		if (request instanceof HttpServletRequest) {
			HttpServletRequest httpServletRequest = (HttpServletRequest) request;
			traceId = httpServletRequest.getHeader("X-Trace-ID");
		} else if (request instanceof RequestFacade) {
			RequestFacade facade = (RequestFacade) request;
			traceId = facade.getHeader("X-Trace-ID");
		}
		if (null != traceId) {
			System.out.println("X-Trace-ID ====> " + traceId);
		}
		TraceValue.set(traceId);
	}
}
