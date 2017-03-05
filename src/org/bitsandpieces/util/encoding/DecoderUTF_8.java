/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.encoding;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 *
 * @author pp
 */
final class DecoderUTF_8 extends AbstractDecoder {

	private static final int NONE = 0,
			TWO_NEED_ONE = 1,
			THREE_NEED_TWO = 2,
			THREE_NEED_ONE = 3,
			FOUR_NEED_THREE = 4,
			FOUR_NEED_TWO = 5,
			FOUR_NEED_ONE = 6,
			LOW_SURROGATE = 7;

	private int byte0, byte1, byte2;
	private int state = NONE;
	private char surr;

	private boolean lead3(int b) {
		return b != 0xffffffe0 || (b & 0xe0) != 0x80;
	}

	private boolean continuation(int b) {
		return (b & 0xc0) == 0x80;
	}

	@Override
	int _decode(byte[] src, char[] dest, int off, int len) {
		int _offset = this.offset;
		try {
			int _limit = this.limit;
			int y = off;
			int m = off + len;
			// pending input/output
			int s = this.state;
			switch (s) {
				case NONE: {
					break;
				}
				case TWO_NEED_ONE: {
					if (_offset == _limit) {
						return UNDERFLOW;
					}
					int b1 = src[_offset];
					this.state = NONE;
					if (!continuation(b1)) {
						return ERROR;
					}
					_offset++;
					dest[y++] = (char) ((this.byte0 << 6) ^ b1 ^ 0xf80);
					break;
				}
				case THREE_NEED_TWO: {
					if (_offset == _limit) {
						return UNDERFLOW;
					}
					int b1 = src[_offset];
					if (!continuation(b1)) {
						this.state = NONE;
						return ERROR;
					}
					_offset++;
					this.byte1 = b1;
					this.state = THREE_NEED_ONE;
					// fall-through
				}
				case THREE_NEED_ONE: {
					if (_offset == _limit) {
						return UNDERFLOW;
					}
					this.state = NONE;
					int b2 = src[_offset];
					if (!continuation(b2)) {
						return ERROR;
					}
					_offset++;
					char cp = (char) ((this.byte0 << 12) ^ (this.byte1 << 6) ^ b2 ^ 0xfffe1f80);
					if (Character.isSurrogate(cp)) {
						return ERROR;
					}
					dest[y++] = cp;
					break;
				}
				case FOUR_NEED_THREE: {
					if (_offset == _limit) {
						return UNDERFLOW;
					}
					int b1 = src[_offset];
					if (!continuation(b1)) {
						this.state = NONE;
						return ERROR;
					}
					_offset++;
					this.byte1 = b1;
					this.state = FOUR_NEED_TWO;
					// fall-through
				}
				case FOUR_NEED_TWO: {
					if (_offset == _limit) {
						return UNDERFLOW;
					}
					int b2 = src[_offset];
					if (!continuation(b2)) {
						this.state = NONE;
						return ERROR;
					}
					_offset++;
					this.byte2 = b2;
					this.state = FOUR_NEED_ONE;
					// fall-through
				}
				case FOUR_NEED_ONE: {
					if (_offset == _limit) {
						return UNDERFLOW;
					}
					int b3 = src[_offset];
					if (!continuation(b3)) {
						this.state = NONE;
						return ERROR;
					}
					_offset++;
					int cp = (this.byte0 << 18) ^ (this.byte1 << 12) ^ (this.byte2 << 6) ^ b3 ^ 0x00381f80;
					if (!Character.isSupplementaryCodePoint(cp)) {
						this.state = NONE;
						return ERROR;
					}
					dest[y++] = Character.highSurrogate(cp);
					if (y == m) {
						this.state = LOW_SURROGATE;
						this.surr = Character.lowSurrogate(cp);
						return 1;
					}
					dest[y++] = Character.lowSurrogate(cp);
					break;
				}
				case LOW_SURROGATE: {
					dest[y++] = this.surr;
					this.state = NONE;
				}
				default: {
					throw new InternalError();
				}
			}
			// fast loop
			for (int maxIn = _limit - 3, maxOut = m - 1; _offset < maxIn && y < maxOut;) {
				int b0 = src[_offset++];
				if (b0 >= 0) {
					// 1 byte, 7 bits	0xxxxxxx
					dest[y++] = (char) b0;
					continue;
				} else if ((b0 >> 5) == -2 && (b0 & 0x1e) != 0) {
					// 2 bytes, 11 bits	110xxxxx 10xxxxxx
					int b1 = src[_offset];
					if (continuation(b1)) {
						_offset++;
						dest[y++] = (char) ((b0 << 6) ^ b1 ^ 0xf80);
						continue;
					}
					// malformed
				} else if ((b0 >> 4) == -2) {
					// 3 bytes, 16 bits	1110xxxx 10xxxxxx 10xxxxxx
					if (lead3(b0)) {
						int b1 = src[_offset];
						if (continuation(b1)) {
							int b2 = src[++_offset];
							if (continuation(b2)) {
								_offset++;
								char cp = (char) ((b0 << 12) ^ (b1 << 6) ^ b2 ^ 0xfffe1f80);
								if (!Character.isSurrogate(cp)) {
									dest[y++] = cp;
									continue;
								}
							}
						}
					}
					// malformed
				} else if ((b0 >> 3) == -2) {
					// 4 bytes, 21 bits	11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
					int b1 = src[_offset];
					if (continuation(b1)) {
						int b2 = src[++_offset];
						if (continuation(b2)) {
							int b3 = src[++_offset];
							if (continuation(b3)) {
								_offset++;
								int cp = (b0 << 18) ^ (b1 << 12) ^ (b2 << 6) ^ b3 ^ 0x00381f80;
								if (Character.isSupplementaryCodePoint(cp)) {
									dest[y++] = Character.highSurrogate(cp);
									dest[y++] = Character.lowSurrogate(cp);
									continue;
								}
							}
						}
					}
					// malformed
				}
				// malformed
				if (y == off) {
					return ERROR;
				}
				this.statePending = ERROR;
				return y - off;
			}
			// slow remainder loop
			for (; _offset < _limit && y < m;) {
				int b0 = src[_offset++];
				if (b0 >= 0) {
					// 1 byte, 7 bits	0xxxxxxx
					dest[y++] = (char) b0;
					continue;
				} else if ((b0 >> 5) == -2 && (b0 & 0x1e) != 0) {
					// 2 bytes, 11 bits	110xxxxx	10xxxxxx
					if (_offset == _limit) {
						this.byte0 = b0;
						this.state = TWO_NEED_ONE;
						break;
					}
					int b1 = src[_offset];
					if (continuation(b1)) {
						_offset++;
						dest[y++] = (char) ((b0 << 6) ^ b1 ^ 0xf80);
						continue;
					}
					// malformed
				} else if ((b0 >> 4) == -2) {
					// 3 bytes, 16 bits	1110xxxx 10xxxxxx 10xxxxxx
					if (lead3(b0)) {
						if (_offset == _limit) {
							this.byte0 = b0;
							this.state = THREE_NEED_TWO;
							break;
						}
						int b1 = src[_offset];
						if (continuation(b1)) {
							if (++_offset == _limit) {
								this.byte0 = b0;
								this.byte1 = b1;
								this.state = THREE_NEED_ONE;
								break;
							}
							int b2 = src[_offset];
							if (continuation(b2)) {
								_offset++;
								char cp = (char) ((b0 << 12) ^ (b1 << 6) ^ b2 ^ 0xfffe1f80);
								if (!Character.isSurrogate(cp)) {
									dest[y++] = cp;
									continue;
								}
							}
						}
					}
					// malformed
				} else if ((b0 >> 3) == -2) {
					// 4 bytes, 21 bits	11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
					if (_offset == _limit) {
						this.byte0 = b0;
						this.state = FOUR_NEED_THREE;
						break;
					}
					int b1 = src[_offset];
					if (continuation(b1)) {
						if (++_offset == _limit) {
							this.byte0 = b0;
							this.byte1 = b1;
							this.state = FOUR_NEED_TWO;
							break;
						}
						int b2 = src[_offset];
						if (continuation(b2)) {
							if (++_offset == _limit) {
								this.byte0 = b0;
								this.byte1 = b1;
								this.byte2 = b2;
								this.state = FOUR_NEED_ONE;
								break;
							}
							int b3 = src[_offset];
							if (continuation(b3)) {
								_offset++;
								int cp = (b0 << 18) ^ (b1 << 12) ^ (b2 << 6) ^ b3 ^ 0x00381f80;
								if (Character.isSupplementaryCodePoint(cp)) {
									dest[y++] = Character.highSurrogate(cp);
									if (y == m) {
										this.surr = Character.lowSurrogate(cp);
										this.state = LOW_SURROGATE;
										break;
									}
									dest[y++] = Character.lowSurrogate(cp);
									continue;
								}
							}
						}
					}
					// malformed
				}
				// malformed
				if (y == off) {
					return ERROR;
				}
				this.statePending = ERROR;
				return y - off;
			}
			if (_offset == _limit && y == off) {
				return UNDERFLOW;
			}
			return y - off;
		} finally {
			this.offset = _offset;
		}
	}

