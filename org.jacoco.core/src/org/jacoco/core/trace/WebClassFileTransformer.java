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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class WebClassFileTransformer implements ClassFileTransformer {

	public static final List<String> WEB_SERVER_FILTER = Arrays
			.asList("org/springframework/web/servlet" + "/DispatcherServlet");
	public static final List<String> WEB_SERVER_FILTER_METHOD = Arrays
			.asList("doDispatch");

	@Override
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) {

		if (WEB_SERVER_FILTER.contains(className)) {
			System.out.println("====> Transforming DispatcherServlet");

			ClassReader cr = new ClassReader(classfileBuffer);
			ClassWriter cw = new ClassWriter(cr,
					ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

			ClassVisitor cv = new ClassVisitor(Opcodes.ASM7, cw) {
				@Override
				public MethodVisitor visitMethod(int access, String name,
						String descriptor, String signature,
						String[] exceptions) {
					MethodVisitor mv = super.visitMethod(access, name,
							descriptor, signature, exceptions);

					// Inject interceptor logic before doDispatch method in
					// DispatcherServlet
					if (WEB_SERVER_FILTER_METHOD.contains(name)) {
						return new MethodVisitor(Opcodes.ASM7, mv) {
							@Override
							public void visitCode() {
								try {
									// 加载doDispatch方法的第一个参数
									// 如果varIndex是0，代表org/springframework/web/servlet/DispatcherServlet那个类的this
									mv.visitVarInsn(Opcodes.ALOAD, 1);
									mv.visitVarInsn(Opcodes.ALOAD, 2);
									// 执行MyInterceptor.beforeRequest 方法
									mv.visitMethodInsn(Opcodes.INVOKESTATIC,
											Type.getInternalName(
													WebInterceptor.class),
											"beforeRequest",
											Type.getMethodDescriptor(
													WebInterceptor.class
															.getMethod(
																	"beforeRequest",
																	Object.class,
																	Object.class)),
											false);
								} catch (NoSuchMethodException e) {
									e.printStackTrace();
								}
								super.visitCode();
							}
						};
					}
					return mv;
				}
			};

			cr.accept(cv, 0);
			return cw.toByteArray();
		}

		return classfileBuffer;
	}
}
