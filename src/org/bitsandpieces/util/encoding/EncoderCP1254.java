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
final class EncoderCP1254 extends SingleByteEncoder implements Encoder {
	
	// collision-free hash table for CP1254 char-to-byte conversion.
	// unless entry == INVALID_ID, highest 16 bits are used 
	// to verify key match, lowest byte is mapped byte.
	// only maps entries for keys >= 128 (non-ASCII range).
	private static final int[] TABLE = {
		INVALID_ID, 0x00df00df, 0x00be00be, INVALID_ID, 0x20200086, INVALID_ID, INVALID_ID, INVALID_ID,
		0x00f800f8, 0x00d700d7, 0x00b600b6, 0x2039008b, 0x20180091, INVALID_ID, 0x0153009c, INVALID_ID,
		INVALID_ID, 0x00cf00cf, 0x00ae00ae, INVALID_ID, INVALID_ID, INVALID_ID, INVALID_ID, INVALID_ID,
		0x00e800e8, 0x00c700c7, 0x00a600a6, INVALID_ID, INVALID_ID, INVALID_ID, INVALID_ID, INVALID_ID,
		0x00e000e0, 0x00bf00bf, INVALID_ID, 0x20210087, INVALID_ID, INVALID_ID, INVALID_ID, 0x00f900f9,
		0x00d800d8, 0x00b700b7, 0x203a009b, 0x20190092, INVALID_ID, INVALID_ID, INVALID_ID, 0x00f100f1,
		INVALID_ID, 0x00af00af, INVALID_ID, INVALID_ID, INVALID_ID, INVALID_ID, INVALID_ID, 0x00e900e9,
		0x00c800c8, 0x00a700a7, INVALID_ID, INVALID_ID, INVALID_ID, INVALID_ID, INVALID_ID, 0x00e100e1,
		0x00c000c0, INVALID_ID, 0x20220095, 0x21220099, INVALID_ID, INVALID_ID, 0x00fa00fa, 0x00d900d9,
		0x00b800b8, INVALID_ID, 0x201a0082, INVALID_ID, INVALID_ID, INVALID_ID, 0x00f200f2, 0x00d100d1,
		0x00b000b0, INVALID_ID, INVALID_ID, INVALID_ID, INVALID_ID, INVALID_ID, 0x00ea00ea, 0x00c900c9,
		0x00a800a8, INVALID_ID, INVALID_ID, INVALID_ID, INVALID_ID, INVALID_ID, 0x00e200e2, 0x00c100c1,
		0x00a000a0, INVALID_ID, INVALID_ID, 0x015e00de, INVALID_ID, 0x00fb00fb, 0x00da00da, 0x00b900b9,
		INVALID_ID, INVALID_ID, INVALID_ID, INVALID_ID, INVALID_ID, 0x00f300f3, 0x00d200d2, 0x00b100b1,
		INVALID_ID, 0x20130096, INVALID_ID, INVALID_ID, INVALID_ID, 0x00eb00eb, 0x00ca00ca, 0x00a900a9,
		INVALID_ID, INVALID_ID, INVALID_ID, INVALID_ID, INVALID_ID, 0x00e300e3, 0x00c200c2, 0x00a100a1,
		INVALID_ID, INVALID_ID, 0x015f00fe, INVALID_ID, 0x00fc00fc, 0x00db00db, 0x00ba00ba, INVALID_ID,
		0x201c0093, 0x0178009f, INVALID_ID, INVALID_ID, 0x00f400f4, 0x00d300d3, 0x00b200b2, INVALID_ID,
		0x20140097, INVALID_ID, INVALID_ID, INVALID_ID, 0x00ec00ec, 0x00cb00cb, 0x00aa00aa, INVALID_ID,
		INVALID_ID, INVALID_ID, INVALID_ID, INVALID_ID, 0x00e400e4, 0x00c300c3, 0x00a200a2, INVALID_ID,
		INVALID_ID, 0x0160008a, INVALID_ID, 0x011e00d0, 0x00dc00dc, 0x00bb00bb, 0x02dc0098, 0x201d0094,
		INVALID_ID, INVALID_ID, INVALID_ID, 0x00f500f5, 0x00d400d4, 0x00b300b3, INVALID_ID, 0x01920083,
		INVALID_ID, INVALID_ID, INVALID_ID, 0x00ed00ed, 0x00cc00cc, 0x00ab00ab, INVALID_ID, INVALID_ID,
		INVALID_ID, INVALID_ID, INVALID_ID, 0x00e500e5, 0x00c400c4, 0x00a300a3, 0x20260085, INVALID_ID,
		0x0161009a, INVALID_ID, 0x011f00f0, INVALID_ID, 0x00bc00bc, INVALID_ID, 0x201e0084, INVALID_ID,
		INVALID_ID, INVALID_ID, 0x00f600f6, 0x00d500d5, 0x00b400b4, INVALID_ID, INVALID_ID, INVALID_ID,
		INVALID_ID, 0x013000dd, 0x00ee00ee, 0x00cd00cd, 0x00ac00ac, INVALID_ID, INVALID_ID, INVALID_ID,
		INVALID_ID, INVALID_ID, 0x00e600e6, 0x00c500c5, 0x00a400a4, INVALID_ID, INVALID_ID, INVALID_ID,
		INVALID_ID, 0x00ff00ff, INVALID_ID, 0x00bd00bd, INVALID_ID, INVALID_ID, INVALID_ID, INVALID_ID,
		INVALID_ID, 0x00f700f7, 0x00d600d6, 0x00b500b5, INVALID_ID, INVALID_ID, INVALID_ID, 0x0152008c,
		0x013100fd, 0x00ef00ef, 0x00ce00ce, 0x00ad00ad, 0x20300089, INVALID_ID, INVALID_ID, INVALID_ID,
		0x20ac0080, 0x00e700e7, 0x00c600c6, 0x00a500a5, 0x02c60088, INVALID_ID, INVALID_ID, INVALID_ID
	};

	@Override
	int convert(int c) {
		return TABLE[(((((c << 13) + c) >>> 8) ^ (c >>> 11)) - c) & 0xff];
	}

	@Override
	public Encoding encoding() {
		return Encoding.CP1254;
	}
}
