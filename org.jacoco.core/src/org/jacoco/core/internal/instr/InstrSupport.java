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
package org.jacoco.core.internal.instr;

import static java.lang.String.format;

import org.jacoco.core.runtime.IExecutionDataAccessorGenerator;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Constants and utilities for byte code instrumentation.
 */
public final class InstrSupport {

	private InstrSupport() {
	}

	/** ASM API version */
	public static final int ASM_API_VERSION = Opcodes.ASM9;

	// === Data Field ===

	/**
	 * Name of the field that stores coverage information of a class.
	 */
	public static final String DATAFIELD_NAME = "$jacocoData";

	/**
	 * Access modifiers of the field that stores coverage information of a
	 * class.
	 *
	 * According to Java Virtual Machine Specification <a href=
	 * "https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-6.html#jvms-6.5.putstatic">
	 * §6.5.putstatic</a> this field must not be final:
	 *
	 * <blockquote>
	 * <p>
	 * if the field is final, it must be declared in the current class, and the
	 * instruction must occur in the {@code <clinit>} method of the current
	 * class.
	 * </p>
	 * </blockquote>
	 */
	public static final int DATAFIELD_ACC = Opcodes.ACC_PRIVATE
			| Opcodes.ACC_STATIC | Opcodes.ACC_TRANSIENT;

	/**
	 * Access modifiers of the field that stores coverage information of a Java
	 * 8 interface.
	 *
	 * According to Java Virtual Machine Specification <a href=
	 * "https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.5-200-A.3">
	 * §4.5</a>:
	 *
	 * <blockquote>
	 * <p>
	 * Fields of interfaces must have their ACC_PUBLIC, ACC_STATIC, and
	 * ACC_FINAL flags set; they may have their ACC_SYNTHETIC flag set and must
	 * not have any of the other flags.
	 * </p>
	 * </blockquote>
	 */
	public static final int DATAFIELD_INTF_ACC = Opcodes.ACC_PUBLIC
			| Opcodes.ACC_STATIC | Opcodes.ACC_FINAL;

	/**
	 * Data type of the field that stores coverage information for a class (
	 * <code>boolean[]</code>).
	 */
	public static final String DATAFIELD_DESC = "[Z";

	// === Init Method ===

	/**
	 * Name of the initialization method.
	 */
	public static final String INITMETHOD_NAME = "$jacocoInit";

	/**
	 * Descriptor of the initialization method.
	 */
	public static final String INITMETHOD_DESC = "()[Z";

	/**
	 * Access modifiers of the initialization method.
	 */
	public static final int INITMETHOD_ACC = Opcodes.ACC_PRIVATE
			| Opcodes.ACC_STATIC;

	/**
	 * Name of the interface initialization method.
	 *
	 * According to Java Virtual Machine Specification <a href=
	 * "https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-2.html#jvms-2.9-200">
	 * §2.9</a>:
	 *
	 * <blockquote>
	 * <p>
	 * A class or interface has at most one class or interface initialization
	 * method and is initialized by invoking that method. The initialization
	 * method of a class or interface has the special name {@code <clinit>},
	 * takes no arguments, and is void.
	 * </p>
	 * <p>
	 * Other methods named {@code <clinit>} in a class file are of no
	 * consequence. They are not class or interface initialization methods. They
	 * cannot be invoked by any Java Virtual Machine instruction and are never
	 * invoked by the Java Virtual Machine itself.
	 * </p>
	 * <p>
	 * In a class file whose version number is 51.0 or above, the method must
	 * additionally have its ACC_STATIC flag set in order to be the class or
	 * interface initialization method.
	 * </p>
	 * <p>
	 * This requirement was introduced in Java SE 7. In a class file whose
	 * version number is 50.0 or below, a method named {@code <clinit>} that is
	 * void and takes no arguments is considered the class or interface
	 * initialization method regardless of the setting of its ACC_STATIC flag.
	 * </p>
	 * </blockquote>
	 *
	 * And <a href=
	 * "https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.6-200-A.6">
	 * §4.6</a>:
	 *
	 * <blockquote>
	 * <p>
	 * Class and interface initialization methods are called implicitly by the
	 * Java Virtual Machine. The value of their access_flags item is ignored
	 * except for the setting of the ACC_STRICT flag.
	 * </p>
	 * </blockquote>
	 */
	static final String CLINIT_NAME = "<clinit>";

