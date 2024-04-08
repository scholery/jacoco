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
package org.jacoco.core.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.nio.ByteBuffer;

import org.jacoco.core.internal.data.CompactDataOutput;

/**
 * Serialization of execution data into binary streams.
 */
public class ExecutionDataWriter
		implements ISessionInfoVisitor, IExecutionDataVisitor {

	/**
	 * File format version, will be incremented for each incompatible change.
	 */
	public static final char FORMAT_VERSION;

	static {
		// Runtime initialize to ensure javac does not inline the value.
		FORMAT_VERSION = 0x1007;
	}

	/** Magic number in header for file format identification. */
	public static final char MAGIC_NUMBER = 0xC0C0;

	/** Block identifier for file headers. */
	public static final byte BLOCK_HEADER = 0x01;

	/** Block identifier for session information. */
	public static final byte BLOCK_SESSIONINFO = 0x10;

	/** Block identifier for execution data of a single class. */
	public static final byte BLOCK_EXECUTIONDATA = 0x11;

	/** Underlying data output */
	protected final CompactDataOutput out;

	/**
	 * Creates a new writer based on the given output stream. Depending on the
	 * nature of the underlying stream output should be buffered as most data is
	 * written in single bytes.
	 *
	 * @param output
	 *            binary stream to write execution data to
	 * @throws IOException
	 *             if the header can't be written
	 */
	public ExecutionDataWriter(final OutputStream output) throws IOException {
		this.out = new CompactDataOutput(output);
		writeHeader();
	}

	/**
	 * Writes an file header to identify the stream and its protocol version.
	 *
	 * @throws IOException
	 *             if the header can't be written
	 */
	private void writeHeader() throws IOException {
		// out.writeByte(BLOCK_HEADER);
		// out.writeChar(MAGIC_NUMBER);
		// out.writeChar(FORMAT_VERSION);

		ByteBuffer buffer = ByteBuffer.allocate(1024);
		buffer.put(BLOCK_HEADER);
		buffer.putChar(MAGIC_NUMBER);
		buffer.putChar(FORMAT_VERSION);
		out.write(
				ByteBuffer.wrap(buffer.array(), 0, buffer.position()).array());
	}

	/**
	 * Flushes the underlying stream.
	 *
	 * @throws IOException
	 *             if the underlying stream can't be flushed
	 */
	public void flush() throws IOException {
		out.flush();
	}

	public void visitSessionInfo(final SessionInfo info) {
		try {
			// out.writeByte(BLOCK_SESSIONINFO);
			// out.writeUTF(info.getId());
			// out.writeLong(info.getStartTimeStamp());
			// out.writeLong(info.getDumpTimeStamp());

			ByteBuffer buffer = ByteBuffer.allocate(1024);
			buffer.put(BLOCK_SESSIONINFO);
			writeUTF8(buffer, info.getId());
			buffer.putLong(info.getStartTimeStamp());
			buffer.putLong(info.getDumpTimeStamp());
			out.write(ByteBuffer.wrap(buffer.array(), 0, buffer.position())
					.array());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void visitClassExecution(final ExecutionData data) {
		if (data.hasHits()) {
			try {
				// out.writeByte(BLOCK_EXECUTIONDATA);
				// out.writeLong(data.getId());
				// out.writeUTF(data.getName());
				// out.writeUTF(data.getTraceId());
				// out.writeBooleanArray(data.getProbes());

				ByteBuffer buffer = ByteBuffer.allocate(1024);
				buffer.put(BLOCK_EXECUTIONDATA);
				buffer.putLong(data.getId());
				writeUTF8(buffer, data.getName());
				writeUTF8(buffer, data.getTraceId());
				writeBooleanArray(buffer, data.getProbes());
				out.write(ByteBuffer.wrap(buffer.array(), 0, buffer.position())
						.array());
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Returns the first bytes of a file that represents a valid execution data
	 * file. In any case every execution data file starts with the three bytes
	 * <code>0x01 0xC0 0xC0</code>.
	 *
	 * @return first bytes of a execution data file
	 */
	public static final byte[] getFileHeader() {
		final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try {
			new ExecutionDataWriter(buffer);
		} catch (final IOException e) {
			// Must not happen with ByteArrayOutputStream
			throw new AssertionError(e);
		}
		return buffer.toByteArray();
	}

	public int writeUTF8(final ByteBuffer buffer, String str)
			throws IOException {
		int strlen = null == str ? 0 : str.length();
		int utflen = 0;
		int c, count = 0;

		/* use charAt instead of copying String to char array */
		for (int i = 0; i < strlen; i++) {
			c = str.charAt(i);
			if ((c >= 0x0001) && (c <= 0x007F)) {
				utflen++;
			} else if (c > 0x07FF) {
				utflen += 3;
			} else {
				utflen += 2;
			}
		}

		if (utflen > 65535)
			throw new UTFDataFormatException(
					"encoded string too long: " + utflen + " bytes");

		byte[] bytearr = new byte[utflen + 2];

		bytearr[count++] = (byte) ((utflen >>> 8) & 0xFF);
		bytearr[count++] = (byte) ((utflen >>> 0) & 0xFF);

		int i = 0;
		for (i = 0; i < strlen; i++) {
			c = str.charAt(i);
			if (!((c >= 0x0001) && (c <= 0x007F)))
				break;
			bytearr[count++] = (byte) c;
		}

		for (; i < strlen; i++) {
			c = str.charAt(i);
			if ((c >= 0x0001) && (c <= 0x007F)) {
				bytearr[count++] = (byte) c;

			} else if (c > 0x07FF) {
				bytearr[count++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
				bytearr[count++] = (byte) (0x80 | ((c >> 6) & 0x3F));
				bytearr[count++] = (byte) (0x80 | ((c >> 0) & 0x3F));
			} else {
				bytearr[count++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
				bytearr[count++] = (byte) (0x80 | ((c >> 0) & 0x3F));
			}
		}
		buffer.put(bytearr, 0, utflen + 2);
		return utflen + 2;
	}

	public void writeBooleanArray(final ByteBuffer out, final boolean[] value)
			throws IOException {
		writeVarInt(out, value.length);
		int buffer = 0;
		int bufferSize = 0;
		for (final boolean b : value) {
			if (b) {
				buffer |= 0x01 << bufferSize;
			}
			if (++bufferSize == 8) {
				out.putInt(buffer);
				buffer = 0;
				bufferSize = 0;
			}
		}
		if (bufferSize > 0) {
			out.putInt(buffer);
		}
	}

	public void writeVarInt(final ByteBuffer buffer, final int value)
			throws IOException {
		if ((value & 0xFFFFFF80) == 0) {
			buffer.putInt(value);
		} else {
			buffer.putInt(0x80 | (value & 0x7F));
			writeVarInt(buffer, value >>> 7);
		}
	}
}
