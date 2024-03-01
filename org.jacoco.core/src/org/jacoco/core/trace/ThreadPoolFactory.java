/*******************************************************************************
 * Copyright (c) 2009, 2023 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * 		Kevin. Zhu - initial API and implementation
 *
 *******************************************************************************/

package org.jacoco.core.trace;

import java.util.concurrent.ExecutorService;

import com.alibaba.ttl.threadpool.TtlExecutors;

public class ThreadPoolFactory {

	public static ExecutorService getTtlExecutorService(
			ExecutorService executorService) {
		return TtlExecutors.getTtlExecutorService(executorService);
	}
}
