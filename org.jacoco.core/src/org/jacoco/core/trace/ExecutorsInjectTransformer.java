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

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.List;

import org.jacoco.core.internal.instr.InstrSupport;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ExecutorsInjectTransformer implements ClassFileTransformer {
	public static final String EXECUTORS = "java/util/concurrent/Executors";
	public static final List<String> THREAD_POOLS = Arrays.asList(
			"java/util/concurrent/ThreadPoolExecutor",
			"java" + "/util/concurrent/ForkJoinPool");

	@Override
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) {

		if (!EXECUTORS.equals(className) && !THREAD_POOLS.contains(className)) {
			return classfileBuffer;
		}

		ClassReader cr = new ClassReader(classfileBuffer);
		ClassWriter cw = new ClassWriter(cr,
				ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		if (EXECUTORS.equals(className)) {
			cr.accept(new ExecutorServiceInject(className, cw), 0);
		}

		if (THREAD_POOLS.contains(className)) {
			cr.accept(new ExecutorInject(className, cw), 0);
		}
		return cw.toByteArray();
	}

	public static class ExecutorServiceInject extends ClassVisitor {
		protected String className;

		public ExecutorServiceInject(String className, ClassVisitor cv) {
			super(InstrSupport.ASM_API_VERSION, cv);
			this.className = className;
		}

		@Override
		public MethodVisitor visitMethod(int access, final String name,
				String descriptor, String signature, String[] exceptions) {

			final MethodVisitor mv = cv.visitMethod(access, name, descriptor,
					signature, exceptions);

			// public and static method, and return type is ExecutorService,then
			// inject
			if ((Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC) != access
					|| !descriptor.endsWith(
							")Ljava/util/concurrent/ExecutorService;")) {
				return mv;
			}
			System.out.println(
					"====> ExecutorService inject : Transforming to TtlExecutorService ["
							+ className + ":" + name + "" + descriptor
							+ "] <====");
			return new MethodVisitor(api, mv) {
				public void visitInsn(int opcode) {
					try {
						// 这里是访问语句结束，在return结束之前添加语句
						// 其中的 owner 必须被设定为所转换类的名字。现在必须在任意 RETURN 之前添加其他四条
						// 指令，还要在任何 xRETURN 或 ATHROW 之前添加，它们都是终止该方法执行过程的指令。这些
						// 指令没有任何参数，因此在 visitInsn 方法中访问。于是，可以重写这一方法，以增加指令：
						if (opcode >= Opcodes.IRETURN
								&& opcode <= Opcodes.RETURN) {
							mv.visitMethodInsn(Opcodes.INVOKESTATIC,
									Type.getInternalName(
											ThreadPoolFactory.class),
									"getTtlExecutorService",
									"(Ljava/util/concurrent/ExecutorService;)Ljava/util/concurrent/ExecutorService;",
									false);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					mv.visitInsn(opcode);
				}
			};
		}
	}

	public static class ExecutorInject extends ClassVisitor {
		protected String className;

		public ExecutorInject(String className, ClassVisitor cv) {
			super(InstrSupport.ASM_API_VERSION, cv);
			this.className = className;
		}

		@Override
		public MethodVisitor visitMethod(int access, final String name,
				String descriptor, String signature, String[] exceptions) {

			final MethodVisitor mv = cv.visitMethod(access, name, descriptor,
					signature, exceptions);

			// public and static method, and return type is ExecutorService,then
			// inject
			if (!name.equals("execute") || Opcodes.ACC_PUBLIC != access
					|| !descriptor.endsWith("(Ljava/lang/Runnable;)V")) {
				return mv;
			}
			System.out.println(
					"====> Executors inject : Transforming to TtlExecutor ["
							+ className + ":" + name + "" + descriptor
							+ "] <====");
			return new MethodVisitor(api, mv) {
				@Override
				public void visitCode() {
					super.visitCode();
					// 方法开始（可以在此处添加代码，在原来的方法之前执行）
					try {
						mv.visitVarInsn(Opcodes.ALOAD, 1);
						mv.visitMethodInsn(Opcodes.INVOKESTATIC,
								Type.getInternalName(ThreadPoolFactory.class),
								"getTtlRunnable",
								"(Ljava/lang/Runnable;)Ljava/lang/Runnable;",
								false);
						mv.visitVarInsn(Opcodes.ASTORE, 1);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
		}
	}
}
