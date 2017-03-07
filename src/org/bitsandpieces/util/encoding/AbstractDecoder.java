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

	abstract int _decode(byte[] src, char[] dest, int off, int len);

	abstract int _decode(byte[] src, Appendable dest, int len) throws UncheckedIOException;

	private int _decode(char[] dest, int off, int len) {
		int s = this.statePending;
		if (s != 0) {
			this.statePending = 0;
			return s;
		}
		if (len == 0) {
			return len;
		}
		return _decode(this.src, dest, off, len);
	}

	private int _decode(Appendable dest, int len) throws UncheckedIOException {
		int s = this.statePending;
		if (s != 0) {
			this.statePending = 0;
			return s;
		}
		if (len == 0) {
			return len;
		}
		return _decode(this.src, dest, len);
	}

	@Override
	public final int decode(char[] dest) {
		Objects.requireNonNull(dest);
		return _decode(dest, 0, dest.length);
	}

	@Override
	public final int decode(char[] dest, int off, int len) {
		Objects.requireNonNull(dest);
		if (off < 0 || len < 0 || off > dest.length - len) {
			throw new IndexOutOfBoundsException();
		}
		return _decode(dest, off, len);
	}

	@Override
	public final int decode(Appendable dest) throws UncheckedIOException {
		Objects.requireNonNull(dest);
		return _decode(dest, Integer.MAX_VALUE);
	}

	@Override
	public final int decode(Appendable dest, int len) throws UncheckedIOException {
		Objects.requireNonNull(dest);
		if (len < 0) {
			throw new IllegalArgumentException("len < 0:" + len);
		}
		return _decode(dest, len);
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
		this.src = EMPTY;
		this.offset = this.limit = 0;
		this.statePending = 0;
		return this;
	}
}
