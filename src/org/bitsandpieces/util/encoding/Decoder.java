/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.encoding;

/**
 * Decodes sequences of input bytes into sequences of Unicode code points and
 * Java UTF-16 {@code String}s respectively.
 * <p>
 * How many bytes to decode, whether to stop when errors happen or specific code
 * points are decoded and how many decoded code points to roll back in such
 * cases, is all configurable.</p>
 * <p>
 * Malformed or unmappable input is replaced with a dummy character, '\uFFFD'
 * (U+FFFD) by defualt.</p>
 * <p>
 * Note: Instances implementing this interface are never considered safe for use
 * by multiple concurrent threads.</p>
 *
 * @author Jan Kebernik
 */
public interface Decoder {

	/**
	 * Decodes the specified bytes. The bytes provided are always treated as a
	 * continuation of any currently {@link #pendingInput() pending input}. This
	 * method terminates only when no more input can be processed or when an
	 * installed {@link Parser} or {@link ErrorHandler} causes code points to be
	 * discarded.
	 *
	 * @param buf the array whose bytes to decode
	 * @param off the offset into the array
	 * @param len the number of bytes to decode
	 * @return the number of input bytes discarded:<ul>
	 * <li>&lt; 0: No input was discarded. All input was processed and more
	 * input is expected.</li>
	 * <li>= 0: The input or output limit was reached or 0 code points were
	 * rolled back by request of an intalled {@code Parser} or
	 * {@code ErrorHandler}. It may be necessary to check the respective limits
	 * in order to correctly interpret this result.</li>
	 * <li>&gt; 0: The number of input bytes discarded by request of an
	 * installed {@code Parser} or {@code ErrorHandler}. This number has no
	 * direct relation to the specified array range, and may well be
	 * larger.</li></ul>
	 * @throws IndexOutOfBoundsException if the specified array range is illegal
	 */
	public long decode(byte[] buf, int off, int len);

	// small note: because code points are emitted when pending input is resolved, 
	// (non-manual) discarding of input will only take place when no input is pending.
	/**
	 * The number of input bytes held by this {@code Decoder} as part of the
	 * next code point to decode. These bytes may be
	 * {@link dropPendingInput() discarded manually}.
	 *
	 * @return the number of pending input bytes.
	 */
	public int pendingInput();

	/**
	 * Discards any currently pending input bytes. Note that this may affect
	 * {@link #bytes() byte length}.
	 *
	 * @return this {@code Decoder}
	 */
	public Decoder dropPendingInput();

	/**
	 * Returns the number of Unicode code points that make up the message
	 * decoded so far. Note that code points may be discarded at the end of a
	 * {@link #decode(byte[], int, int) decode()} call.
	 *
	 * @return the number of Unicode code points that make up the message
	 * decoded so far.
	 */
	public int codePoints();

	/**
	 * Returns the number of {@code char}s that make up the message decoded so
	 * far. Note that {@code char}s may be discarded at the end of a
	 * {@link #decode(byte[], int, int) decode()} call as a result of a number
	 * of code points being discarded.
	 *
	 * @return the number of {@code char}s that make up the message decoded so
	 * far.
	 */
	public int chars();

	/**
	 * Returns the number of input bytes that make up the message decoded so
	 * far, including any {@link #pendingInput() pending input bytes}. Note that
	 * input may be discarded at the end of a
	 * {@link #decode(byte[], int, int) decode()} call as a result of a number
	 * of code points being discarded.
	 *
	 * @return the number of input bytes that make up the message decoded so far
	 */
	public long bytes();

	/**
	 * Returns the total amount of Unicode code points decoded as output since
	 * the last {@link #reset() reset()}. This value is not affected when code
	 * points are discarded during {@link #decode(byte[], int, int) decode()}.
	 *
	 * @return the total amount of Unicode code points decoded as output since
	 * the last {@code reset()}.
	 */
	public long codePointsTotal();

	/**
	 * Returns the total amount of {@code char}s decoded as output since the
	 * last {@link #reset() reset()}. This value is not affected when code
	 * points are discarded during {@link #decode(byte[], int, int) decode()}.
	 *
	 * @return the total amount of {@code char}s decoded as output since the
	 * last {@code reset()}.
	 */
	public long charsTotal();

	/**
	 * Returns the total amount of input bytes processed as input since the last
	 * {@link #reset() reset()}. This value is not affected when code points are
	 * discarded during {@link #decode(byte[], int, int) decode()}.
	 *
	 * @return the total amount of input bytes processed as input since the last
	 * {@code reset()}.
	 */
	public long bytesTotal();

	// don't really see an application for this
	/**
	 * Returns the maximum number of Unicode code points to decode.
	 *
	 * @return the maximum number of Unicode code points to decode
	 */
	//public int outputLimit();
	/**
	 * Sets the maximum number of Unicode code points to decode.
	 *
	 * @param limit the maximum number of Unicode code points to decode
	 * @return this Decoder
	 * @throws IllegalArgumentException if the new limit is less than the
	 * current output length.
	 */
	//public Decoder outputLimit(int limit);
	//
	/**
	 * Returns the maximum number of input bytes to process. This limit is
	 * counted against {@link #bytesTotal() bytesTotal()}.
	 *
	 * @return the maximum number of input bytes to process or {@code -1} if no
	 * limit is set.
	 */
	public long inputLimit();

	/**
	 * Sets the maximum number of input bytes to decode. If the {@code limit}
	 * argument is negative, the amount is unlimited.
	 *
	 * @param limit the maximum number of input bytes to decode
	 * @return this Decoder
	 * @throws IllegalArgumentException if the new limit is less than the
	 * current input length.
	 */
	public Decoder inputLimit(long limit);

