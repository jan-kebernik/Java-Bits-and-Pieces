/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.encoding;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * A character encoding scheme.
 *
 * @author Jan Kebernik
 */
public enum Encoding {
	/**
	 * American Standard Code for Information Interchange
	 */
	ASCII(StandardCharsets.US_ASCII) {
		@Override
		public Decoder newDecoder() {
			return new DecoderASCII();
		}
	},
	/**
	 * 8-Bit Unicode.
	 */
	UTF_8(StandardCharsets.UTF_8) {
		@Override
		public Decoder newDecoder() {
			return new DecoderUTF_8();
		}
	},
	/**
	 * Eastern European Code Page.
	 */
	CP1250(Charset.forName("windows-1250")) {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP1250();
		}
	},
	/**
	 * Russian (Cyrillic) Code Page.
	 */
	CP1251(Charset.forName("windows-1251")) {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP1251();
		}
	},
	/**
	 * Central European Code Page.
	 */
	CP1252(Charset.forName("windows-1252")) {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP1252();
		}
	},
	/**
	 * Greek Code Page.
	 */
	CP1253(Charset.forName("windows-1253")) {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP1253();
		}
	},
	/**
	 * Turkish Code Page.
	 */
	CP1254(Charset.forName("windows-1254")) {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP1254();
		}
	},
	/**
	 * Hebrew Code Page.
	 */
	CP1255(Charset.forName("windows-1255")) {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP1255();
		}
	},
	/**
	 * Arabic Code Page.
	 */
	CP1256(Charset.forName("windows-1256")) {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP1256();
		}
	},
	/**
	 * Baltic Code Page.
	 */
	CP1257(Charset.forName("windows-1257")) {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP1257();
		}
	},
	/**
	 * Vietnamese Code Page.
	 */
	CP1258(Charset.forName("windows-1258")) {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP1258();
		}
	},
	/**
	 * Thai Code Page.
	 */
	CP874(Charset.forName("windows-874")) {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP874();
		}
	},
	/**
	 * Japanese Code Page.
	 */
	CP932(Charset.forName("windows-31j")) {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP932();
		}
	},
	/**
	 * Simplified Chinese Code Page.
	 */
	CP936(Charset.forName("windows-936")) {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP936();
		}
	},
	/**
	 * Korean Code Page.
	 */
	CP949(Charset.forName("windows-949")) {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP949();
		}
	},
	/**
	 * Traditional Chinese Code Page.
	 */
	CP950(Charset.forName("windows-950")) {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP950();
		}
	},
	/**
	 * Portugese Code Page.
	 */
	CP860(Charset.forName("IBM860")) {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP860();
		}
	},
	/**
	 * Nordic (except Iceland) Code Page.
	 */
	CP865(Charset.forName("IBM865")) {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP860();
		}
	},
	/**
	 * ISO/IEC 8859-1.
	 */
	ISO_8859_1(Charset.forName("ISO-8859-1")) {
		@Override
		public Decoder newDecoder() {
			return new DecoderISO_8859_1();
		}
	};

	public abstract Decoder newDecoder();

	private final Charset charset;

	private Encoding(Charset charset) {
		this.charset = charset;
	}

	// TODO decoders
}
