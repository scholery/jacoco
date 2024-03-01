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

import java.lang.reflect.Method;

public class HttpRequestInterceptor {
	public static final String TRACE_ID_HEADER = "X-Trace-ID";

	public static void beforeRequest(Object request, Object response) {
		String traceId = getTraceId(request);
		TraceValue.set(traceId);
	}

	public static String getTraceId(Object request) {
		String traceId = null;
		try {
			Class<?> requestClass = request.getClass();
			Method method = requestClass.getMethod("getHeader", String.class);
			traceId = (String) method.invoke(request, TRACE_ID_HEADER);
		} catch (Exception e) {
			System.err.println("=============> Not support HTTP request("
					+ request.getClass().getName() + "):" + e.getMessage());
		}
		return traceId;
	}
}
