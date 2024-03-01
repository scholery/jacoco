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
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadPoolExecutor;

import org.jacoco.core.trace.ExecutorsInjectTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class ThreadPoolProbeTest {

	public void injectExecutors(String className) throws Exception {
		// --------------------------------------------------------------------------------
		// 1.在使用 new ClassWriter(0)时，不会自动计算任何东西。必须自行计算帧、局部变 量与操作数栈的大小。
		// --------------------------------------------------------------------------------
		// 2.在使用 new ClassWriter(ClassWriter.COMPUTE_MAXS)时，将为你计算局部变量与操作数栈部分的大小。
		// 还是必须调用 visitMaxs，但可以使用任何参数：它们将被忽略并重新计算。使用这一选项时，仍然必须自行计算这些帧。
		// --------------------------------------------------------------------------------
		// 3.在 new ClassWriter(ClassWriter.COMPUTE_FRAMES)时，一切都是自动计算。不再需要调用
		// visitFrame，
		// 但仍然必须调用 visitMaxs（参数将被忽略并重新计算）
		// --------------------------------------------------------------------------------
		ClassReader cr = new ClassReader(className);
		ClassWriter cw = new ClassWriter(cr,
				ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS);

		cr.accept(new ExecutorsInjectTransformer.ExecutorInject(className, cw),
				0);
		// cr.accept(new ExecutorsInject(cw), 0);
		// write to file
		String path = Thread.currentThread().getContextClassLoader()
				.getResource("").getPath();
		FileOutputStream fos = new FileOutputStream(
				path + className.substring(className.lastIndexOf(".") + 1)
						+ ".class");
		fos.write(cw.toByteArray());
		fos.close();
	}

	public static void main(String[] args) throws Exception {
		new ThreadPoolProbeTest()
				.injectExecutors(ThreadPoolExecutor.class.getName());
		new ThreadPoolProbeTest().injectExecutors(ForkJoinPool.class.getName());
	}
}
