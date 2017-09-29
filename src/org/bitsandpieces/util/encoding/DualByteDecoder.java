/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.encoding;

import java.io.UncheckedIOException;

// exactly one or two bytes map to exactly one code point
// decoded code points must be neither supplementary nor surrogates (single char only)
abstract class DualByteDecoder extends AbstractDecoder {

	private static final int NONE = 0;			// never a lead byte
	static final char LEAD_B = 0xFFFE;	// lead byte identifier
	static final char NO_DEF = 0xFFFF;			// unmapped

	private final char[] t0, t1;
	private final int tableOffset;

	private int lead = NONE;

	DualByteDecoder(char[] t0, char[] t1, int tableOffset) {
		this.t0 = t0;
		this.t1 = t1;
		this.tableOffset = tableOffset;
	}

	@Override
	final int _decode(byte[] src, Appendable dest, int maxChars, int maxCodePoints, int _offset, int _limit) {
		try {
			int _offsetOld = _offset;
			int y = 0;
			int p = this.lead;
			if (p != NONE) {
				if (_offset == _limit) {
					return 0;
				}
				int i = ((p << 8) | (src[_offset] & 0xff)) - this.tableOffset;
				this.lead = NONE;
				if (i < 0) {
					return -1;	// unmappable, lead is at fault
				}
				char c = this.t1[i];
				if (c == NO_DEF) {
					return -1;	// unmappable, lead is at fault
				}
				_offset++;	// consume continuation byte
				dest.append(c);
				y++;
			}
			for (int m = Math.min(maxChars, maxCodePoints); _offset < _limit && y < m;) {
				int b = src[_offset++] & 0xff;
				char c0 = this.t0[b];
				if (c0 != NO_DEF) {
					// provisionally mappable
					if (c0 != LEAD_B) {
						// single byte mapping
						dest.append(c0);
						y++;
						continue;
					}
					// dual byte mapping
					if (_offset == _limit) {
						// out of input
						this.lead = b;
						break;
					}
					int i = ((b << 8) | (src[_offset] & 0xff)) - this.tableOffset;
					if (i >= 0) {
						char c1 = this.t1[i];
						if (c1 != NO_DEF) {
							_offset++;
							dest.append(c1);
							y++;
							continue;
						}
					}
					// unmappable
				}
				// unmappable
				if (y == 0) {
					this.bytes += (_offset - _offsetOld);
					this.offset = _offset;
					return -1;
				}
				this.pendingError = -1;
				break;
			}
			this.bytes += (_offset - _offsetOld);
			this.offset = _offset;
			this.chars += y;
			return y;
		} catch (java.io.IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	@Override
	final int _decode(byte[] src, char[] dest, int off, int maxChars, int maxCodePoints, int _offset, int _limit) {
		int _offsetOld = _offset;
		int y = off;
		int p = this.lead;
		if (p != NONE) {
			if (_offset == _limit) {
				return 0;
			}
			int i = ((p << 8) | (src[_offset] & 0xff)) - this.tableOffset;
			this.lead = NONE;
			if (i < 0) {
				return -1;	// unmappable, lead is at fault
			}
			char c = this.t1[i];
			if (c == NO_DEF) {
				return -1;	// unmappable, lead is at fault
			}
			_offset++;	// consume continuation byte
			dest[y++] = c;
		}
		for (int m = off + Math.min(maxChars, maxCodePoints); _offset < _limit && y < m;) {
			int b = src[_offset++] & 0xff;
			char c0 = this.t0[b];
			if (c0 != NO_DEF) {
				// provisionally mappable
				if (c0 != LEAD_B) {
					// single byte mapping
					dest[y++] = c0;
					continue;
				}
				// dual byte mapping
				if (_offset == _limit) {
					// out of input
					this.lead = b;
					break;
				}
				int i = ((b << 8) | (src[_offset] & 0xff)) - this.tableOffset;
				if (i >= 0) {
					char c1 = this.t1[i];
					if (c1 != NO_DEF) {
						_offset++;
						dest[y++] = c1;
						continue;
					}
				}
				// unmappable
			}
			// unmappable
			if (y == off) {
				this.bytes += (_offset - _offsetOld);
				this.offset = _offset;
				return -1;
			}
			this.pendingError = -1;
			break;
		}
		this.bytes += (_offset - _offsetOld);
		this.offset = _offset;
		int n = y - off;
		this.chars += n;
		return n;
	}

	@Override
	public final int needsInput() {
		return this.lead != NONE ? 1 : 0;
	}

	@Override
	public final int pendingInput() {
		return this.lead != NONE ? 1 : 0;
	}

	@Override
	public final int pendingOutput() {
		return 0;
	}

	@Override
	public boolean hasPending() {
		return this.lead != NONE;
	}

	@Override
	public final long codePointsResolved() {
		// each produced char is a valid code point
		return this.chars;
	}

	@Override
	public final Decoder dropPending() {
		this.bytes -= pendingInput();
		this.lead = NONE;
		return this;
	}

	@Override
	public final Decoder reset() {
		_reset();
		this.lead = NONE;
		return dropInput();
	}
}
