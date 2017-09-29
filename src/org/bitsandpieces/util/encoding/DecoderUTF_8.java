/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.encoding;

import java.io.UncheckedIOException;

final class DecoderUTF_8 extends AbstractDecoder {

	private static final int NONE = 0,
			OF_TWO_NEED_ONE = 1,
			OF_THREE_NEED_TWO = 2,
			OF_THREE_NEED_ONE = 3,
			OF_FOUR_NEED_THREE = 4,
			OF_FOUR_NEED_TWO = 5,
			OF_FOUR_NEED_ONE = 6,
			PENDING_LOW_SURROGATE = 7;

	private int byte0, byte1, byte2;
	private int state = NONE;
	private char surr;
	private long codePoints;

	@Override
	public boolean hasPending() {
		return this.state != NONE;
	}
	
	@Override
	int _decode(byte[] src, Appendable dest, int maxChars, int maxCodePoints, int _offset, int _limit) {
		int _numCP = 0;
		int _offsetOld = _offset;
		try {
			int y = 0;
			switch (this.state) {
				case OF_TWO_NEED_ONE: {
					if (_offset == _limit) {
						return 0;
					}
					int b1 = src[_offset];
					this.state = NONE;
					if ((b1 & 0xc0) != 0x80) {
						// not a continuation byte. lead byte at fault.
						return -1;
					}
					_offset++;	// consume continuation byte
					dest.append((char) ((this.byte0 << 6) ^ b1 ^ 0xf80));
					y++;
					_numCP++;	// code point resolved
					break;
				}
				case OF_THREE_NEED_TWO: {
					if (_offset == _limit) {
						return 0;
					}
					int b1 = src[_offset];
					if ((b1 & 0xc0) != 0x80) {
						// not a continuation byte. lead byte at fault.
						this.state = NONE;
						return -1;
					}
					// is continuation byte. cannot start new sequence.
					_offset++;	// consume continuation byte
					this.byte1 = b1;
					this.state = OF_THREE_NEED_ONE;
					// FALL-THROUGH
				}
				case OF_THREE_NEED_ONE: {
					if (_offset == _limit) {
						return 0;
					}
					this.state = NONE;
					int b2 = src[_offset];
					if ((b2 & 0xc0) != 0x80) {
						// not a continuation byte. lead and previous cont. byte at fault.
						return -2;
					}
					// is continuation byte. cannot start new sequence.
					_offset++;	// consume continuation byte
					char cp = (char) ((this.byte0 << 12) ^ (this.byte1 << 6) ^ b2 ^ 0xfffe1f80);
					if (Character.isSurrogate(cp)) {
						// malformed 3-byte code point
						return -3;
					}
					dest.append(cp);
					y++;
					_numCP++;	// code point resolved
					break;
				}
				case OF_FOUR_NEED_THREE: {
					if (_offset == _limit) {
						return 0;
					}
					int b1 = src[_offset];
					if ((b1 & 0xc0) != 0x80) {
						// not a continuation byte. lead byte at fault.
						this.state = NONE;
						return -1;
					}
					// is continuation byte. cannot start new sequence.
					_offset++;	// consume continuation byte
					this.byte1 = b1;
					this.state = OF_FOUR_NEED_TWO;
					// FALL-THROUGH
				}
				case OF_FOUR_NEED_TWO: {
					if (_offset == _limit) {
						return 0;
					}
					int b2 = src[_offset];
					if ((b2 & 0xc0) != 0x80) {
						// not a continuation byte. lead and previous cont. byte at fault.
						this.state = NONE;
						return -2;
					}
					// is continuation byte. cannot start new sequence.
					_offset++;	// consume continuation byte
					this.byte2 = b2;
					this.state = OF_FOUR_NEED_ONE;
					// FALL-THROUGH
				}
				case OF_FOUR_NEED_ONE: {
					if (_offset == _limit) {
						return 0;
					}
					int b3 = src[_offset];
					if ((b3 & 0xc0) != 0x80) {
						// not a continuation byte. lead and previous cont. bytes at fault.
						this.state = NONE;
						return -3;
					}
					// is continuation byte. cannot start new sequence.
					_offset++;	// consume continuation byte
					int cp = (this.byte0 << 18) ^ (this.byte1 << 12) ^ (this.byte2 << 6) ^ b3 ^ 0x00381f80;
					if (!Character.isSupplementaryCodePoint(cp)) {
						this.state = NONE;
						return -4;
					}
					dest.append(Character.highSurrogate(cp));
					y++;
					if (y == maxChars) {
						this.state = PENDING_LOW_SURROGATE;
						this.surr = Character.lowSurrogate(cp);
						this.chars++;
						return 1;
					}
					dest.append(Character.lowSurrogate(cp));
					y++;
					_numCP++;	// code point resolved
					break;
				}
				case PENDING_LOW_SURROGATE: {
					dest.append(this.surr);
					y++;
					_numCP++;	// code point resolved
					this.state = NONE;
					// FALL-THROUGH
				}
				default: {
					break;
				}
			}
			// fast loop
			for (int maxIn = _limit - 3, maxOut = maxChars - 1;
					_offset < maxIn && y < maxOut && _numCP < maxCodePoints;) {
				int b0 = src[_offset++];	// keep sign for ensuing black magic
				if (b0 >= 0) {
					// 1 byte, 7 bits	0xxxxxxx	(ASCII range)
					dest.append((char) b0);
					y++;
					_numCP++;	// code point resolved
					continue;
				}
				int error_length = -1;
				if ((b0 >> 5) == -2 && (b0 & 0x1e) != 0) {
					// 2 bytes, 11 bits	110xxxxx 10xxxxxx
					int b1 = src[_offset];
					if ((b1 & 0xc0) == 0x80) {
						_offset++;
						dest.append((char) ((b0 << 6) ^ b1 ^ 0xf80));
						y++;
						_numCP++;	// code point resolved
						continue;
					}
				} else if ((b0 >> 4) == -2) {
					// 3 bytes, 16 bits	1110xxxx 10xxxxxx 10xxxxxx
					if (b0 != 0xffffffe0 || (b0 & 0xe0) != 0x80) {
						int b1 = src[_offset];
						if ((b1 & 0xc0) == 0x80) {
							int b2 = src[++_offset];
							if ((b2 & 0xc0) == 0x80) {
								_offset++;
								char cp = (char) ((b0 << 12) ^ (b1 << 6) ^ b2 ^ 0xfffe1f80);
								if (!Character.isSurrogate(cp)) {
									dest.append(cp);
									y++;
									_numCP++;	// code point resolved
									continue;
								}
								error_length--;	// malformed code point
							}
							error_length--;	// not a continuation byte
						}
					}
				} else if ((b0 >> 3) == -2) {
					// 4 bytes, 21 bits	11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
					int b1 = src[_offset];
					if ((b1 & 0xc0) == 0x80) {
						int b2 = src[++_offset];
						if ((b2 & 0xc0) == 0x80) {
							int b3 = src[++_offset];
							if ((b3 & 0xc0) == 0x80) {
								_offset++;
								int cp = (b0 << 18) ^ (b1 << 12) ^ (b2 << 6) ^ b3 ^ 0x00381f80;
								if (Character.isSupplementaryCodePoint(cp)) {
									dest.append(Character.highSurrogate(cp));
									y++;
									dest.append(Character.lowSurrogate(cp));
									y++;
									_numCP++;	// code point resolved
									continue;
								}
								error_length--;	// malfored code point
							}
							error_length--;	// not a continuation byte
						}
						error_length--;	// not a continuation byte
					}
				}
				if (y == 0) {
					// no output produced yet
					return error_length;
				}
				this.pendingError = error_length;
				this.chars += y;
				return y;
			}
			// "slow" remainder loop
			for (; _offset < _limit && y < maxChars && _numCP < maxCodePoints;) {
				int b0 = src[_offset++];	// keep sign for ensuing black magic
				if (b0 >= 0) {
					// 1 byte, 7 bits	0xxxxxxx	(ASCII range)
					dest.append((char) b0);
					y++;
					_numCP++;
					continue;
				}
				int error_length = -1;
				if ((b0 >> 5) == -2 && (b0 & 0x1e) != 0) {
					// 2 bytes, 11 bits	110xxxxx 10xxxxxx
					if (_offset == _limit) {
						this.byte0 = b0;
						this.state = OF_TWO_NEED_ONE;
						break;
					}
					int b1 = src[_offset];
					if ((b1 & 0xc0) == 0x80) {
						_offset++;
						dest.append((char) ((b0 << 6) ^ b1 ^ 0xf80));
						y++;
						_numCP++;	// code point resolved
						continue;
					}
					// not a continuation byte
				} else if ((b0 >> 4) == -2) {
					// 3 bytes, 16 bits	1110xxxx 10xxxxxx 10xxxxxx
					if (b0 != 0xffffffe0 || (b0 & 0xe0) != 0x80) {
						if (_offset == _limit) {
							this.byte0 = b0;
							this.state = OF_THREE_NEED_TWO;
							break;
						}
						int b1 = src[_offset];
						if ((b1 & 0xc0) == 0x80) {
							if (++_offset == _limit) {
								this.byte0 = b0;
								this.byte1 = b1;
								this.state = OF_THREE_NEED_ONE;
								break;
							}
							int b2 = src[_offset];
							if ((b2 & 0xc0) == 0x80) {
								_offset++;
								char cp = (char) ((b0 << 12) ^ (b1 << 6) ^ b2 ^ 0xfffe1f80);
								if (!Character.isSurrogate(cp)) {
									dest.append(cp);
									y++;
									_numCP++;
									continue;
								}
								error_length--;	// malformed code point
							}
							error_length--;	// not a continuation byte
						}
						// not a continuation byte
					}
					// malformed lead byte
				} else if ((b0 >> 3) == -2) {
					// 4 bytes, 21 bits	11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
					if (_offset == _limit) {
						this.byte0 = b0;
						this.state = OF_FOUR_NEED_THREE;
						break;
					}
					int b1 = src[_offset];
					if ((b1 & 0xc0) == 0x80) {
						if (++_offset == _limit) {
							this.byte0 = b0;
							this.byte1 = b1;
							this.state = OF_FOUR_NEED_TWO;
							break;
						}
						int b2 = src[_offset];
						if ((b2 & 0xc0) == 0x80) {
							if (++_offset == _limit) {
								this.byte0 = b0;
								this.byte1 = b1;
								this.byte2 = b2;
								this.state = OF_FOUR_NEED_ONE;
								break;
							}
							int b3 = src[_offset];
							if ((b3 & 0xc0) == 0x80) {
								_offset++;	// consume continuation byte
								int cp = (b0 << 18) ^ (b1 << 12) ^ (b2 << 6) ^ b3 ^ 0x00381f80;
								if (Character.isSupplementaryCodePoint(cp)) {
									dest.append(Character.highSurrogate(cp));
									y++;
									if (y == maxChars) {
										// out of output
										this.surr = Character.lowSurrogate(cp);
										this.state = PENDING_LOW_SURROGATE;
										break;
									}
									dest.append(Character.lowSurrogate(cp));
									y++;
									_numCP++;
									continue;
								}
								error_length--;	// malformed code point
							}
							error_length--;	// not a continuation byte
						}
						error_length--;	// not a continuation byte
					}
					// not a continuation byte
				}
				if (y == 0) {
					return error_length;
				}
				this.pendingError = error_length;
				break;
			}
			this.chars += y;
			return y;
		} catch (java.io.IOException ex) {
			throw new UncheckedIOException(ex);
		} finally {
			this.bytes += (_offset - _offsetOld);
			this.offset = _offset;
			this.codePoints += _numCP;
		}
	}

