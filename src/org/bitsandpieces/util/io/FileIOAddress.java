/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.io;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.EnumSet;
import java.util.Set;

/**
 * Address for a physical file on disk. If it doesn't exist, opening it will
 * attempt to create it.
 *
 * @author Jan Kebernik
 */
// backed FileChannel
public class FileIOAddress implements IOAddress {

	private final Path file;

	public FileIOAddress(String file) {
		if (file == null) {
			throw new NullPointerException();
		}
		this.file = Paths.get(file);
	}

	public FileIOAddress(File file) {
		if (file == null) {
			throw new NullPointerException();
		}
		this.file = file.toPath();
	}

	public FileIOAddress(Path file) {
		if (file == null) {
			throw new NullPointerException();
		}
		this.file = file;
	}

	@Override
	public IOBuffer open() throws IOException {
		return new FileIOSource(this, this.file).buffer();
	}

	@Override
	public IOBuffer open(int bufferSize) throws IOException {
		return new FileIOSource(this, this.file).buffer(bufferSize);
	}

	static final class FileIOSource implements IOSource {

		private static final Set<StandardOpenOption> OPTIONS = EnumSet.of(
				StandardOpenOption.READ,
				StandardOpenOption.WRITE,
				StandardOpenOption.CREATE);
		private static final FileAttribute<?>[] NO_ATTR = {};

		private static final ByteBuffer EMPTY = ByteBuffer.wrap(new byte[0]);

		final FileChannel fc;
		private final IOAddress address;

		private ByteBuffer byteBuffer;

		FileIOSource(IOAddress address, Path file) throws IOException {
			try {
				// neat, but not really necessary
				// Path parent = file.getParent();
				// if (parent == null) {
				// 	throw new NullPointerException("Path has no root.");
				// }
				// Files.createDirectories(parent);
				this.fc = FileChannel.open(file, OPTIONS, NO_ATTR);
			} catch (java.io.IOException ex) {
				throw new IOException(ex);
			}
			this.byteBuffer = EMPTY;
			this.address = address;
		}

		final ByteBuffer wrap(byte[] buf, int off, int len) {
			ByteBuffer bb = this.byteBuffer;
			if (bb.array() == buf) {
				// order matters
				bb.limit(off + len);
				bb.position(off);
				return bb;
			}
			return (this.byteBuffer = ByteBuffer.wrap(buf, off, len));
		}

		@Override
		public IOAddress address() {
			return this.address;
		}

		@Override
		public long size() throws IOException {
			try {
				return this.fc.size();
			} catch (java.io.IOException ex) {
				throw new IOException(ex);
			}
		}

		@Override
		public int read(long pos, byte[] buf, int off, int len) throws IOException {
			try {
				return this.fc.read(wrap(buf, off, len), pos);
			} catch (java.io.IOException ex) {
				throw new IOException(ex);
			}
		}

		@Override
		public void write(long pos, byte[] buf, int off, int len) throws IOException {
			try {
				if (this.fc.write(wrap(buf, off, len), pos) != len) {
					throw new IOException("Unexpected number of bytes written.");
				}
			} catch (java.io.IOException ex) {
				throw new IOException(ex);
			}
		}

		@Override
		public void truncate(long size) throws IOException {
			try {
				this.fc.truncate(size);
			} catch (java.io.IOException ex) {
				throw new IOException(ex);
			}
		}

		@Override
		public void close() throws IOException {
			try {
				this.fc.close();
			} catch (java.io.IOException ex) {
				throw new IOException(ex);
			}
		}
	}
}