	@Override
	int _decode(byte[] src, Appendable dest, int len) throws UncheckedIOException {
		int _offset = this.offset;
		try {
			int _limit = this.limit;
			int y = 0;
			// pending input/output
			int s = this.state;
			switch (s) {
				case NONE: {
					break;
				}
				case TWO_NEED_ONE: {
					if (_offset == _limit) {
						return UNDERFLOW;
					}
					int b1 = src[_offset];
					this.state = NONE;
					if (!continuation(b1)) {
						return ERROR;
					}
					_offset++;
					dest.append((char) ((this.byte0 << 6) ^ b1 ^ 0xf80));
					break;
				}
				case THREE_NEED_TWO: {
					if (_offset == _limit) {
						return UNDERFLOW;
					}
					int b1 = src[_offset];
					if (!continuation(b1)) {
						this.state = NONE;
						return ERROR;
					}
					_offset++;
					this.byte1 = b1;
					this.state = THREE_NEED_ONE;
					// fall-through
				}
				case THREE_NEED_ONE: {
					if (_offset == _limit) {
						return UNDERFLOW;
					}
					this.state = NONE;
					int b2 = src[_offset];
					if (!continuation(b2)) {
						return ERROR;
					}
					_offset++;
					char cp = (char) ((this.byte0 << 12) ^ (this.byte1 << 6) ^ b2 ^ 0xfffe1f80);
					if (Character.isSurrogate(cp)) {
						return ERROR;
					}
					dest.append(cp);
					break;
				}
				case FOUR_NEED_THREE: {
					if (_offset == _limit) {
						return UNDERFLOW;
					}
					int b1 = src[_offset];
					if (!continuation(b1)) {
						this.state = NONE;
						return ERROR;
					}
					_offset++;
					this.byte1 = b1;
					this.state = FOUR_NEED_TWO;
					// fall-through
				}
				case FOUR_NEED_TWO: {
					if (_offset == _limit) {
						return UNDERFLOW;
					}
					int b2 = src[_offset];
					if (!continuation(b2)) {
						this.state = NONE;
						return ERROR;
					}
					_offset++;
					this.byte2 = b2;
					this.state = FOUR_NEED_ONE;
					// fall-through
				}
				case FOUR_NEED_ONE: {
					if (_offset == _limit) {
						return UNDERFLOW;
					}
					int b3 = src[_offset];
					if (!continuation(b3)) {
						this.state = NONE;
						return ERROR;
					}
					_offset++;
					int cp = (this.byte0 << 18) ^ (this.byte1 << 12) ^ (this.byte2 << 6) ^ b3 ^ 0x00381f80;
					if (!Character.isSupplementaryCodePoint(cp)) {
						this.state = NONE;
						return ERROR;
					}
					dest.append(Character.highSurrogate(cp));
					if (y == len) {
						this.state = LOW_SURROGATE;
						this.surr = Character.lowSurrogate(cp);
						return 1;
					}
					dest.append(Character.lowSurrogate(cp));
					break;
				}
				case LOW_SURROGATE: {
					dest.append(this.surr);
					this.state = NONE;
				}
				default: {
					throw new InternalError();
				}
			}
			// fast loop
			for (int maxIn = _limit - 3, maxOut = len - 1; _offset < maxIn && y < maxOut;) {
				int b0 = src[_offset++];
				if (b0 >= 0) {
					// 1 byte, 7 bits	0xxxxxxx
					dest.append((char) b0);
					continue;
				} else if ((b0 >> 5) == -2 && (b0 & 0x1e) != 0) {
					// 2 bytes, 11 bits	110xxxxx 10xxxxxx
					int b1 = src[_offset];
					if (continuation(b1)) {
						_offset++;
						dest.append((char) ((b0 << 6) ^ b1 ^ 0xf80));
						continue;
					}
					// malformed
				} else if ((b0 >> 4) == -2) {
					// 3 bytes, 16 bits	1110xxxx 10xxxxxx 10xxxxxx
					if (lead3(b0)) {
						int b1 = src[_offset];
						if (continuation(b1)) {
							int b2 = src[++_offset];
							if (continuation(b2)) {
								_offset++;
								char cp = (char) ((b0 << 12) ^ (b1 << 6) ^ b2 ^ 0xfffe1f80);
								if (!Character.isSurrogate(cp)) {
									dest.append(cp);
									continue;
								}
							}
						}
					}
					// malformed
				} else if ((b0 >> 3) == -2) {
					// 4 bytes, 21 bits	11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
					int b1 = src[_offset];
					if (continuation(b1)) {
						int b2 = src[++_offset];
						if (continuation(b2)) {
							int b3 = src[++_offset];
							if (continuation(b3)) {
								_offset++;
								int cp = (b0 << 18) ^ (b1 << 12) ^ (b2 << 6) ^ b3 ^ 0x00381f80;
								if (Character.isSupplementaryCodePoint(cp)) {
									dest.append(Character.highSurrogate(cp));
									dest.append(Character.lowSurrogate(cp));
									continue;
								}
							}
						}
					}
					// malformed
				}
				// malformed
				if (y == 0) {
					return ERROR;
				}
				this.statePending = ERROR;
				return y;
			}
			// slow remainder loop
			for (; _offset < _limit && y < len;) {
				int b0 = src[_offset++];
				if (b0 >= 0) {
					// 1 byte, 7 bits	0xxxxxxx
					dest.append((char) b0);
					continue;
				} else if ((b0 >> 5) == -2 && (b0 & 0x1e) != 0) {
					// 2 bytes, 11 bits	110xxxxx	10xxxxxx
					if (_offset == _limit) {
						this.byte0 = b0;
						this.state = TWO_NEED_ONE;
						break;
					}
					int b1 = src[_offset];
					if (continuation(b1)) {
						_offset++;
						dest.append((char) ((b0 << 6) ^ b1 ^ 0xf80));
						continue;
					}
					// malformed
				} else if ((b0 >> 4) == -2) {
					// 3 bytes, 16 bits	1110xxxx 10xxxxxx 10xxxxxx
					if (lead3(b0)) {
						if (_offset == _limit) {
							this.byte0 = b0;
							this.state = THREE_NEED_TWO;
							break;
						}
						int b1 = src[_offset];
						if (continuation(b1)) {
							if (++_offset == _limit) {
								this.byte0 = b0;
								this.byte1 = b1;
								this.state = THREE_NEED_ONE;
								break;
							}
							int b2 = src[_offset];
							if (continuation(b2)) {
								_offset++;
								char cp = (char) ((b0 << 12) ^ (b1 << 6) ^ b2 ^ 0xfffe1f80);
								if (!Character.isSurrogate(cp)) {
									dest.append(cp);
									continue;
								}
							}
						}
					}
					// malformed
				} else if ((b0 >> 3) == -2) {
					// 4 bytes, 21 bits	11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
					if (_offset == _limit) {
						this.byte0 = b0;
						this.state = FOUR_NEED_THREE;
						break;
					}
					int b1 = src[_offset];
					if (continuation(b1)) {
						if (++_offset == _limit) {
							this.byte0 = b0;
							this.byte1 = b1;
							this.state = FOUR_NEED_TWO;
							break;
						}
						int b2 = src[_offset];
						if (continuation(b2)) {
							if (++_offset == _limit) {
								this.byte0 = b0;
								this.byte1 = b1;
								this.byte2 = b2;
								this.state = FOUR_NEED_ONE;
								break;
							}
							int b3 = src[_offset];
							if (continuation(b3)) {
								_offset++;
								int cp = (b0 << 18) ^ (b1 << 12) ^ (b2 << 6) ^ b3 ^ 0x00381f80;
								if (Character.isSupplementaryCodePoint(cp)) {
									dest.append(Character.highSurrogate(cp));
									if (y == len) {
										this.surr = Character.lowSurrogate(cp);
										this.state = LOW_SURROGATE;
										break;
									}
									dest.append(Character.lowSurrogate(cp));
									continue;
								}
							}
						}
					}
					// malformed
				}
				// malformed
				if (y == 0) {
					return ERROR;
				}
				this.statePending = ERROR;
				return y;
			}
			if (_offset == _limit && y == 0) {
				return UNDERFLOW;
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
		return this.state == LOW_SURROGATE ? 1 : 0;
	}

	@Override
	public Encoding encoding() {
		return Encoding.UTF_8;
	}
}
