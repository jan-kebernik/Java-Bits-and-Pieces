/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.encoding;

/**
 * {@code Encoder}s are objects that encode UTF-16 {@code char}s to
 * {@code byte}s.
 * <p>
 * Both input and output are provided purely by the calling classes, making
 * {@code Encoder}s very light-weight. The calling class is free to deal with
 * errors as they are encountered.
 * </p>
 *
 * @author Jan Kebernik
 */
public interface Encoder {

	/**
	 * Returns this {@code Encoder}'s {@link Encoding}.
	 *
	 * @return this {@code Encoder}'s {@code Encoding}.
	 */
	Encoding encoding();

	/**
	 * Encodes the current input into the specified array.
	 * <p>
	 * The method will terminate when all available input is processed, when no
	 * more output can be produced or when an error occurs. Partial code points
	 * may need to be stored by this {@code Encoder} until more input is made
	 * available or more output can be stored.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of 0. This ensures that no pending errors are
	 * swallowed.
	 *
	 * @param dest the array into which to encode.
	 * @param off the offset into the array.
	 * @return <ul><li>&gt 0: The actual number of {@code byte}s stored.</li>
	 * <li>== 0: Indicates that no further work can be done by this
	 * {@code Encoder} until more input or output is provided. If
	 * {@link #needsInput() needsInput()} reports a non-zero value, more input
	 * is required to resolve the current code point. If
	 * {@link #pendingOutput() pendingOutput()} reports a non-zero value, then
	 * the provided array range did not provide enough space to fully store all
	 * input.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code char}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code byte}s were produced.
	 * @throws NullPointerException if {@code dest == null}.
	 * @throws IndexOutOfBoundsException if {@code off < 0} or
	 * {@code off > dest.length}.
	 */
	int encode(byte[] dest, int off);

	/**
	 * Encodes the current input into the specified array.
	 * <p>
	 * The method will terminate when all available input is processed, when no
	 * more output can be produced or when an error occurs. Partial code points
	 * may need to be stored by this {@code Encoder} until more input is made
	 * available or more output can be stored.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of 0. This ensures that no pending errors are
	 * swallowed.
	 *
	 * @param inputChars the desired number of {@code char}s to consume. Must be
	 * less than or equal to {@link #inputRemaining() inputRemaining()}.
	 * @param dest the array into which to encode.
	 * @param off the offset into the array.
	 * @return <ul><li>&gt 0: The actual number of {@code byte}s stored.</li>
	 * <li>== 0: Indicates that no further work can be done by this
	 * {@code Encoder} until more input or output is provided. If
	 * {@link #needsInput() needsInput()} reports a non-zero value, more input
	 * is required to resolve the current code point. If
	 * {@link #pendingOutput() pendingOutput()} reports a non-zero value, then
	 * the provided array range did not provide enough space to fully store all
	 * input.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code char}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code byte}s were produced.
	 * @throws NullPointerException if {@code dest == null}.
	 * @throws IllegalArgumentException if {@code inputChars < 0} or
	 * {@code inputChars > inputRemaining()}.
	 * @throws IndexOutOfBoundsException if {@code off < 0} or
	 * {@code off > dest.length}.
	 */
	int encode(int inputChars, byte[] dest, int off);

	/**
	 * Encodes the current input into the specified array.
	 * <p>
	 * The method will terminate when all available input is processed, when no
	 * more output can be produced or when an error occurs. Partial code points
	 * may need to be stored by this {@code Encoder} until more input is made
	 * available or more output can be stored.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of 0. This ensures that no pending errors are
	 * swallowed.
	 *
	 * @param dest the array into which to encode.
	 * @param off the offset into the array.
	 * @param maxBytes the maximum number of {@code byte}s to store.
	 * @param maxCodePoints the maximum number of code points to resolve.
	 * @return <ul><li>&gt 0: The actual number of {@code byte}s stored.</li>
	 * <li>== 0: Indicates that no further work can be done by this
	 * {@code Encoder} until more input or output is provided. If
	 * {@link #needsInput() needsInput()} reports a non-zero value, more input
	 * is required to resolve the current code point. If
	 * {@link #pendingOutput() pendingOutput()} reports a non-zero value, then
	 * the provided array range did not provide enough space to fully store all
	 * input.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code char}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code byte}s were produced.
	 * @throws NullPointerException if {@code dest == null}.
	 * @throws IllegalArgumentException if {@code inputChars < 0}, if
	 * {@code inputChars > inputRemaining()}, if {@code maxBytes < 0} or if
	 * {@code maxCodePoints < 0}.
	 * @throws IndexOutOfBoundsException if {@code off < 0} or
	 * {@code off > dest.length}.
	 */
	int encode(byte[] dest, int off, int maxBytes, int maxCodePoints);