	/**
	 * Descriptor of the interface initialization method.
	 *
	 * @see #CLINIT_NAME
	 */
	static final String CLINIT_DESC = "()V";

	/**
	 * Access flags of the interface initialization method generated by JaCoCo.
	 *
	 * @see #CLINIT_NAME
	 */
	static final int CLINIT_ACC = Opcodes.ACC_STATIC;

	/**
	 * Gets major version number from given bytes of class (unsigned two bytes
	 * at offset 6).
	 *
	 * @param b
	 *            bytes of class
	 * @return major version of bytecode
	 * @see <a href=
	 *      "https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-4.html#jvms-4.1">Java
	 *      Virtual Machine Specification §4 The class File Format</a>
	 * @see #setMajorVersion(int, byte[])
	 * @see #getMajorVersion(ClassReader)
	 */
	public static int getMajorVersion(final byte[] b) {
		return ((b[6] & 0xFF) << 8) | (b[7] & 0xFF);
	}

	/**
	 * Sets major version number in given bytes of class (unsigned two bytes at
	 * offset 6).
	 *
	 * @param majorVersion
	 *            major version of bytecode to set
	 * @param b
	 *            bytes of class
	 * @see #getMajorVersion(byte[])
	 */
	public static void setMajorVersion(final int majorVersion, final byte[] b) {
		b[6] = (byte) (majorVersion >>> 8);
		b[7] = (byte) majorVersion;
	}

	/**
	 * Gets major version number from given {@link ClassReader}.
	 *
	 * @param reader
	 *            reader to get information about the class
	 * @return major version of bytecode
	 * @see ClassReader#ClassReader(byte[], int, int)
	 * @see #getMajorVersion(byte[])
	 */
	public static int getMajorVersion(final ClassReader reader) {
		// relative to the beginning of constant pool because ASM provides API
		// to construct ClassReader which reads from the middle of array
		final int firstConstantPoolEntryOffset = reader.getItem(1) - 1;
		return reader.readUnsignedShort(firstConstantPoolEntryOffset - 4);
	}

	/**
	 * Determines whether the given class file version requires stackmap frames.
	 *
	 * @param version
	 *            class file version
	 * @return <code>true</code> if frames are required
	 */
	public static boolean needsFrames(final int version) {
		// consider major version only (due to 1.1 anomaly)
		return (version & 0xFFFF) >= Opcodes.V1_6;
	}

	/**
	 * Ensures that the given member does not correspond to a internal member
	 * created by the instrumentation process. This would mean that the class is
	 * already instrumented.
	 *
	 * @param member
	 *            name of the member to check
	 * @param owner
	 *            name of the class owning the member
	 * @throws IllegalStateException
	 *             thrown if the member has the same name than the
	 *             instrumentation member
	 */
	public static void assertNotInstrumented(final String member,
			final String owner) throws IllegalStateException {
		if (member.equals(DATAFIELD_NAME) || member.equals(INITMETHOD_NAME)) {
			throw new IllegalStateException(format(
					"Cannot process instrumented class %s. Please supply original non-instrumented classes.",
					owner));
		}
	}

