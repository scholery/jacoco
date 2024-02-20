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

public class TraceValue {
	public static final String DEFAULT_TRACE_ID = "default_trace_id";
	private static ThreadLocal<String> localVar = new ThreadLocal<String>();

	public static void set(String str) {
		localVar.set(str);
	}

	public static String get() {
		String str = localVar.get();
		if (null == str || str.trim().length() == 0) {
			str = DEFAULT_TRACE_ID;
		}
		return str;
	}

	public static String getOrNUll() {
		return localVar.get();
	}
}
