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
final class EncoderCP1258 extends SingleByteEncoder implements Encoder {

	// collision-free hash table for CP1258 char-to-byte conversion.
	// unless entry == INVALID_ID, highest 16 bits are used 
	// to verify key match, lowest byte is mapped byte.
	// only maps entries for keys >= 128 (non-ASCII range).
	private static final int[] TABLE = {
		INVALID_ID, 0x20300089, 0x00bd00bd, 0x030000cc, 0x00fa00fa, INVALID_ID, 0x00b700b7, INVALID_ID,
		0x00f400f4, INVALID_ID, 0x00b100b1, INVALID_ID, 0x00ee00ee, 0x201e0084, 0x00ab00ab, INVALID_ID,
		0x00e800e8, 0x20180091, 0x00a500a5, INVALID_ID, 0x00e200e2, INVALID_ID, INVALID_ID, INVALID_ID,
		0x00dc00dc, INVALID_ID, 0x02dc0098, INVALID_ID, 0x00d600d6, INVALID_ID, INVALID_ID, INVALID_ID,
		INVALID_ID, INVALID_ID, INVALID_ID, INVALID_ID, 0x00ca00ca, INVALID_ID, INVALID_ID, INVALID_ID,
		0x00c400c4, INVALID_ID, INVALID_ID, INVALID_ID, 0x00be00be, 0x030100ec, 0x00fb00fb, 0x20ab00fe,
		0x00b800b8, INVALID_ID, INVALID_ID, INVALID_ID, 0x00b200b2, INVALID_ID, 0x00ef00ef, INVALID_ID,
		0x00ac00ac, INVALID_ID, 0x00e900e9, 0x20190092, 0x00a600a6, INVALID_ID, INVALID_ID, 0x20130096,
		0x00a000a0, 0x01a000d5, INVALID_ID, INVALID_ID, INVALID_ID, INVALID_ID, 0x00d700d7, INVALID_ID,
		INVALID_ID, INVALID_ID, 0x00d100d1, INVALID_ID, INVALID_ID, INVALID_ID, 0x00cb00cb, INVALID_ID,
		INVALID_ID, INVALID_ID, 0x00c500c5, INVALID_ID, INVALID_ID, 0x010200c3, 0x00bf00bf, INVALID_ID,
		0x00fc00fc, 0x20ac0080, 0x00b900b9, INVALID_ID, 0x00f600f6, 0x20260085, 0x00b300b3, INVALID_ID,
		INVALID_ID, 0x20200086, 0x00ad00ad, INVALID_ID, 0x00ea00ea, 0x201a0082, 0x00a700a7, INVALID_ID,
		0x00e400e4, 0x20140097, 0x00a100a1, 0x01a100f5, INVALID_ID, INVALID_ID, INVALID_ID, INVALID_ID,
		0x00d800d8, INVALID_ID, INVALID_ID, INVALID_ID, INVALID_ID, 0x0152008c, INVALID_ID, INVALID_ID,
		INVALID_ID, INVALID_ID, INVALID_ID, 0x2039008b, 0x00c600c6, 0x030900d2, 0x02c60088, 0x010300e3,
		0x00c000c0, 0x030300de, INVALID_ID, INVALID_ID, 0x00ba00ba, INVALID_ID, 0x00f700f7, INVALID_ID,
		0x00b400b4, INVALID_ID, 0x00f100f1, 0x20210087, 0x00ae00ae, INVALID_ID, 0x00eb00eb, INVALID_ID,
		0x00a800a8, INVALID_ID, 0x00e500e5, INVALID_ID, 0x00a200a2, INVALID_ID, 0x00df00df, INVALID_ID,
		INVALID_ID, INVALID_ID, 0x00d900d9, INVALID_ID, INVALID_ID, INVALID_ID, 0x00d300d3, 0x0153009c,
		INVALID_ID, 0x011000d0, 0x00cd00cd, INVALID_ID, INVALID_ID, 0x203a009b, 0x00c700c7, INVALID_ID,
		INVALID_ID, INVALID_ID, 0x00c100c1, INVALID_ID, INVALID_ID, INVALID_ID, 0x00bb00bb, INVALID_ID,
		0x00f800f8, 0x0178009f, 0x00b500b5, INVALID_ID, 0x21220099, 0x20220095, 0x00af00af, 0x01af00dd,
		INVALID_ID, 0x201c0093, 0x00a900a9, INVALID_ID, 0x00e600e6, INVALID_ID, 0x00a300a3, INVALID_ID,
		0x00e000e0, 0x032300f2, INVALID_ID, INVALID_ID, 0x00da00da, INVALID_ID, INVALID_ID, INVALID_ID,
		0x00d400d4, INVALID_ID, INVALID_ID, 0x011100f0, 0x00ce00ce, INVALID_ID, INVALID_ID, INVALID_ID,
		0x00c800c8, INVALID_ID, INVALID_ID, INVALID_ID, 0x00c200c2, INVALID_ID, 0x00ff00ff, INVALID_ID,
		0x00bc00bc, INVALID_ID, 0x00f900f9, INVALID_ID, 0x00b600b6, INVALID_ID, 0x00f300f3, INVALID_ID,
		0x00b000b0, 0x01b000fd, 0x00ed00ed, 0x201d0094, 0x00aa00aa, INVALID_ID, 0x00e700e7, INVALID_ID,
		0x00a400a4, INVALID_ID, 0x00e100e1, INVALID_ID, INVALID_ID, INVALID_ID, 0x00db00db, INVALID_ID,
		INVALID_ID, INVALID_ID, INVALID_ID, INVALID_ID, INVALID_ID, 0x01920083, 0x00cf00cf, INVALID_ID,
		INVALID_ID, INVALID_ID, 0x00c900c9, INVALID_ID, INVALID_ID, INVALID_ID, INVALID_ID, INVALID_ID
	};
	
	@Override
	int convert(int i) {
		return TABLE[(((i * 10753) >>> 8) ^ (i >>> 13)) & 0xff];
	}

	@Override
	public Encoding encoding() {
		return Encoding.CP1258;
	}
}