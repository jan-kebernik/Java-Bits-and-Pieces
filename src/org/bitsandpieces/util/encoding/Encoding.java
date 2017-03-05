/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.encoding;

/**
 * A character encoding scheme.
 *
 * @author Jan Kebernik
 */
public enum Encoding {
	/**
	 * American Standard Code for Information Interchange
	 */
	ASCII() {
		@Override
		public Decoder newDecoder() {
			return new DecoderASCII();
		}
		@Override
		public Encoder newEncoder() {
			return new EncoderASCII();
		}
	},
	/**
	 * Eight-bit Unicode (or UCS) Transformation Format.
	 */
	UTF_8() {
		@Override
		public Decoder newDecoder() {
			return new DecoderUTF_8();
		}
		@Override
		public Encoder newEncoder() {
			return new EncoderUTF_8();
		}
	},
	/**
	 * Windows Eastern European.
	 */
	CP1250() {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP1250();
		}
		@Override
		public Encoder newEncoder() {
			return new EncoderCP1250();
		}
	},
	/**
	 * Windows Cyrillic.
	 */
	CP1251() {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP1251();
		}
		@Override
		public Encoder newEncoder() {
			return new EncoderCP1251();
		}
	},
	/**
	 * Windows Latin-1.
	 */
	CP1252() {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP1252();
		}
		@Override
		public Encoder newEncoder() {
			return new EncoderCP1252();
		}
	},
	/**
	 * Windows Greek.
	 */
	CP1253() {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP1253();
		}
		@Override
		public Encoder newEncoder() {
			return new EncoderCP1253();
		}
	},
	/**
	 * Windows Turkish.
	 */
	CP1254() {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP1254();
		}
		@Override
		public Encoder newEncoder() {
			return new EncoderCP1254();
		}
	},
	/**
	 * Windows Hebrew.
	 */
	CP1255() {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP1255();
		}
		@Override
		public Encoder newEncoder() {
			return new EncoderCP1255();
		}
	},
	/**
	 * Windows Arabic.
	 */
	CP1256() {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP1256();
		}
		@Override
		public Encoder newEncoder() {
			return new EncoderCP1256();
		}
	},
	/**
	 * Windows Baltic.
	 */
	CP1257() {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP1257();
		}
		@Override
		public Encoder newEncoder() {
			return new EncoderCP1257();
		}
	},
	/**
	 * Windows Vietnamese.
	 */
	CP1258() {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP1258();
		}
		@Override
		public Encoder newEncoder() {
			return new EncoderCP1258();
		}
	},
	/**
	 * Windows Thai.
	 */
	CP874() {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP874();
		}
		@Override
		public Encoder newEncoder() {
			return new EncoderCP874();
		}
	},
	/**
	 * Windows Japanese.
	 */
	CP932() {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP932();
		}
		@Override
		public Encoder newEncoder() {
			return new EncoderCP932();
		}
	},
	/**
	 * Windows Simplified Chinese.
	 */
	CP936() {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP936();
		}
		@Override
		public Encoder newEncoder() {
			return new EncoderCP936();
		}
	},
	/**
	 * Windows Korean.
	 */
	CP949() {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP949();
		}
		@Override
		public Encoder newEncoder() {
			return new EncoderCP949();
		}
	},
	/**
	 * Windows Traditional Chinese.
	 */
	CP950() {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP950();
		}
		@Override
		public Encoder newEncoder() {
			return new EncoderCP950();
		}
	},
	/**
	 * MS-DOS Portuguese.
	 */
	CP860() {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP860();
		}
		@Override
		public Encoder newEncoder() {
			return new EncoderCP860();
		}
	},
	/**
	 * MS-DOS Nordic.
	 */
	CP865() {
		@Override
		public Decoder newDecoder() {
			return new DecoderCP865();
		}
		@Override
		public Encoder newEncoder() {
			return new EncoderCP865();
		}
	},
	/**
	 * ISO-8859-1, Latin Alphabet No. 1.
	 */
	ISO_8859_1() {
		@Override
		public Decoder newDecoder() {
			return new DecoderISO_8859_1();
		}
		@Override
		public Encoder newEncoder() {
			return new EncoderISO_8859_1();
		}
	};

	public abstract Decoder newDecoder();
	
	public abstract Encoder newEncoder();
}