	@Override
	int _decode(byte[] src, char[] dest, int off, int maxChars, int maxCodePoints, int _offset, int _limit) {
		int _numCP = 0;
		int _offsetOld = _offset;
		try {
			int y = off;
			int m = off + maxChars;
			switch (this.state) {
				case OF_TWO_NEED_ONE: {
					if (_offset == _limit) {
						return 0;
					}
					int b1 = src[_offset];
					this.state = NONE;
					if ((b1 & 0xc0) != 0x80) {
						// not a continuation byte. lead byte at fault.
						return -1;
					}
					_offset++;	// consume continuation byte
					dest[y++] = (char) ((this.byte0 << 6) ^ b1 ^ 0xf80);
					_numCP++;	// code point resolved
					break;
				}
				case OF_THREE_NEED_TWO: {
					if (_offset == _limit) {
						return 0;
					}
					int b1 = src[_offset];
					if ((b1 & 0xc0) != 0x80) {
						// not a continuation byte. lead byte at fault.
						this.state = NONE;
						return -1;
					}
					// is continuation byte. cannot start new sequence.
					_offset++;	// consume continuation byte
					this.byte1 = b1;
					this.state = OF_THREE_NEED_ONE;
					// FALL-THROUGH
				}
				case OF_THREE_NEED_ONE: {
					if (_offset == _limit) {
						return 0;
					}
					this.state = NONE;
					int b2 = src[_offset];
					if ((b2 & 0xc0) != 0x80) {
						// not a continuation byte. lead and previous cont. byte at fault.
						return -2;
					}
					// is continuation byte. cannot start new sequence.
					_offset++;	// consume continuation byte
					char cp = (char) ((this.byte0 << 12) ^ (this.byte1 << 6) ^ b2 ^ 0xfffe1f80);
					if (Character.isSurrogate(cp)) {
						// malformed 3-byte code point
						return -3;
					}
					dest[y++] = cp;
					_numCP++;	// code point resolved
					break;
				}
				case OF_FOUR_NEED_THREE: {
					if (_offset == _limit) {
						return 0;
					}
					int b1 = src[_offset];
					if ((b1 & 0xc0) != 0x80) {
						// not a continuation byte. lead byte at fault.
						this.state = NONE;
						return -1;
					}
					// is continuation byte. cannot start new sequence.
					_offset++;	// consume continuation byte
					this.byte1 = b1;
					this.state = OF_FOUR_NEED_TWO;
					// FALL-THROUGH
				}
				case OF_FOUR_NEED_TWO: {
					if (_offset == _limit) {
						return 0;
					}
					int b2 = src[_offset];
					if ((b2 & 0xc0) != 0x80) {
						// not a continuation byte. lead and previous cont. byte at fault.
						this.state = NONE;
						return -2;
					}
					// is continuation byte. cannot start new sequence.
					_offset++;	// consume continuation byte
					this.byte2 = b2;
					this.state = OF_FOUR_NEED_ONE;
					// FALL-THROUGH
				}
				case OF_FOUR_NEED_ONE: {
					if (_offset == _limit) {
						return 0;
					}
					int b3 = src[_offset];
					if ((b3 & 0xc0) != 0x80) {
						// not a continuation byte. lead and previous cont. bytes at fault.
						this.state = NONE;
						return -3;
					}
					// is continuation byte. cannot start new sequence.
					_offset++;	// consume continuation byte
					int cp = (this.byte0 << 18) ^ (this.byte1 << 12) ^ (this.byte2 << 6) ^ b3 ^ 0x00381f80;
					if (!Character.isSupplementaryCodePoint(cp)) {
						this.state = NONE;
						return -4;
					}
					dest[y++] = Character.highSurrogate(cp);
					if (y == m) {
						this.state = PENDING_LOW_SURROGATE;
						this.surr = Character.lowSurrogate(cp);
						this.chars++;
						return 1;
					}
					dest[y++] = Character.lowSurrogate(cp);
					_numCP++;	// code point resolved
					break;
				}
				case PENDING_LOW_SURROGATE: {
					dest[y++] = this.surr;
					_numCP++;	// code point resolved
					this.state = NONE;
					// FALL-THROUGH
				}
				default: {
					break;
				}
			}
			// fast loop
			for (int maxIn = _limit - 3, maxOut = m - 1;
					_offset < maxIn && y < maxOut && _numCP < maxCodePoints;) {
				int b0 = src[_offset++];	// keep sign for ensuing black magic
				if (b0 >= 0) {
					// 1 byte, 7 bits	0xxxxxxx	(ASCII range)
					dest[y++] = (char) b0;
					_numCP++;	// code point resolved
					continue;
				}
				int error_length = -1;
				if ((b0 >> 5) == -2 && (b0 & 0x1e) != 0) {
					// 2 bytes, 11 bits	110xxxxx 10xxxxxx
					int b1 = src[_offset];
					if ((b1 & 0xc0) == 0x80) {
						_offset++;
						dest[y++] = (char) ((b0 << 6) ^ b1 ^ 0xf80);
						_numCP++;	// code point resolved
						continue;
					}
				} else if ((b0 >> 4) == -2) {
					// 3 bytes, 16 bits	1110xxxx 10xxxxxx 10xxxxxx
					if (b0 != 0xffffffe0 || (b0 & 0xe0) != 0x80) {
						int b1 = src[_offset];
						if ((b1 & 0xc0) == 0x80) {
							int b2 = src[++_offset];
							if ((b2 & 0xc0) == 0x80) {
								_offset++;
								char cp = (char) ((b0 << 12) ^ (b1 << 6) ^ b2 ^ 0xfffe1f80);
								if (!Character.isSurrogate(cp)) {
									dest[y++] = cp;
									_numCP++;	// code point resolved
									continue;
								}
								error_length--;	// malformed code point
							}
							error_length--;	// not a continuation byte
						}
					}
				} else if ((b0 >> 3) == -2) {
					// 4 bytes, 21 bits	11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
					int b1 = src[_offset];
					if ((b1 & 0xc0) == 0x80) {
						int b2 = src[++_offset];
						if ((b2 & 0xc0) == 0x80) {
							int b3 = src[++_offset];
							if ((b3 & 0xc0) == 0x80) {
								_offset++;
								int cp = (b0 << 18) ^ (b1 << 12) ^ (b2 << 6) ^ b3 ^ 0x00381f80;
								if (Character.isSupplementaryCodePoint(cp)) {
									dest[y++] = Character.highSurrogate(cp);
									dest[y++] = Character.lowSurrogate(cp);
									_numCP++;	// code point resolved
									continue;
								}
								error_length--;	// malfored code point
							}
							error_length--;	// not a continuation byte
						}
						error_length--;	// not a continuation byte
					}
				}
				if (y == off) {
					// no output produced yet
					return error_length;
				}
				this.pendingError = error_length;
				int n = y - off;
				this.chars += n;
				return n;
			}
			// "slow" remainder loop
			for (; _offset < _limit && y < m && _numCP < maxCodePoints;) {
				int b0 = src[_offset++];	// keep sign for ensuing black magic
				if (b0 >= 0) {
					// 1 byte, 7 bits	0xxxxxxx	(ASCII range)
					dest[y++] = (char) b0;
					_numCP++;
					continue;
				}
				int error_length = -1;
				if ((b0 >> 5) == -2 && (b0 & 0x1e) != 0) {
					// 2 bytes, 11 bits	110xxxxx 10xxxxxx
					if (_offset == _limit) {
						this.byte0 = b0;
						this.state = OF_TWO_NEED_ONE;
						break;
					}
					int b1 = src[_offset];
					if ((b1 & 0xc0) == 0x80) {
						_offset++;
						dest[y++] = (char) ((b0 << 6) ^ b1 ^ 0xf80);
						_numCP++;	// code point resolved
						continue;
					}
					// not a continuation byte
				} else if ((b0 >> 4) == -2) {
					// 3 bytes, 16 bits	1110xxxx 10xxxxxx 10xxxxxx
					if (b0 != 0xffffffe0 || (b0 & 0xe0) != 0x80) {
						if (_offset == _limit) {
							this.byte0 = b0;
							this.state = OF_THREE_NEED_TWO;
							break;
						}
						int b1 = src[_offset];
						if ((b1 & 0xc0) == 0x80) {
							if (++_offset == _limit) {
								this.byte0 = b0;
								this.byte1 = b1;
								this.state = OF_THREE_NEED_ONE;
								break;
							}
							int b2 = src[_offset];
							if ((b2 & 0xc0) == 0x80) {
								_offset++;
								char cp = (char) ((b0 << 12) ^ (b1 << 6) ^ b2 ^ 0xfffe1f80);
								if (!Character.isSurrogate(cp)) {
									dest[y++] = cp;
									_numCP++;
									continue;
								}
								error_length--;	// malformed code point
							}
							error_length--;	// not a continuation byte
						}
						// not a continuation byte
					}
					// malformed lead byte
				} else if ((b0 >> 3) == -2) {
					// 4 bytes, 21 bits	11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
					if (_offset == _limit) {
						this.byte0 = b0;
						this.state = OF_FOUR_NEED_THREE;
						break;
					}
					int b1 = src[_offset];
					if ((b1 & 0xc0) == 0x80) {
						if (++_offset == _limit) {
							this.byte0 = b0;
							this.byte1 = b1;
							this.state = OF_FOUR_NEED_TWO;
							break;
						}
						int b2 = src[_offset];
						if ((b2 & 0xc0) == 0x80) {
							if (++_offset == _limit) {
								this.byte0 = b0;
								this.byte1 = b1;
								this.byte2 = b2;
								this.state = OF_FOUR_NEED_ONE;
								break;
							}
							int b3 = src[_offset];
							if ((b3 & 0xc0) == 0x80) {
								_offset++;	// consume continuation byte
								int cp = (b0 << 18) ^ (b1 << 12) ^ (b2 << 6) ^ b3 ^ 0x00381f80;
								if (Character.isSupplementaryCodePoint(cp)) {
									dest[y++] = Character.highSurrogate(cp);
									if (y == m) {
										// out of output
										this.surr = Character.lowSurrogate(cp);
										this.state = PENDING_LOW_SURROGATE;
										break;
									}
									dest[y++] = Character.lowSurrogate(cp);
									_numCP++;
									continue;
								}
								error_length--;	// malformed code point
							}
							error_length--;	// not a continuation byte
						}
						error_length--;	// not a continuation byte
					}
					// not a continuation byte
				}
				if (y == off) {
					return error_length;
				}
				this.pendingError = error_length;
				break;
			}
			int n = y - off;
			this.chars += n;
			return n;
		} finally {
			this.bytes += (_offset - _offsetOld);
			this.offset = _offset;
			this.codePoints += _numCP;
		}
	}