	/**
	 * Encodes the current input into the specified array.
	 * <p>
	 * The method will terminate when all available input is processed, when no
	 * more output can be produced or when an error occurs. Partial code points
	 * may need to be stored by this {@code Encoder} until more input is made
	 * available or more output can be stored.
	 *
	 * @apiNote All decoding and encoding methods are only truly complete once
	 * they return a value of 0. This ensures that no pending errors are
	 * swallowed.
	 *
	 * @param inputChars the desired number of {@code char}s to consume. Must be
	 * less than or equal to {@link #inputRemaining() inputRemaining()}.
	 * @param dest the array into which to encode.
	 * @param off the offset into the array.
	 * @param maxBytes the maximum number of {@code byte}s to store.
	 * @param maxCodePoints the maximum number of code points to resolve.
	 * @return <ul><li>&gt 0: The actual number of {@code byte}s stored.</li>
	 * <li>== 0: Indicates that no further work can be done by this
	 * {@code Encoder} until more input or output is provided. If
	 * {@link #needsInput() needsInput()} reports a non-zero value, more input
	 * is required to resolve the current code point. If
	 * {@link #pendingOutput() pendingOutput()} reports a non-zero value, then
	 * the provided array range did not provide enough space to fully store all
	 * input.</li>
	 * <li>&lt 0: Indicates that the last code point could not be resolved due
	 * to malformed or unmappable input, and returns 0 minus the number of
	 * {@code char}s forming the unresolvable code point.</li></ul>
	 * A negative result always implies that no {@code byte}s were produced.
	 * @throws NullPointerException if {@code dest == null}.
	 * @throws IllegalArgumentException if {@code inputChars < 0}, if
	 * {@code inputChars > inputRemaining()}, if {@code maxBytes < 0} or if
	 * {@code maxCodePoints < 0}.
	 * @throws IndexOutOfBoundsException if {@code off < 0} or
	 * {@code off > dest.length}.
	 */
	int encode(int inputChars, byte[] dest, int off, int maxBytes, int maxCodePoints);

	/**
	 * Instructs this {@code Encoder} to use the specified {@code char}s as
	 * input. The current input is discarded. Pending input is unaffected.
	 *
	 * @param src the {@code CharSequence} whose {@code char}s to use as input.
	 * @return this {@code Encoder}.
	 * @throws NullPointerException if {@code src} is {@code null}
	 */
	default Encoder setInput(CharSequence src) {
		return setInput(src, 0, src.length());
	}

	/**
	 * Instructs this {@code Encoder} to use the specified {@code char}s as
	 * input. The current input is discarded. Pending input is unaffected.
	 *
	 * @param src the {@code CharSequence} whose {@code char}s to use as input.
	 * @param off the offset into the {@code CharSequence}.
	 * @param len the number of {@code char}s to use.
	 * @return this {@code Encoder}.
	 * @throws NullPointerException if {@code src} is {@code null}
	 * @throws IndexOutOfBoundsException if the specified range is illegal
	 */
	Encoder setInput(CharSequence src, int off, int len);

	/**
	 * Instructs this {@code Encoder} to use the specified {@code char}s as
	 * input. The current input is discarded. Pending input is unaffected.
	 *
	 * @param src the array whose {@code char}s to use as input.
	 * @return this {@code Encoder}.
	 * @throws NullPointerException if {@code src} is {@code null}
	 */
	default Encoder setInput(char[] src) {
		return setInput(src, 0, src.length);
	}

