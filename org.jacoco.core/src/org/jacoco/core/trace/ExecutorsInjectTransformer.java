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

import org.jacoco.core.internal.instr.InstrSupport;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ExecutorsInjectTransformer implements ClassFileTransformer {
	public static final String EXECUTORS = "java/util/concurrent/Executors";

	@Override
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) {

		if (!EXECUTORS.equals(className)) {
			return classfileBuffer;
		}

		ClassReader cr = new ClassReader(classfileBuffer);
		ClassWriter cw = new ClassWriter(cr,
				ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		cr.accept(new ExecutorServiceInject(className, cw), 0);
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
					"====> Executors inject : Transforming to TtlExecutors ["
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
}
