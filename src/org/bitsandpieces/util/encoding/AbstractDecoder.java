/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.encoding;

import java.io.UncheckedIOException;
import java.util.Objects;

/**
 *
 * @author pp
 */
abstract class AbstractDecoder implements Decoder {

	private static final byte[] EMPTY = {};

	private byte[] src = EMPTY;

	protected int offset, limit;
	protected int statePending;
	protected long codePoints, bytes;

	abstract int _decode(byte[] src, char[] dest, int off, int len, int numCodePoints);

	abstract int _decode(byte[] src, Appendable dest, int len, int numCodePoints) throws UncheckedIOException;

	private int _decode(char[] dest, int off, int len, int numCodePoints) {
		int s = this.statePending;
		if (s != 0) {
			this.statePending = 0;
			return s;
		}
		if (len == 0 || numCodePoints == 0) {
			return len;
		}
		return _decode(this.src, dest, off, len, numCodePoints);
	}

	private int _decode(Appendable dest, int len, int numCodePoints) throws UncheckedIOException {
		int s = this.statePending;
		if (s != 0) {
			this.statePending = 0;
			return s;
		}
		if (len == 0 || numCodePoints == 0) {
			return len;
		}
		return _decode(this.src, dest, len, numCodePoints);
	}

	@Override
	public final int decode(char[] dest) {
		Objects.requireNonNull(dest);
		return _decode(dest, 0, dest.length, Integer.MAX_VALUE);
	}

	@Override
	public final int decode(Appendable dest) throws UncheckedIOException {
		Objects.requireNonNull(dest);
		return _decode(dest, Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	@Override
	public final int decode(char[] dest, int off, int len, int numCodePoints) {
		Objects.requireNonNull(dest);
		if (off < 0 || len < 0 || off > dest.length - len) {
			throw new IndexOutOfBoundsException();
		}
		if (numCodePoints < 0) {
			throw new IllegalArgumentException("numCodePoints < 0:" + numCodePoints);
		}
		return _decode(dest, off, len, numCodePoints);
	}

	@Override
	public final int decode(Appendable dest, int len, int numCodePoints) throws UncheckedIOException {
		Objects.requireNonNull(dest);
		if (len < 0) {
			throw new IllegalArgumentException("len < 0:" + len);
		}
		if (numCodePoints < 0) {
			throw new IllegalArgumentException("numCodePoints < 0:" + numCodePoints);
		}
		return _decode(dest, len, numCodePoints);
	}

	@Override
	public final Decoder feedInput(byte[] src) {
		Objects.requireNonNull(src);
		if (this.offset != this.limit) {
			throw new IllegalStateException("Current input not fully processed.");
		}
		this.src = src;
		this.offset = 0;
		this.limit = src.length;
		return this;
	}

	@Override
	public final Decoder feedInput(byte[] src, int off, int len) {
		Objects.requireNonNull(src);
		if (off < 0 || len < 0 || off > src.length - len) {
			throw new IndexOutOfBoundsException();
		}
		if (this.offset != this.limit) {
			throw new IllegalStateException("Current input not fully processed.");
		}
		this.src = src;
		this.offset = off;
		this.limit = off + len;
		return this;
	}

	@Override
	public Decoder reset() {
		dropInput();
		this.statePending = 0;
		this.codePoints = 0L;
		return this;
	}

	@Override
	public final int inputRemaining() {
		return this.limit - this.offset;
	}

	@Override
	public final long codePoints() {
		return this.codePoints;
	}

	@Override
	public long bytes() {
		return this.bytes;
	}
	
	@Override
	public final Decoder dropInput() {
		this.src = EMPTY;
		this.offset = this.limit = 0;
		return this;
	}
}
