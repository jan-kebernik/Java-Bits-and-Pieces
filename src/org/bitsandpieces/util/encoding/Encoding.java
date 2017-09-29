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
			return new SingleByte.DecoderASCII();
		}

		@Override
		public Encoder newEncoder() {
			return new SingleByte.EncoderASCII();
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
			return new SingleByte.DecoderCP1250();
		}

		@Override
		public Encoder newEncoder() {
			return new SingleByte.EncoderCP1250();
		}
	},
	/**
	 * Windows Cyrillic.
	 */
	CP1251() {
		@Override
		public Decoder newDecoder() {
			return new SingleByte.DecoderCP1251();
		}

		@Override
		public Encoder newEncoder() {
			return new SingleByte.EncoderCP1251();
		}
	},
	/**
	 * Windows Latin-1.
	 */
	CP1252() {
		@Override
		public Decoder newDecoder() {
			return new SingleByte.DecoderCP1252();
		}

		@Override
		public Encoder newEncoder() {
			return new SingleByte.EncoderCP1252();
		}
	},
	/**
	 * Windows Greek.
	 */
	CP1253() {
		@Override
		public Decoder newDecoder() {
			return new SingleByte.DecoderCP1253();
		}

		@Override
		public Encoder newEncoder() {
			return new SingleByte.EncoderCP1253();
		}
	},
	/**
	 * Windows Turkish.
	 */
	CP1254() {
		@Override
		public Decoder newDecoder() {
			return new SingleByte.DecoderCP1254();
		}

		@Override
		public Encoder newEncoder() {
			return new SingleByte.EncoderCP1254();
		}
	},
	/**
	 * Windows Hebrew.
	 */
	CP1255() {
		@Override
		public Decoder newDecoder() {
			return new SingleByte.DecoderCP1255();
		}

		@Override
		public Encoder newEncoder() {
			return new SingleByte.EncoderCP1255();
		}
	},
	/**
	 * Windows Arabic.
	 */
	CP1256() {
		@Override
		public Decoder newDecoder() {
			return new SingleByte.DecoderCP1256();
		}

		@Override
		public Encoder newEncoder() {
			return new SingleByte.EncoderCP1256();
		}
	},
	/**
	 * Windows Baltic.
	 */
	CP1257() {
		@Override
		public Decoder newDecoder() {
			return new SingleByte.DecoderCP1257();
		}

		@Override
		public Encoder newEncoder() {
			return new SingleByte.EncoderCP1257();
		}
	},
	/**
	 * Windows Vietnamese.
	 */
	CP1258() {
		@Override
		public Decoder newDecoder() {
			return new SingleByte.DecoderCP1258();
		}

		@Override
		public Encoder newEncoder() {
			return new SingleByte.EncoderCP1258();
		}
	},
	/**
	 * Windows Thai.
	 */
	CP874() {
		@Override
		public Decoder newDecoder() {
			return new SingleByte.DecoderCP874();
		}

		@Override
		public Encoder newEncoder() {
			return new SingleByte.EncoderCP874();
		}
	},
	/**
	 * Windows Japanese.
	 */
	CP932() {
		@Override
		public Decoder newDecoder() {
			return new DualByte.DecoderCP932();
		}

		@Override
		public Encoder newEncoder() {
			return new DualByte.EncoderCP932();
		}
	},
	/**
	 * Windows Simplified Chinese.
	 */
	CP936() {
		@Override
		public Decoder newDecoder() {
			return new DualByte.DecoderCP936();
		}

		@Override
		public Encoder newEncoder() {
			return new DualByte.EncoderCP936();
		}
	},
	/**
	 * Windows Korean.
	 */
	CP949() {
		@Override
		public Decoder newDecoder() {
			return new DualByte.DecoderCP949();
		}

		@Override
		public Encoder newEncoder() {
			return new DualByte.EncoderCP949();
		}
	},
	/**
	 * Windows Traditional Chinese.
	 */
	CP950() {
		@Override
		public Decoder newDecoder() {
			return new DualByte.DecoderCP950();
		}

		@Override
		public Encoder newEncoder() {
			return new DualByte.EncoderCP950();
		}
	},
	/**
	 * MS-DOS Portuguese.
	 */
	CP860() {
		@Override
		public Decoder newDecoder() {
			return new SingleByte.DecoderCP860();
		}

		@Override
		public Encoder newEncoder() {
			return new SingleByte.EncoderCP860();
		}
	},
	/**
	 * MS-DOS Nordic.
	 */
	CP865() {
		@Override
		public Decoder newDecoder() {
			return new SingleByte.DecoderCP865();
		}

		@Override
		public Encoder newEncoder() {
			return new SingleByte.EncoderCP865();
		}
	},
	/**
	 * ISO-8859-1, Latin Alphabet No. 1.
	 */
	ISO_8859_1() {
		@Override
		public Decoder newDecoder() {
			return new SingleByte.DecoderISO_8859_1();
		}

		@Override
		public Encoder newEncoder() {
			return new SingleByte.EncoderISO_8859_1();
		}
	};

	/**
	 * Returns a new {@code Decoder} for this {@code Encoding}.
	 *
	 * @return a new {@code Decoder} for this {@code Encoding}.
	 */
	public abstract Decoder newDecoder();

	/**
	 * Returns a new {@code Encoder} for this {@code Encoding}.
	 *
	 * @return a new {@code Encoder} for this {@code Encoding}.
	 */
	public abstract Encoder newEncoder();
}
