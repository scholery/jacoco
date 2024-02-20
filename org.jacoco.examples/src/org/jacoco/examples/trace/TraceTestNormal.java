/*******************************************************************************
 * Copyright (c) 2009, 2023 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *
 *******************************************************************************/
package org.jacoco.examples.trace;

public class TraceTestNormal {
	public void run() {
		isPrime(7);
	}

	private boolean isPrime(final int n) {
		for (int i = 2; i * i <= n; i++) {
			if ((n ^ i) == 0) {
				return false;
			}
		}
		return true;
	}
}