	/**
	 * Installs a new {@code Parser}.
	 *
	 * @param parser the new {@code Parser} to be installed, or {@code null} if
	 * no {@code Parser} should be used.
	 * @return this {@code Decoder}
	 */
	public Decoder parser(Parser parser);

	/**
	 * Returns the currently installed {@code Parser}, or {@code null} if none
	 * is installed.
	 *
	 * @return the currently installed {@code Parser}
	 */
	public Parser parser();

	/**
	 * Installs a new {@code ErrorHandler}.
	 *
	 * @param errorHandler the new {@code ErrorHandler} to be installed, or
	 * {@code null} if no {@code ErrorHandler} should be used.
	 * @return this {@code Decoder}
	 */
	public Decoder errorHandler(ErrorHandler errorHandler);

	/**
	 * Returns the currently installed {@code ErrorHandler}, or {@code null} if
	 * none is installed.
	 *
	 * @return the currently installed {@code ErrorHandler}
	 */
	public ErrorHandler errorHandler();

	/**
	 * Resets the internal state of this {@code Decoder}. Any statistics
	 * regarding input or output, including pending input, are discarded. Input
	 * limit, output limit, the {@code Parser} and the {@code ErrorHandler} are
	 * not modified. These settings must be changed manually.
	 *
	 * @return this {@code Decoder}
	 */
	public Decoder reset();

	/**
	 * Constructs and returns a new {@code String} containing all currently
	 * decoded code points.
	 *
	 * @return a new {@code String} containing all currently decoded code points
	 */
	public String result();

	/**
	 * Returns this {@code Decoder}'s {@code Encoding}.
	 *
	 * @return this {@code Decoders}'s {@code Encoding}
	 */
	public Encoding encoding();

	/**
	 * The {@link #parser() installed} {@code Parser} will receive newly decoded
	 * code points and is responsible for deciding whether any previous code
	 * points (including the new code point) should be discarded and whether
	 * decoding should stop.
	 * <p>
	 * If no {@code Parser} is installed, no code points will be discarded and
	 * the {@code Decoder} will not stop because of it.</p>
	 * <p>
	 * A custom {@code Parser} {@link #parser(Parser) may be installed}. No
	 * {@code Parser} is intalled by default.</p>
	 */
	@FunctionalInterface
	public static interface Parser {

		/**
		 * Interprets the newly decoded code point and returns the number of
		 * code points to discard.
		 * <p>
		 * If no code points should be discarded and the {@code Decoder} should
		 * continue decoding, this method must return a negative value.
		 * Otherwise, the returned number of code points (possibly {@code 0})
		 * will be discarded and the {@code Decoder} will stop decoding.</p>
		 * <p>
		 * Note that this method must be side-effect free. The state of the
		 * {@code Decoder} is undefined if it is modified from within this
		 * method.</p>
		 *
		 * @param codePoint a newly decoded code point
		 * @param numCodePoints the number of currently decoded code points
		 * (including the new one)
		 * @return the number of code points to discard. This number must not be
		 * greater than the number of currently decoded code points.
		 */
		public int accept(int codePoint, int numCodePoints);
	}

	/**
	 * The {@link errorHandler() installed} {@code ErrorHandler} will be invoked
	 * whenever a code point could not be decoded, either because it is
	 * unmappable or because of malformed input. It is responsible for deciding
	 * whether any previous code points (including the new code point, which in
	 * this case is always a dummny error code point) should be discarded and
	 * whether decoding should stop.
	 * <p>
	 * If no {@code ErrorHandler} is installed, no code points will be discarded
	 * and the {@code Decoder} will not stop because of it.</p>
	 * <p>
	 * A custom {@code ErrorHandler}
	 * {@link #errorHandler(ErrorHandler) may be installed}. No
	 * {@code ErrorHandler} is intalled by default.</p>
	 */
	@FunctionalInterface
	public static interface ErrorHandler {

		/**
		 * Determines whether previous code points should be discarded.
		 * <p>
		 * If no code points should be discarded and the {@code Decoder} should
		 * continue decoding, this method must return a negative value.
		 * Otherwise, the returned number of code points (possibly {@code 0})
		 * will be discarded and the {@code Decoder} will stop decoding.</p>
		 * <p>
		 * Note that this method must be side-effect free. The state of the
		 * {@code Decoder} is undefined if it is modified from within this
		 * method.</p>
		 *
		 * @param numCodePoints the number of currently decoded code points
		 * (including the new dummy code point)
		 * @return the number of code points to discard. This number must not be
		 * greater than the number of currently decoded code points.
		 */
		public int apply(int numCodePoints);
	}

	public static enum Terminator implements Parser {

		NUL {
			@Override
			public int accept(int codePoint, int numCodePoints) {
				return codePoint == 0 ? 1 : -1;
			}
		},
		WHITESPACE {
			@Override
			public int accept(int codePoint, int numCodePoints) {
				return Character.isWhitespace(codePoint) ? 1 : -1;
			}
		},
		NON_WHITESPACE {
			@Override
			public int accept(int codePoint, int numCodePoints) {
				return !Character.isWhitespace(codePoint) ? 1 : -1;
			}
		},
		NON_ALPHABETIC {
			@Override
			public int accept(int codePoint, int numCodePoints) {
				return !Character.isAlphabetic(codePoint) ? 1 : -1;
			}
		},
		NON_ALPHANUMERIC {
			@Override
			public int accept(int codePoint, int numCodePoints) {
				return (!Character.isAlphabetic(codePoint) && !Character.isDigit(codePoint)) ? 1 : -1;
			}
		};
	}
}
