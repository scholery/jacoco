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

import java.io.FileOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class TraceProbeTest {
	public static void main(String[] args) throws Exception {
		String className = TraceTestMap.class.getName();
		ClassReader cr = new ClassReader(className);
		ClassWriter cw = new ClassWriter(cr, 0);
		TraceProbeInsert ca = new TraceProbeInsert(cw);
		cr.accept(ca, 0);

		byte[] b = cw.toByteArray();
		String path = Thread.currentThread().getContextClassLoader()
				.getResource("").getPath();
		FileOutputStream fos = new FileOutputStream(path + "BBATestMap.class");
		fos.write(b);
		fos.close();

		final MemoryClassLoader memoryClassLoader = new MemoryClassLoader();
		memoryClassLoader.addDefinition(className, b);
		final Class<?> targetClass = memoryClassLoader.loadClass(className);

		// Here we execute our test target class through its Runnable interface:
		final Runnable targetInstance = (Runnable) targetClass.newInstance();
		new Thread(targetInstance).start();
	}
}
