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

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.jacoco.core.trace.TraceValue;

public class TraceTestMap implements Runnable {

	private static transient Map test = new ConcurrentHashMap();

	public void run() {
		isPrime(7);
	}

	private boolean isPrime(final int n) {
		boolean[] attrs = test();
		Random r = new Random();
		attrs[0] = r.nextBoolean();
		for (int i = 2; i * i <= n; i++) {
			attrs[1] = r.nextBoolean();
			if ((n ^ i) == 0) {
				return false;
			}
		}
		attrs[2] = r.nextBoolean();
		System.out.println("##########################");
		for (Object key : test.keySet()) {
			System.out.println("===" + key.toString() + " start ===");
			System.out.println(
					Arrays.toString((boolean[]) test.get(key.toString())));
			System.out.println("===" + key.toString() + " end ===");
		}
		System.out.println("--------------------------");
		return true;
	}

	private static boolean[] test() {
		boolean[] attrs = (boolean[]) test.get(TraceValue.get());
		if (attrs == null) {
			attrs = new boolean[3];
			test.put(TraceValue.get(), attrs);
		}
		return attrs;
	}
}