	@Override
	public Encoding encoding() {
		return Encoding.UTF_8;
	}

	@Override
	public int needsInput() {
		int s = this.state;
		switch (s) {
			case OF_TWO_NEED_ONE:
				return 1;
			case OF_THREE_NEED_TWO:
				return 2;
			case OF_THREE_NEED_ONE:
				return 1;
			case OF_FOUR_NEED_THREE:
				return 3;
			case OF_FOUR_NEED_TWO:
				return 2;
			case OF_FOUR_NEED_ONE:
				return 1;
			default:
				return 0;
		}
	}

	@Override
	public int pendingInput() {
		int s = this.state;
		switch (s) {
			case OF_TWO_NEED_ONE:
				return 1;
			case OF_THREE_NEED_TWO:
				return 1;
			case OF_THREE_NEED_ONE:
				return 2;
			case OF_FOUR_NEED_THREE:
				return 1;
			case OF_FOUR_NEED_TWO:
				return 2;
			case OF_FOUR_NEED_ONE:
				return 3;
			default:
				return 0;
		}
	}

	@Override
	public int pendingOutput() {
		return this.state == PENDING_LOW_SURROGATE ? 1 : 0;
	}

	@Override
	public long codePointsResolved() {
		return this.codePoints;
	}

	@Override
	public Decoder dropPending() {
		this.bytes -= pendingInput();
		this.state = NONE;
		return this;
	}

	@Override
	public Decoder reset() {
		_reset();
		this.state = NONE;
		this.codePoints = 0L;
		return dropInput();
	}
}
