/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.encoding;

/**
 *
 * @author Jan Kebernik
 */
final class DecoderUTF_8 extends AbstractDecoder {

	private static final int ONE = 0, TWO = 1, THREE = 2, FOUR = 3;
	private static final int CNT_EXP = 4, CNT_SHIFT = 2, CNT_MASK = 3;

	private int state;		// used for partial decoding
	private int byte1;		// used for partial decoding
	private int byte2;		// used for partial decoding
	private int byte3;		// used for partial decoding

	private int chars;	// num chars produced so far
	private long charsTotal;

	public DecoderUTF_8() {
		super(16);
	}

	private void put(int index, char code) {
		preparePut(index)[index] = code;
	}

	private void put(int index0, int index1, char code0, char code1) {
		@SuppressWarnings("MismatchedReadAndWriteOfArray")
		char[] a = preparePut(index1);
		a[index0] = code0;
		a[index1] = code1;
	}

	private void inc(int index, int len) {
		inc(index, len, CNT_EXP, CNT_SHIFT);
	}

	private static int dec(int[] a, int index) {
		return dec(a, index, CNT_EXP, CNT_SHIFT, CNT_MASK);
	}

	/* Legal UTF-8 Byte Sequences
	 *
	 * #    Code Points      Bits   Bit/Byte pattern
	 * 1                     7   s0 0xxxxxxx										1 char
	 *      U+0000..U+007F          00..7F
	 *
	 * 2                     11  s1 110xxxxx    10xxxxxx							1 char
	 *      U+0080..U+07FF          C2..DF      80..BF
	 *
	 * 3                     16  s2 1110xxxx s3 10xxxxxx    10xxxxxx				1 char
	 *      U+0800..U+0FFF          E0          A0..BF      80..BF
	 *      U+1000..U+FFFF          E1..EF      80..BF      80..BF
	 *
	 * 4                     21  s4 11110xxx s5 10xxxxxx s6 10xxxxxx    10xxxxxx	2 chars
	 *     U+10000..U+3FFFF         F0          90..BF      80..BF      80..BF
	 *     U+40000..U+FFFFF         F1..F3      80..BF      80..BF      80..BF
	 *    U+100000..U10FFFF         F4          80..8F      80..BF      80..BF
	 *
	 */
	@Override
	public long decode(byte[] buf, int off, int len) {
		if (off < 0 || len < 0 || off > buf.length - len) {
			throw new IndexOutOfBoundsException();
		}
		// make local snapshots for speed
		int _chars = this.chars;
		int _codePoints = this.codePoints;
		long _bytes = this.bytes;
		long _bytesTotal = this.bytesTotal;
		int s = this.state;
		int offOld = off;
		int charsOld = _chars;
		int codePointsOld = _codePoints;
		try {
			// make local snapshots for speed
			ErrorHandler _err = this.err;
			Parser _stop = this.stop;
			//int opLim = this.outputLimit;
			long _inputLimit = this.inputLimit;
			// limit to smaller of len and remaining limit
			int p = _inputLimit < 0L ? len : (int) Math.min(len, _inputLimit - _bytesTotal);
			int m = off + p;
			int r;	// amount of roll-back
			rollback:
			{
				//if (opLim >= 0 && _codePoints >= opLim) {
				//	// output limit reached
				//	return 0L;
				//}
				// handle partial code point
				switch (s) {
					case 0: {
						// no partial code exists
						break;
					}
					case 1: {
						// have 110xxxxx
						// need 10xxxxxx
						if (off < m) {
							int b2 = buf[off++];
							s = 0;					// state resolves either way
							if ((b2 & 0xc0) != 0x80) {
								put(_chars++, DUMMY);
								inc(_codePoints++, TWO);	// two bytes (malformed)
								r = _err.apply(_codePoints);
							} else {
								int code = this.byte1 ^ b2 ^ 0x00000f80;
								put(_chars++, (char) code);
								inc(_codePoints++, TWO);	// two bytes
								r = _stop.accept(code, _codePoints);
							}
							if (r >= 0) {
								break rollback;		// GOTO roll back
							}
						}
						break;	// no roll-back
					}
					case 2: {
						// have 1110xxxx
						// need 10xxxxxx
						// need 10xxxxxx
						if (off >= m) {
							break;
						}
						int b2 = buf[off++];
						if ((b2 & 0xc0) != 0x80) {
							put(_chars++, DUMMY);
							inc(_codePoints++, TWO);		// two bytes (malformed)
							r = _err.apply(_codePoints);
							if (r >= 0) {
								break rollback;		// GOTO roll back
							}
							s = 0;					// partial code resolved
							break;
						}
						this.byte2 = b2;
						s = 3;
						// fall-through
					}
					case 3: {
						// have 1110xxxx
						// have 10xxxxxx
						// need 10xxxxxx
						if (off < m) {
							s = 0;	// state resolves either way
							int b3 = buf[off++];
							if ((b3 & 0xc0) == 0x80) {
								char code = (char) ((this.byte1 << 12)
										^ (this.byte2 << 6) ^ b3 ^ 0xfffe1f80);
								if (!Character.isSurrogate(code)) {
									put(_chars++, (char) code);
									inc(_codePoints++, THREE);		// three bytes
									r = _stop.accept(code, _codePoints);
									if (r >= 0) {
										break rollback;		// GOTO roll back
									}
									break;
								}
							}
							put(_chars++, DUMMY);
							inc(_codePoints++, THREE);		// three bytes (malformed)
							r = _err.apply(_codePoints);
							if (r >= 0) {
								break rollback;		// GOTO roll back
							}
						}
						break;
					}
					case 4: {
						// have 11110xxx
						// need 10xxxxxx
						// need 10xxxxxx
						// need 10xxxxxx
						if (off >= m) {
							break;
						}
						int b2 = buf[off++];
						if ((b2 & 0xc0) != 0x80) {
							put(_chars++, DUMMY);
							inc(_codePoints++, TWO);		// two bytes (malformed)
							r = _err.apply(_codePoints);
							if (r >= 0) {
								break rollback;		// GOTO roll back
							}
							s = 0;					// partial code resolved
							break;
						}
						this.byte2 = b2;
						s = 5;
						// fall-through
					}
					case 5: {
						// have 11110xxx
						// have 10xxxxxx
						// need 10xxxxxx
						// need 10xxxxxx
						if (off >= m) {
							break;
						}
						int b3 = buf[off++];
						if ((b3 & 0xc0) != 0x80) {
							put(_chars++, DUMMY);
							inc(_codePoints++, THREE);		// three bytes (malformed)
							r = _err.apply(_codePoints);
							if (r >= 0) {
								break rollback;		// GOTO roll back
							}
							s = 0;					// partial code resolved
							break;
						}
						this.byte3 = b3;
						s = 6;
						// fall-through
					}
					default: {
						// have 11110xxx
						// have 10xxxxxx
						// have 10xxxxxx
						// need 10xxxxxx
						if (off < m) {
							s = 0;	// state resolves either way
							int b4 = buf[off++];
							if ((b4 & 0xc0) == 0x80) {
								int code = (this.byte1 << 18) ^ (this.byte2 << 12)
										^ (this.byte3 << 6) ^ b4 ^ 0x00381f80;
								if (Character.isSupplementaryCodePoint(code)) {
									char code0 = Character.highSurrogate(code);
									char code1 = Character.lowSurrogate(code);
									put(_chars++, _chars++, code0, code1);
									inc(_codePoints++, FOUR);		// four bytes
									r = _stop.accept(code, _codePoints);
									if (r >= 0) {
										break rollback;		// GOTO roll back
									}
									break;
								}
							}
							put(_chars++, DUMMY);
							inc(_codePoints++, FOUR);		// four bytes (malformed)
							r = _err.apply(_codePoints);
							if (r >= 0) {
								break rollback;		// GOTO roll back
							}
						}
						break;
					}
				}
				// handle full codes
				// loop while there is no limit, or while it's not reached yet
				//while (opLim < 0 || _codePoints < opLim) {
				while (off < m) {
					// decode next code point
					//if (off >= m) {
					//	return inputLimitReached(_inputLimit);
					//}
					int b1 = buf[off++];
					if (b1 >= 0) {
						// 1 byte, 7 bits: 0xxxxxxx
						put(_chars++, (char) b1);
						inc(_codePoints++, ONE);		// one byte
						r = _stop.accept(b1, _codePoints);
						if (r >= 0) {
							break rollback;		// GOTO roll back
						}
						continue;				// handle next code point
					}
					if ((b1 >>> 5) == 0x07fffffe && (b1 & 0x1e) != 0) {
						// 2 bytes, 11 bits: 110xxxxx 10xxxxxx
						if (off >= m) {
							// out of input
							this.byte1 = b1;
							s = 1;
							return inputLimitReached(_inputLimit);
						}
						int b2 = buf[off++];
						if ((b2 & 0xc0) != 0x80) {
							// malformed
							put(_chars++, DUMMY);
							inc(_codePoints++, TWO);		// two bytes (malformed)
							r = _err.apply(_codePoints);
						} else {
							int code = b1 ^ b2 ^ 0x00000f80;
							put(_chars++, (char) code);
							inc(_codePoints++, TWO);		// two bytes
							r = _stop.accept(code, _codePoints);
						}
						if (r >= 0) {
							break rollback;		// GOTO roll back
						}
						continue;				// handle next code point
					}
					if ((b1 >>> 4) == 0x0ffffffe) {
						// 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
						if (off >= m) {
							// out of input
							this.byte1 = b1;
							s = 2;
							return inputLimitReached(_inputLimit);
						}
						int b2 = buf[off++];
						if ((b2 & 0xc0) != 0x80) {
							put(_chars++, DUMMY);
							inc(_codePoints++, TWO);		// two bytes (malformed)
							r = _err.apply(_codePoints);
							if (r >= 0) {
								break rollback;		// GOTO roll back
							}
						}
						if (off >= m) {
							// out of input
							this.byte1 = b1;
							this.byte2 = b1;
							s = 3;
							return inputLimitReached(_inputLimit);
						}
						int b3 = buf[off++];
						if ((b3 & 0xc0) == 0x80) {
							int code = (b1 << 12) ^ (b2 << 6) ^ b3 ^ 0xfffe1f80;
							char c = (char) code;
							if (!Character.isSurrogate(c)) {
								put(_chars++, c);
								inc(_codePoints++, THREE);		// three bytes
								r = _stop.accept(code, _codePoints);
								if (r >= 0) {
									break rollback;		// GOTO roll back
								}
								continue;				// handle next code point
							}
						}
						put(_chars++, DUMMY);
						inc(_codePoints++, THREE);				// three bytes (malformed)
						r = _err.apply(_codePoints);
						if (r >= 0) {
							break rollback;				// GOTO roll back
						}
						continue;						// handle next code point
					}
					if ((b1 >>> 3) == 0x1ffffffe) {
						// 4 bytes, 21 bits: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
						if (off >= m) {
							// out of input
							this.byte1 = b1;
							s = 4;
							return inputLimitReached(_inputLimit);
						}
						int b2 = buf[off++];
						if ((b2 & 0xc0) != 0x80) {
							put(_chars++, DUMMY);
							inc(_codePoints++, TWO);			// two bytes (malformed)
							r = _err.apply(_codePoints);
							if (r >= 0) {
								break rollback;			// GOTO roll back
							}
						}
						if (off >= m) {
							// out of input
							this.byte1 = b1;
							this.byte2 = b2;
							s = 5;
							return inputLimitReached(_inputLimit);
						}
						int b3 = buf[off++];
						if ((b3 & 0xc0) != 0x80) {
							put(_chars++, DUMMY);
							inc(_codePoints++, THREE);			// three bytes (malformed)
							r = _err.apply(_codePoints);
							if (r >= 0) {
								break rollback;			// GOTO roll back
							}
						}
						if (off >= m) {
							// out of input
							this.byte1 = b1;
							this.byte2 = b2;
							this.byte3 = b3;
							s = 6;
							return inputLimitReached(_inputLimit);
						}
						int b4 = buf[off++];
						if ((b4 & 0xc0) == 0x80) {
							int code = (b1 << 18) ^ (b2 << 12) ^ (b3 << 6) ^ b4 ^ 0x00381f80;
							if (Character.isSupplementaryCodePoint(code)) {
								char code0 = Character.highSurrogate(code);
								char code1 = Character.lowSurrogate(code);
								put(_chars++, _chars++, code0, code1);
								inc(_codePoints++, FOUR);		// four bytes
								r = _stop.accept(code, _codePoints);
								if (r >= 0) {
									break rollback;		// GOTO roll back
								}
								continue;				// handle next code point
							}
						}
						put(_chars++, DUMMY);
						inc(_codePoints++, FOUR);				// four bytes (malformed)
						r = _err.apply(_codePoints);
						if (r >= 0) {
							break rollback;				// GOTO roll back
						}
						continue;						// handle next code point
					}
					// no prefix match
					put(_chars++, DUMMY);
					inc(_codePoints++, ONE);					// one byte (malformed)
					r = _err.apply(_codePoints);
					if (r >= 0) {
						break rollback;					// GOTO roll back
					}
					// handle next code point
				}
				//// no further output allowed
				//return 0L;	// done. no roll-back
				return inputLimitReached(_inputLimit);
			}	// rollback
			if (r > _codePoints) {
				throw new IllegalStateException("Roll-back amount is too great: " + r);
			}

			int x = _chars;
			int[] cnt = this.counts;
			long rb = 0L;	// input roll back amount
			for (int i = _codePoints - 1, h = _codePoints - r; i >= h; i--) {
				int n = dec(cnt, i);
				rb += n;
				_chars -= (n >>> 2) + 1;	// remove 1 char or 2 chars if 4 bytes used
			}
			int dif = x - _chars;
			charsOld -= dif;
			_codePoints -= r;
			codePointsOld -= r;
			_bytes -= rb;
			return rb;
		} finally {
			// update internals from local snapshots
			this.codePoints = _codePoints;
			this.codePointsTotal += _codePoints - codePointsOld;

			this.chars = _chars;
			this.charsTotal += _chars - charsOld;

			int num0 = off - offOld;
			this.bytes = _bytes + num0;
			this.bytesTotal = _bytesTotal + num0;

			this.state = s;
		}
	}

	@Override
	public int chars() {
		return this.chars;
	}

	@Override
	public long charsTotal() {
		return this.charsTotal;
	}

	@Override
	public String result() {
		return String.valueOf(this.charBuffer, 0, this.chars);
	}

	@Override
	public int pendingInput() {
		int s = this.state;
		switch (s) {
			case 0:
				return 0;
			case 1:
			case 2:
				return 1;
			case 3:
				return 2;
			case 4:
				return 1;
			case 5:
				return 2;
			default:
				return 3;
		}
	}

	@Override
	public Decoder dropPendingInput() {
		int s = this.state;
		switch (s) {
			case 0: {
				return this;
			}
			case 1:
			case 2:
				this.bytes--;
				break;
			case 3:
				this.bytes -= 2;
				break;
			case 4:
				this.bytes--;
				break;
			case 5:
				this.bytes -= 2;
				break;
			default:
				this.bytes -= 3;
				break;
		}
		this.state = 0;
		return this;
	}

	@Override
	public Decoder reset() {
		super.reset();
		this.state = 0;
		this.chars = 0;
		this.charsTotal = 0L;
		return this;
	}

	@Override
	public Encoding encoding() {
		return Encoding.UTF_8;
	}
}
