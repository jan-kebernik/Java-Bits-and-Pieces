/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.encoding;

import java.util.Objects;

/**
 *
 * @author pp
 */
abstract class AbstractEncoder implements Encoder {

	private static final char[] EMPTY = {};

	private char[] inArr = EMPTY;
	private CharSequence inSeq;

	protected int offset, limit;

	protected int statePending;	// 0 or ERROR

	abstract int _encode(char[] src, byte[] buf, int off, int len);

	abstract int _encode(CharSequence src, byte[] buf, int off, int len);
	
	private int _encode(byte[] buf, int off, int len) {
		int s = this.statePending;
		if (s != 0) {
			this.statePending = 0;
			return s;
		}
		if (len == 0) {
			return 0;
		}
		char[] in = this.inArr;
		return in != null 
				? _encode(in, buf, off, len) 
				: _encode(this.inSeq, buf, off, len);
	}

	@Override
	public final int encode(byte[] buf) {
		Objects.requireNonNull(buf);
		return _encode(buf, 0, buf.length);
	}

	@Override
	public final int encode(byte[] buf, int off, int len) {
		Objects.requireNonNull(buf);
		if (off < 0 || len < 0 || off > buf.length - len) {
			throw new IndexOutOfBoundsException();
		}
		return _encode(buf, off, len);
	}

	@Override
	public final Encoder feedInput(CharSequence src) {
		Objects.requireNonNull(src);
		if (this.offset != this.limit) {
			throw new IllegalStateException("Current input not fully processed.");
		}
		this.inArr = null;
		this.inSeq = src;
		this.offset = 0;
		this.limit = src.length();
		return this;
	}

	@Override
	public final Encoder feedInput(CharSequence src, int off, int len) {
		Objects.requireNonNull(src);
		if (off < 0 || len < 0 || off > src.length() - len) {
			throw new IndexOutOfBoundsException();
		}
		if (this.offset != this.limit) {
			throw new IllegalStateException("Current input not fully processed.");
		}
		this.inArr = null;
		this.inSeq = src;
		this.offset = off;
		this.limit = off + len;
		return this;
	}

	@Override
	public final Encoder feedInput(char[] src) {
		Objects.requireNonNull(src);
		if (this.offset != this.limit) {
			throw new IllegalStateException("Current input not fully processed.");
		}
		this.inArr = src;
		this.inSeq = null;
		this.offset = 0;
		this.limit = src.length;
		return this;
	}

	@Override
	public final Encoder feedInput(char[] src, int off, int len) {
		Objects.requireNonNull(src);
		if (off < 0 || len < 0 || off > src.length - len) {
			throw new IndexOutOfBoundsException();
		}
		if (this.offset != this.limit) {
			throw new IllegalStateException("Current input not fully processed.");
		}
		this.inArr = src;
		this.inSeq = null;
		this.offset = off;
		this.limit = off + len;
		return this;
	}
}
