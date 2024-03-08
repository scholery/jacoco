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

import java.util.concurrent.ConcurrentHashMap;

import org.jacoco.core.internal.instr.InstrSupport;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class TraceProbeInsert extends ClassVisitor {
	public static final String className = TraceTestMap.class.getName();

	public TraceProbeInsert(ClassVisitor cv) {
		super(Opcodes.ASM9, cv);
	}

	/**
	 * private static boolean[] test(); descriptor: ()[Z flags: ACC_PRIVATE,
	 * ACC_STATIC Code: stack=3, locals=1, args_size=0 0: getstatic #31 // Field
	 * test:Ljava/util/Map; 3: invokestatic #87 // Method
	 * org/jacoco/core/bba/ThreadValue.get:()Ljava/lang/String; 6: invokedynamic
	 * #91, 0 // InvokeDynamic #0:apply:()Ljava/util/function/Function; 11:
	 * invokeinterface #95, 3 // InterfaceMethod
	 * java/util/Map.computeIfAbsent:(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;
	 * 16: checkcast #76 // class "[Z" 19: astore_0 20: aload_0 21: areturn
	 * LineNumberTable: line 50: 0 line 53: 20 LocalVariableTable: Start Length
	 * Slot Name Signature 20 2 0 attrs [Z
	 * <p>
	 * private static boolean[] test() { boolean[] attrs = (boolean[])
	 * test.computeIfAbsent(ThreadValue.get(), key -&gt; { return new
	 * boolean[3]; }); return attrs; }
	 *
	 */
	@Override
	public void visitEnd() {
		// 添加静态字段
		FieldVisitor fieldVisitor = cv.visitField(InstrSupport.DATAFIELD_ACC,
				InstrSupport.DATAFIELD_NAME, InstrSupport.DATAFIELD_DESC_MAP,
				null, null);
		if (fieldVisitor != null) {
			fieldVisitor.visitEnd();
		}

		// 添加静态初始化块
		final MethodVisitor initBlock = cv.visitMethod(
				InstrSupport.INITMETHOD_ACC, InstrSupport.INITMETHOD_NAME,
				"()" + "[Z", null, null);
		initBlock.visitCode();
		// 1. get map
		initBlock.visitFieldInsn(Opcodes.GETSTATIC, className,
				InstrSupport.DATAFIELD_NAME, InstrSupport.DATAFIELD_DESC_MAP);
		initBlock.visitVarInsn(Opcodes.ASTORE, 0);
		initBlock.visitVarInsn(Opcodes.ALOAD, 0);

		// 2. check map
		Label MAP_NOT_NULL = new Label();
		initBlock.visitJumpInsn(Opcodes.IFNONNULL, MAP_NOT_NULL); // 如果不为null，跳转到ifNotNull标签

		// 3. map null,init
		initBlock.visitTypeInsn(Opcodes.NEW,
				InstrSupport.getClassPath(ConcurrentHashMap.class));
		initBlock.visitInsn(Opcodes.DUP);
		initBlock.visitMethodInsn(Opcodes.INVOKESPECIAL,
				InstrSupport.getClassPath(ConcurrentHashMap.class), "<init>",
				"()V", false);
		// 存放HashMap实例到静态字段MY_MAP
		initBlock.visitFieldInsn(Opcodes.PUTSTATIC, className,
				InstrSupport.DATAFIELD_NAME, InstrSupport.DATAFIELD_DESC_MAP);
		initBlock.visitFieldInsn(Opcodes.GETSTATIC, className,
				InstrSupport.DATAFIELD_NAME, InstrSupport.DATAFIELD_DESC_MAP);
		initBlock.visitVarInsn(Opcodes.ASTORE, 0);

		// 4. map not null,get array
		initBlock.visitLabel(MAP_NOT_NULL);
		initBlock.visitMethodInsn(Opcodes.INVOKESTATIC,
				"org/jacoco/core/trace/ThreadValue", "get",
				"()Ljava/lang/String;", false);
		initBlock.visitVarInsn(Opcodes.ASTORE, 1);
		// get array
		initBlock.visitVarInsn(Opcodes.ALOAD, 0);
		initBlock.visitVarInsn(Opcodes.ALOAD, 1);
		initBlock.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map",
				"get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);

		// type convert
		// initBlock.visitTypeInsn(Opcodes.CHECKCAST, "[Z");
		initBlock.visitVarInsn(Opcodes.ASTORE, 2);
		initBlock.visitVarInsn(Opcodes.ALOAD, 2);

		Label ATTR_NOT_NULL = new Label();
		// 5. check array null
		initBlock.visitJumpInsn(Opcodes.IFNONNULL, ATTR_NOT_NULL); // 如果不为null，跳转到ifNotNull标签

		// 6. array null,init and set , then return
		initBlock.visitInsn(Opcodes.ICONST_3); // 设置数组大小为3
		initBlock.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BOOLEAN); // 初始化数组
		initBlock.visitVarInsn(Opcodes.ASTORE, 3);

		// initBlock.visitInsn(Opcodes.POP);
		final int size = 0;

		// Stack[0]: [Z

		// Return the class' probe array:
		// if (withFrames) {
		// initBlock.visitFrame(Opcodes.F_NEW, 0, FRAME_LOCALS_EMPTY, 1,
		// FRAME_STACK_ARRZ);
		// }

		initBlock.visitVarInsn(Opcodes.ALOAD, 0);
		initBlock.visitVarInsn(Opcodes.ALOAD, 1);
		initBlock.visitVarInsn(Opcodes.ALOAD, 3);
		initBlock.visitMethodInsn(Opcodes.INVOKEINTERFACE,
				InstrSupport.getClassPath(ConcurrentHashMap.class), "put",
				"(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
				true);

		initBlock.visitVarInsn(Opcodes.ALOAD, 3);
		initBlock.visitInsn(Opcodes.ARETURN);

		// 7. array not null return
		initBlock.visitLabel(ATTR_NOT_NULL);
		initBlock.visitVarInsn(Opcodes.ALOAD, 2);
		initBlock.visitInsn(Opcodes.ARETURN);

		initBlock.visitMaxs(Math.max(size, 2), 0); // Maximum local stack size
													// is 2
		initBlock.visitEnd();
	}
}
