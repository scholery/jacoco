/*******************************************************************************
 * Copyright (c) 2009, 2023 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Kevin. Zhu - initial API and implementation
 *
 *******************************************************************************/
package org.jacoco.core.trace;

import java.lang.reflect.Executable;
import java.security.ProtectionDomain;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

public class HttpRequestTransformer implements AgentBuilder.Transformer {

	@Override
	public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
			TypeDescription typeDescription, ClassLoader classLoader,
			JavaModule javaModule, ProtectionDomain protectionDomain) {

		final AsmVisitorWrapper delMappingVisitor = Advice
				.to(EnterAdvice.class, ExitAdviceMethods.class)
				.on(ElementMatchers.isAnnotatedWith(ElementMatchers.named(
						"org.springframework.web.bind.annotation.DeleteMapping")));

		final AsmVisitorWrapper getMappingVisitor = Advice
				.to(EnterAdvice.class, ExitAdviceMethods.class)
				.on(ElementMatchers.isAnnotatedWith(ElementMatchers.named(
						"org.springframework.web.bind.annotation.GetMapping")));

		final AsmVisitorWrapper mapping = Advice
				.to(EnterAdvice.class, ExitAdviceMethods.class)
				.on(ElementMatchers.isAnnotatedWith(ElementMatchers.named(
						"org.springframework.web.bind.annotation.Mapping")));

		final AsmVisitorWrapper patchMappingVisitor = Advice
				.to(EnterAdvice.class, ExitAdviceMethods.class)
				.on(ElementMatchers.isAnnotatedWith(ElementMatchers.named(
						"org.springframework.web.bind.annotation.PatchMapping")));

		final AsmVisitorWrapper postMappingVisitor = Advice
				.to(EnterAdvice.class, ExitAdviceMethods.class)
				.on(ElementMatchers.isAnnotatedWith(ElementMatchers.named(
						"org.springframework.web.bind.annotation.PostMapping")));

		final AsmVisitorWrapper putMappingVisitor = Advice
				.to(EnterAdvice.class, ExitAdviceMethods.class)
				.on(ElementMatchers.isAnnotatedWith(ElementMatchers.named(
						"org.springframework.web.bind.annotation.PutMapping")));

		final AsmVisitorWrapper reqMappingVisitor = Advice
				.to(EnterAdvice.class, ExitAdviceMethods.class)
				.on(ElementMatchers.isAnnotatedWith(ElementMatchers.named(
						"org.springframework.web.bind.annotation.RequestMapping")));

		final AsmVisitorWrapper doDispatch = Advice
				.to(EnterAdvice.class, ExitAdviceMethods.class)
				.on(ElementMatchers.named("doDispatch"));
		return builder.visit(doDispatch);
		// return builder.visit(delMappingVisitor).visit(getMappingVisitor)
		// .visit(mapping).visit(patchMappingVisitor)
		// .visit(postMappingVisitor).visit(putMappingVisitor)
		// .visit(reqMappingVisitor);
	}

	public static class EnterAdvice {
		@Advice.OnMethodEnter
		static void enter(@Advice.Origin final Executable executable,
				@Advice.AllArguments Object[] args) {
			// Get the HttpServletRequest,HttpServletResponse from the method
			// arguments
			if (args.length >= 2) {
				HttpRequestInterceptor.beforeRequest(args[0], args[1]);
			}
		}
	}

	/**
	 * intercept doDispatch method
	 */
	public static class ExitAdviceMethods {
		@Advice.OnMethodExit(onThrowable = Throwable.class)
		static void exit(@Advice.Origin final Executable executable,
				@Advice.AllArguments Object[] args) {
			// Get the HttpServletRequest,HttpServletResponse from the method
			// arguments
			// if (args.length >= 2) {
			// HttpRequestInterceptor.beforeRequest(args[0], args[1]);
			// }
		}
	}
}
