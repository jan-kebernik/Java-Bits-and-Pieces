/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.encoding;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.bitsandpieces.util.ArrayUtil;

/**
 *
 * @author pp
 */
abstract class DualByteDecoder extends AbstractDecoder {

	private static final int NONE = 0;	// never a lead byte
	static final char LEAD_B = 0xFFFE;	// DBCS LEAD BYTE
	static final char NO_DEF = 0xFFFF;	// UNDEFINED

	private int leadPending = NONE;

	// maps from char to 1 or 2 bytes (stored as a char)
	// entries without a mapping == NO_DEF.
	// in case the same char maps to different bytes, 
	// only the first such mapping is used
	static final char[] buildConversionTable(char[] t0, char[] t1, int off) {
		char[] t = ArrayUtil.fill(new char[65536], NO_DEF);
		for (int i = 0; i < 256; i++) {
			char c = t0[i];
			if (c != NO_DEF && c != LEAD_B) {
				if (t[c] == NO_DEF) {
					// not yet mapped
					t[c] = (char) i;
				}
			}
		}
		for (int i = 0; i < t1.length; i++) {
			char c = t1[i];	// cannot be lead byte
			if (c != NO_DEF) {
				if (t[c] == NO_DEF) {
					// not yet mapped
					t[c] = (char) (i + off);
				}
			}
		}
		return t;
	}

	static final int copyTable(char[] src, char[] dest, int off) {
		System.arraycopy(src, 0, dest, off, src.length);
		return src.length;
	}

	abstract int tableOffset();

	abstract char getT0(int i);

	abstract char getT1(int i);

	@Override
	int _decode(byte[] src, char[] dest, int off, int len) {
		int _offset = this.offset;
		int _limit = this.limit;
		if (_offset == _limit) {
			return UNDERFLOW;
		}
		int y = off;
		int p = this.leadPending;
		if (p != NONE) {
			int i = ((p << 8) | (src[_offset] & 0xff)) - this.tableOffset();
			this.leadPending = NONE;
			if (i < 0) {
				this.offset = _offset;
				return ERROR;
			}
			char c = this.getT1(i);
			if (c == NO_DEF) {
				this.offset = _offset;
				return ERROR;
			}
			_offset++;	// only consume on successful resolution
			dest[y++] = c;
		}
		for (int m = off + len; _offset < _limit && y < m;) {
			int b = src[_offset++] & 0xff;
			char c0 = this.getT0(b);
			if (c0 != NO_DEF) {
				if (c0 != LEAD_B) {
					dest[y++] = c0;
					continue;
				}
				if (_offset == _limit) {
					this.leadPending = b;
					if (y == off) {
						return UNDERFLOW;
					}
					break;
				}
				int i = ((b << 8) | (src[_offset] & 0xff)) - this.tableOffset();
				if (i >= 0) {
					char c1 = this.getT1(i);
					if (c1 != NO_DEF) {
						_offset++;	// only consume on successful resolution
						dest[y++] = c1;
						continue;
					}
				}
			}
			if (y == off) {
				this.offset = _offset;
				return ERROR;
			}
			this.statePending = ERROR;
			break;
		}
		this.offset = _offset;
		return y - off;
	}

	@Override
	int _decode(byte[] src, Appendable dest, int len) throws UncheckedIOException {
		int _offset = this.offset;
		try {
			int _limit = this.limit;
			if (_offset == _limit) {
				return UNDERFLOW;
			}
			int y = 0;
			int p = this.leadPending;
			if (p != NONE) {
				int i = ((p << 8) | (src[_offset] & 0xff)) - this.tableOffset();
				this.leadPending = NONE;
				if (i < 0) {
					return ERROR;
				}
				char c = this.getT1(i);
				if (c == NO_DEF) {
					return ERROR;
				}
				_offset++;	// only consume on successful resolution
				dest.append(c);
			}
			for (; _offset < _limit && y < len;) {
				int b = src[_offset++] & 0xff;
				char c0 = this.getT0(b);
				if (c0 != NO_DEF) {
					if (c0 != LEAD_B) {
						dest.append(c0);
						continue;
					}
					if (_offset == _limit) {
						this.leadPending = b;
						if (y == 0) {
							return UNDERFLOW;
						}
						break;
					}
					int i = ((b << 8) | (src[_offset] & 0xff)) - this.tableOffset();
					if (i >= 0) {
						char c1 = this.getT1(i);
						if (c1 != NO_DEF) {
							_offset++;	// only consume on successful resolution
							dest.append(c1);
							continue;
						}
					}
				}
				if (y == 0) {
					return ERROR;
				}
				this.statePending = ERROR;
				break;
			}
			return y;
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		} finally {
			this.offset = _offset;
		}
	}

	@Override
	public int pendingOutput() {
		return 0;
	}
	
	@Override
	public Decoder reset() {
		super.reset();
		this.leadPending = NONE;
		return this;
	}
}
