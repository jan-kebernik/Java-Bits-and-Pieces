/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.encoding;

/**
 *
 * @author pp
 */
abstract class DualByteEncoder extends AbstractEncoder {

	private static final char NONE = 0;

	private int bytePending;

	private char surr = NONE;

	abstract char get(int i);

	@Override
	int _encode(char[] src, byte[] buf, int off, int len) {
		int _offset = this.offset;
		int _limit = this.limit;
		char hs = this.surr;
		if (hs != NONE) {
			// have a high surrogate waiting
			if (_offset == _limit) {
				return UNDERFLOW;
			}
			this.surr = NONE;
			if (Character.isLowSurrogate(src[_offset])) {
				// two-char error
				this.offset = ++_offset;
			}
			return ERROR;
		}
		int y = off;
		int m = off + len;
		int p = this.bytePending;
		if (p != 0) {
			buf[y++] = (byte) (p & 0xff);
			this.bytePending = 0;
		}
		for (; _offset < _limit && y < m;) {
			char c = src[_offset++];
			char r = get(c);
			if (r == DualByteDecoder.NO_DEF) {
				// unmappable
				if (Character.isHighSurrogate(c)) {
					if (_offset == _limit) {
						this.surr = c;
						this.offset = _offset;
						if (y == off) {
							return UNDERFLOW;
						}
						return y - off;
					}
					if (Character.isLowSurrogate(src[_offset])) {
						// two-char error
						_offset++;
					}
				}
				this.offset = _offset;	// update internal state
				if (y == off) {
					// no bytes produced yet
					return ERROR;
				}
				this.statePending = ERROR;
				return y - off;
			}
			int lead = r >>> 8;
			if (lead == 0) {
				// one byte
				buf[y++] = (byte) r;
				continue;
			}
			// two bytes
			int trail = r & 0xff;
			buf[y++] = (byte) lead;
			if (y == m) {
				this.offset = _offset;	// update internal state
				this.bytePending = 0x80000000 | trail;
				return y - off;
			}
			buf[y++] = (byte) trail;
		}
		this.offset = _offset;	// update internal state
		if (_offset == _limit && y == off) {
			return UNDERFLOW;
		}
		return y - off;
	}

	@Override
	int _encode(CharSequence src, byte[] buf, int off, int len) {
		int _offset = this.offset;
		int _limit = this.limit;
		char hs = this.surr;
		if (hs != NONE) {
			// have a high surrogate waiting
			if (_offset == _limit) {
				return UNDERFLOW;
			}
			this.surr = NONE;
			if (Character.isLowSurrogate(src.charAt(_offset))) {
				// two-char error
				this.offset = ++_offset;
			}
			return ERROR;
		}
		int y = off;
		int m = off + len;
		int p = this.bytePending;
		if (p != 0) {
			buf[y++] = (byte) (p & 0xff);
			this.bytePending = 0;
		}
		for (; _offset < _limit && y < m;) {
			char c = src.charAt(_offset++);
			char r = get(c);
			if (r == DualByteDecoder.NO_DEF) {
				// unmappable
				if (Character.isHighSurrogate(c)) {
					if (_offset == _limit) {
						this.surr = c;
						this.offset = _offset;
						if (y == off) {
							return UNDERFLOW;
						}
						return y - off;
					}
					if (Character.isLowSurrogate(src.charAt(_offset))) {
						// two-char error
						_offset++;
					}
				}
				this.offset = _offset;	// update internal state
				if (y == off) {
					// no bytes produced yet
					return ERROR;
				}
				this.statePending = ERROR;
				return y - off;
			}
			int lead = r >>> 8;
			if (lead == 0) {
				// one byte
				buf[y++] = (byte) r;
				continue;
			}
			// two bytes
			int trail = r & 0xff;
			buf[y++] = (byte) lead;
			if (y == m) {
				this.offset = _offset;	// update internal state
				this.bytePending = 0x80000000 | trail;
				return y - off;
			}
			buf[y++] = (byte) trail;
		}
		this.offset = _offset;	// update internal state
		if (_offset == _limit && y == off) {
			return UNDERFLOW;
		}
		return y - off;
	}

	@Override
	public int pendingOutput() {
		// always 0 or 1 byte is pending
		return this.bytePending >>> 31;
	}
}
