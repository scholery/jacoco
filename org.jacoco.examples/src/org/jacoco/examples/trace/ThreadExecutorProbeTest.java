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

import static org.objectweb.asm.Opcodes.ASM9;

import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import org.jacoco.core.trace.ExecutorsInjectTransformer;
import org.jacoco.core.trace.ThreadPoolFactory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ThreadExecutorProbeTest {

	public void injectExecutors() throws Exception {
		String className = Executors.class.getName();
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

		cr.accept(new ExecutorsInjectTransformer.ExecutorServiceInject(
				className, cw), 0);
		// cr.accept(new ExecutorsInject(cw), 0);
		// write to file
		String path = Thread.currentThread().getContextClassLoader()
				.getResource("").getPath();
		FileOutputStream fos = new FileOutputStream(path + "Executors.class");
		fos.write(cw.toByteArray());
		fos.close();

		// final MemoryClassLoader memoryClassLoader = new MemoryClassLoader();
		// memoryClassLoader.addDefinition(className, cw.toByteArray());
		// final Class<?> targetClass = memoryClassLoader.loadClass(className);
		// final Class<?> targetClass = this.getClass().getClassLoader()
		// .loadClass(className);
		// Here we execute our test target class through its Runnable interface:
		// ExecutorService service = Executors.newFixedThreadPool(1);
		// service.submit(new Runnable() {
		// @Override
		// public void run() {
		// System.out.println("test executors");
		// }
		// });
	}

	class ExecutorsInject extends ClassVisitor {
		public final List<String> INJECT_METHODS = Arrays.asList(
				"newFixedThreadPool", "newWorkStealingPool",
				"newSingleThreadExecutor", "newCachedThreadPool",
				"unconfigurableExecutorService");

		public ExecutorsInject(ClassVisitor classVisitor) {
			super(ASM9, classVisitor);
		}

		@Override
		public MethodVisitor visitMethod(int access, final String name,
				String descriptor, String signature, String[] exceptions) {
			MethodVisitor mv = cv.visitMethod(access, name, descriptor,
					signature, exceptions);
			if (!INJECT_METHODS.contains(name)) {
				return mv;
			}
			return new MethodVisitor(api, mv) {
				public void visitInsn(int opcode) {
					// 这里是访问语句结束，在return结束之前添加语句
					// 其中的 owner 必须被设定为所转换类的名字。现在必须在任意 RETURN 之前添加其他四条
					// 指令，还要在任何 xRETURN 或 ATHROW 之前添加，它们都是终止该方法执行过程的指令。这些
					// 指令没有任何参数，因此在 visitInsn 方法中访问。于是，可以重写这一方法，以增加指令：
					if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
						// 在方法return之前添加代码
						mv.visitMethodInsn(Opcodes.INVOKESTATIC,
								Type.getInternalName(ThreadPoolFactory.class),
								"getTtlExecutorService",
								"(Ljava/util/concurrent/ExecutorService;)Ljava/util/concurrent/ExecutorService;",
								false);
					}
					mv.visitInsn(opcode);
				}
			};
		}
	}

	public static void main(String[] args) throws Exception {
		new ThreadExecutorProbeTest().injectExecutors();
	}
}
