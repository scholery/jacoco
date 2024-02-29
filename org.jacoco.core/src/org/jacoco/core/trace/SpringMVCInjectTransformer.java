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

public class SpringMVCInjectTransformer implements ClassFileTransformer {

	public static final List<String> WEB_SERVER_FILTER = Arrays
			.asList("org/springframework/web/servlet/DispatcherServlet");
	public static final List<String> WEB_SERVER_FILTER_METHOD = Arrays
			.asList("doDispatch");

	@Override
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) {

		if (!WEB_SERVER_FILTER.contains(className)) {
			return classfileBuffer;
		}
		System.out.println(
				"====> TraceId inject : Transforming DispatcherServlet ["
						+ className + "] <====");

		ClassReader cr = new ClassReader(classfileBuffer);
		ClassWriter cw = new ClassWriter(cr,
				ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		cr.accept(new DispatchIject(cw), 0);
		return cw.toByteArray();
	}

	public static class DispatchIject extends ClassVisitor {
		public DispatchIject(ClassVisitor cv) {
			super(InstrSupport.ASM_API_VERSION, cv);
		}

		@Override
		public MethodVisitor visitMethod(int access, String name,
				String descriptor, String signature, String[] exceptions) {
			final MethodVisitor mv = super.visitMethod(access, name, descriptor,
					signature, exceptions);

			if (!WEB_SERVER_FILTER_METHOD.contains(name)) {
				return mv;
			}
			// Inject interceptor logic before doDispatch method in
			// DispatcherServlet
			return new MethodVisitor(api, mv) {
				@Override
				public void visitCode() {
					try {
						// doDispatch load params
						// varIndex 0 is
						// org/springframework/web/servlet/DispatcherServlet:this
						mv.visitVarInsn(Opcodes.ALOAD, 1);
						mv.visitVarInsn(Opcodes.ALOAD, 2);
						// access MyInterceptor.beforeRequest
						mv.visitMethodInsn(Opcodes.INVOKESTATIC,
								Type.getInternalName(
										HttpRequestInterceptor.class),
								"beforeRequest",
								Type.getMethodDescriptor(
										HttpRequestInterceptor.class.getMethod(
												"beforeRequest", Object.class,
												Object.class)),
								false);
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					}
					super.visitCode();
				}
			};
		}
	}
}