	/**
	 * Instructs this {@code Encoder} to use the specified {@code char}s as
	 * input. The current input is discarded. Pending input is unaffected.
	 *
	 * @param src the array whose {@code char}s to use as input.
	 * @param off the offset into the char-array.
	 * @param len the number of {@code char}s to use.
	 * @return this {@code Encoder}.
	 * @throws NullPointerException if {@code src} is {@code null}
	 * @throws IndexOutOfBoundsException if the specified range is illegal
	 */
	Encoder setInput(char[] src, int off, int len);

	/**
	 * Returns the number of input {@code char}s this {@code Encoder} currently
	 * has access to. This does <em>NOT</em> include any
	 * {@link #pendingInput() pending input}.
	 *
	 * @return the number of input {@code char}s this {@code Encoder} currently
	 * has access to.
	 */
	int inputRemaining();

	/**
	 * Returns the number of additional input {@code char}s required to fully
	 * resolve the code point currently being encoded.
	 *
	 * @return the number of additional input {@code char}s required to fully
	 * resolve the code point currently being encoded.
	 */
	int needsInput();

	/**
	 * Returns the number of input {@code char}s held by this {@code Encoder} as
	 * part of the code point currently being encoded. These {@code char}s are
	 * considered to be part of the value returned by
	 * {@link #charsConsumed() charsConsumed()}, either until they are
	 * {@link #dropPending() dropped} or until they are resolved in a subsequent
	 * {@code encode()} call.
	 * <p>
	 * Specifically, returns {@code 1} if the high surrogate of a <em>valid</em>
	 * supplementary code point is currently being held by this {@code Encoder}
	 * until more input is consumed in a subsequent {@code encode()} call,
	 * returning {@code 0} in all other cases.
	 *
	 * @return the number of input {@code char}s held by this {@code Encoder} as
	 * part of the code point currently being encoded.
	 */
	int pendingInput();

	/**
	 * Returns the number of output {@code byte}s held by this {@code Encoder}
	 * as part of the code point currently being encoded.
	 *
	 * @return the number of output {@code byte}s held by this {@code Encoder}
	 * as part of the code point currently being encoded.
	 */
	int pendingOutput();

	/**
	 * Returns the number of <em>valid</em> {@code byte}s produced since the
	 * last {@link #reset() reset}. {@code byte}s that are part of malformed or
	 * unmappable code points are not counted this way.
	 *
	 * @return the number of <em>valid</em> {@code byte}s produced since the
	 * last {@code reset}.
	 */
	long bytesProduced();

	/**
	 * Returns the number of input {@code char}s consumed since the last
	 * {@link #reset() reset}. Any pending input is part of this count,
	 * {@link #dropPending() dropping} it will be reflected in the value
	 * returned by this method.
	 *
	 * @return the number of input {@code char}s consumed since the last
	 * {@link #reset() reset}.
	 */
	long charsConsumed();

	/**
	 * Returns the number of <em>valid</em> code points resolved since the last
	 * {@link #reset() reset}. Malformed or unmappable code points are not
	 * counted this way. Code points are only considered resolved once they have
	 * been fully encoded.
	 *
	 * @return the number of <em>valid</em> code points resolved since the last
	 * {@code reset}.
	 */
	long codePointsResolved();

	/**
	 * Causes this {@code Encoder} to discard its current input. Pending input
	 * is <em>unaffacted</em> by this operation.
	 *
	 * @return this {@code Encoder}.
	 */
	Encoder dropInput();

	/**
	 * Causes this {@code Encoder} to discard its pending input or output (only
	 * one of which can be held at a time).
	 * <p>
	 * If any pending input is discarded, the value returned by
	 * {@link #charsConsumed() charsConsumed()} will be decremented by the
	 * number of currently pending input {@code char}s.
	 *
	 * @return this {@code Encoder}.
	 */
	Encoder dropPending();

	/**
	 * Fully resets this {@code Encoder} to its initial state.
	 *
	 * @return this {@code Encoder}.
	 */
	Encoder reset();

	/**
	 * Returns {@code true} if this {@code Encoder} has any input or output
	 * pending.
	 *
	 * @return {@code true} if this {@code Encoder} has any input or output
	 * pending.
	 */
	boolean hasPending();
}