	/**
	 * Generates the instruction to push the given int value on the stack.
	 * Implementation taken from
	 * {@link org.objectweb.asm.commons.GeneratorAdapter#push(int)}.
	 *
	 * @param mv
	 *            visitor to emit the instruction
	 * @param value
	 *            the value to be pushed on the stack.
	 */
	public static void push(final MethodVisitor mv, final int value) {
		if (value >= -1 && value <= 5) {
			mv.visitInsn(Opcodes.ICONST_0 + value);
		} else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
			mv.visitIntInsn(Opcodes.BIPUSH, value);
		} else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
			mv.visitIntInsn(Opcodes.SIPUSH, value);
		} else {
			mv.visitLdcInsn(Integer.valueOf(value));
		}
	}

	/**
	 * Creates a {@link ClassReader} instance for given bytes of class even if
	 * its version not yet supported by ASM.
	 *
	 * @param b
	 *            bytes of class
	 * @return {@link ClassReader}
	 */
	public static ClassReader classReaderFor(final byte[] b) {
		final int originalVersion = getMajorVersion(b);
		if (originalVersion == Opcodes.V22 + 1) {
			// temporarily downgrade version to bypass check in ASM
			setMajorVersion(Opcodes.V22, b);
		}
		final ClassReader classReader = new ClassReader(b);
		setMajorVersion(originalVersion, b);
		return classReader;
	}

	/**
	 * Name of the field that stores coverage information of a class for trace.
	 */
	public static final String DATAFIELD_NAME_MAP = "$jacocoDataMap";

	/**
	 * Data type of the field that stores coverage information for a class (
	 * <code>Map&lt;String,boolean[]&gt;</code>).
	 */
	public static final String DATAFIELD_DESC_MAP = "Ljava/util/Map;";

	/**
	 * Data container of the field that stores coverage information for a class
	 * ( <code>ConcurrentHashMap&lt;String,boolean[]&gt;</code>).
	 */
	public static final String DATAFIELD_DESC_INSTANCE_IMPLEMENT = "java/util/concurrent/ConcurrentHashMap";
	/**
	 * trace data store container
	 */
	public static final String TRACE_VALUE_CLASS = "org/jacoco/core/trace/TraceValue";
	private static final Object[] FRAME_LOCALS_EMPTY = new Object[0];

	/**
	 * define data field that store execution data
	 *
	 * @param cv
	 *            class to probe
	 */
	public static void createDataFieldMap(final ClassVisitor cv) {
		cv.visitField(InstrSupport.DATAFIELD_ACC,
				InstrSupport.DATAFIELD_NAME_MAP,
				InstrSupport.DATAFIELD_DESC_MAP, null, null);
	}

	/**
	 * define data field init method
	 *
	 * @param cv
	 *            class to probe
	 * @param probeCount
	 *            probe count for this class
	 * @param classId
	 *            identifier of the class
	 * @param className
	 *            VM class name
	 * @param accessorGenerator
	 *            data access code
	 * @param withFrames
	 *            if contains frames
	 *
	 */
	public static void createInitMethodMap(final ClassVisitor cv,
			final int probeCount, final long classId, final String className,
			IExecutionDataAccessorGenerator accessorGenerator,
			boolean withFrames) {
		final MethodVisitor mv = cv.visitMethod(InstrSupport.INITMETHOD_ACC,
				InstrSupport.INITMETHOD_NAME, InstrSupport.INITMETHOD_DESC,
				null, null);
		mv.visitCode();

		// 1. get map
		mv.visitFieldInsn(Opcodes.GETSTATIC, className,
				InstrSupport.DATAFIELD_NAME_MAP,
				InstrSupport.DATAFIELD_DESC_MAP);
		mv.visitInsn(Opcodes.DUP);

		// Stack[1]: MAP
		// Stack[0]: MAP

		// 2. check map
		Label MAP_NOT_NULL = new Label();
		mv.visitJumpInsn(Opcodes.IFNONNULL, MAP_NOT_NULL); // 如果不为null，跳转到ifNotNull标签

		// Stack[0]: MAP
		mv.visitInsn(Opcodes.POP);

		// 3. map null,init
		mv.visitTypeInsn(Opcodes.NEW,
				InstrSupport.DATAFIELD_DESC_INSTANCE_IMPLEMENT);
		mv.visitInsn(Opcodes.DUP);

		// Stack[1]: ConcurrentHashMap
		// Stack[0]: ConcurrentHashMap

		mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
				InstrSupport.DATAFIELD_DESC_INSTANCE_IMPLEMENT, "<init>", "()V",
				false);

		// Stack[0]: ConcurrentHashMap

		// store HashMap instance
		mv.visitFieldInsn(Opcodes.PUTSTATIC, className,
				InstrSupport.DATAFIELD_NAME_MAP,
				InstrSupport.DATAFIELD_DESC_MAP);
		mv.visitFieldInsn(Opcodes.GETSTATIC, className,
				InstrSupport.DATAFIELD_NAME_MAP,
				InstrSupport.DATAFIELD_DESC_MAP);
		// Stack[0]: MAP
		if (withFrames) {
			mv.visitFrame(Opcodes.F_NEW, 0, FRAME_LOCALS_EMPTY, 1,
					new Object[] { "java/util/Map" });
		}

		// 4. map not null,get array
		mv.visitLabel(MAP_NOT_NULL);

		// Stack[0]: MAP

		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_VALUE_CLASS, "get",
				"()Ljava/lang/String;", false);

		// Stack[1]: LString
		// Stack[0]: MAP

		mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map", "get",
				"(Ljava/lang/Object;)Ljava/lang/Object;", true);
		mv.visitInsn(Opcodes.DUP);

		// Stack[1]: [Z
		// Stack[0]: [Z

		Label ARRAY_NOT_NULL = new Label();
		// 5. check array null
		mv.visitJumpInsn(Opcodes.IFNONNULL, ARRAY_NOT_NULL); // 如果不为null，跳转到ifNotNull标签
		mv.visitInsn(Opcodes.POP);

		final int size = accessorGenerator.generateDataAccessor(classId,
				className, probeCount, mv);

		// Stack[0]: [Z

		mv.visitFieldInsn(Opcodes.GETSTATIC, className,
				InstrSupport.DATAFIELD_NAME_MAP,
				InstrSupport.DATAFIELD_DESC_MAP);
		mv.visitInsn(Opcodes.SWAP);
		mv.visitInsn(Opcodes.DUP_X1);

		// Stack[2]: [Z
		// Stack[1]: MAP
		// Stack[0]: [Z

		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_VALUE_CLASS, "get",
				"()Ljava/lang/String;", false);
		mv.visitInsn(Opcodes.SWAP);

		// Stack[3]: [Z
		// Stack[2]: LString
		// Stack[1]: MAP
		// Stack[0]: [Z

		mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map", "put",
				"(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
				true);
		mv.visitInsn(Opcodes.POP);

		// Stack[0]: [Z

		if (withFrames) {
			mv.visitFrame(Opcodes.F_NEW, 0, FRAME_LOCALS_EMPTY, 1,
					new Object[] { "java/lang/Object" });
		}

		// 7. array not null return
		mv.visitLabel(ARRAY_NOT_NULL);

		// Stack[0]: [Z
		mv.visitTypeInsn(Opcodes.CHECKCAST, InstrSupport.DATAFIELD_DESC);
		mv.visitInsn(Opcodes.ARETURN);
		mv.visitMaxs(Math.max(size, 2), 0); // Maximum local stack size is 2
		mv.visitEnd();
	}

	/**
	 * test
	 *
	 * @param mv
	 *            method to probe
	 */
	public static void showTraceId(MethodVisitor mv) {
		// bba define
		mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out",
				"Ljava/io/PrintStream;");
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_VALUE_CLASS, "get",
				"()Ljava" + "/lang/String;", false);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream",
				"println", "(Ljava/lang/String;)V", false);
	}

	/**
	 * test
	 *
	 * @param mv
	 *            method to probe
	 */
	public static void showArray(MethodVisitor mv) {
		// bba define
		mv.visitInsn(Opcodes.DUP);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "toString",
				"([Z)" + "Ljava/lang/String;", false);
		mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out",
				"Ljava/io/PrintStream;");
		mv.visitInsn(Opcodes.SWAP);
		// Stack[0]: [Z
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream",
				"println", "(Ljava/lang/String;)V", false);
	}
}
