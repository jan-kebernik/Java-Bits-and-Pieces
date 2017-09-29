/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * A {@code LineScanner} allows for any (immutable) {@code CharSequence} to be
 * parsed as single lines denoted by {@code '\r'}, {@code '\n'},
 * {@code "\r\n"} or the end of the input sequence. Every input sequence is thus
 * considered to always contain at least one line, even if empty. The
 * line-separators are not part of the {@code String}s returned by this class.
 *
 * <p>
 * This class fully implements an efficient {@code Spliterator}. Due to the
 * nature of the task, the spliterator's size estimate is always
 * {@code Long.MAX_VALUE}, indicating that the cost of calculating a precise
 * measure makes doing so impractical. It also reports {@code ORDERED} and
 * {@code IMMUTABLE}.
 *
 * @author Jan Kebernik
 */
public class LineScanner implements Iterable<String> {

	private final CharSequence src;
	private final int start, end;

	/**
	 * Creates a new {@code LineScanner} for the specified input
	 * {@code CharSequence}.
	 *
	 * @param input the {@code CharSequence} to be scanned.
	 */
	public LineScanner(CharSequence input) {
		this.src = input;
		this.end = input.length();
		this.start = 0;
	}

	/**
	 * Creates a new {@code LineScanner} for the specified input
	 * {@code CharSequence}.
	 *
	 * @param input the {@code CharSequence} to be scanned.
	 * @param start start index (inclusive) of the input {@code CharSequence}.
	 * @param end the end index (exclusive) of the input {@code CharSequence}.
	 * @throws IndexOutOfBoundsException if {@code start} is negative, or
	 * {@code end} is larger than the length of the input {@code CharSequence},
	 * or {@code start} is larger than {@code end}.
	 */
	public LineScanner(CharSequence input, int start, int end) {
		int len = end - start;
		if (start < 0 || len < 0 || start > input.length() - len) {
			throw new IndexOutOfBoundsException();
		}
		this.src = input;
		this.start = start;
		this.end = end;
	}

	/**
	 * The input {@code CharSequence}.
	 *
	 * @return the input {@code CharSequence}.
	 */
	public CharSequence input() {
		return this.src;
	}

	/**
	 * The start index (inclusive) of the input {@code CharSequence}.
	 *
	 * @return the start index (inclusive) of the input {@code CharSequence}.
	 */
	public int inputStart() {
		return this.start;
	}

	/**
	 * The end index (exclusive) of the input {@code CharSequence}.
	 *
	 * @return the end index (exclusive) of the input {@code CharSequence}.
	 */
	public int inputEnd() {
		return this.end;
	}

	@Override
	public Iterator<String> iterator() {
		return new LineIterator(this.src, this.start, this.end);
	}

	@Override
	public Spliterator<String> spliterator() {
		return new LineSpliterator(this.src, this.start, this.end);
	}

	@Override
	public void forEach(Consumer<? super String> action) {
		Objects.requireNonNull(action);
		int _off = this.start;
		boolean _r = false;
		for (int _idx = this.start; _idx < this.end; _idx++) {
			switch (this.src.charAt(_idx)) {
				case '\r':
					_r = true;
					break;
				case '\n':
					if (!_r) {
						break;
					}
					_off++;	// skip \n
				// FALL-THROUGH
				default:
					_r = false;
					continue;
			}
			action.accept(this.src.subSequence(_off, _idx).toString());
			_off = _idx + 1;
		}
		action.accept(this.src.subSequence(_off, this.end).toString());
	}

	private static class LineItrBase {

		int off;
		int idx;
		boolean r;
		final int end;
		final CharSequence src;

		LineItrBase(CharSequence src, int start, int end) {
			this.src = src;
			this.end = end;
			this.off = this.idx = start;
		}
	}

	private static final class LineIterator extends LineItrBase implements Iterator<String> {

		private LineIterator(CharSequence src, int start, int end) {
			super(src, start, end);
		}

		@Override
		public boolean hasNext() {
			return this.idx >= 0;
		}

		@Override
		public void forEachRemaining(Consumer<? super String> action) {
			Objects.requireNonNull(action);
			int _idx = this.idx;
			if (_idx < 0) {
				return;
			}
			if (_idx == this.end) {
				this.idx = -1;
				action.accept("");	// empty line
				return;
			}
			boolean _r = this.r;
			int _off = this.off;
			try {
				for (; _idx < this.end; _idx++) {
					switch (this.src.charAt(_idx)) {
						case '\r':
							_r = true;
							break;
						case '\n':
							if (!_r) {
								break;
							}
							_off++;	// skip \n
						// FALL-THROUGH
						default:
							_r = false;
							continue;
					}
					action.accept(this.src.subSequence(_off, _idx).toString());
					_off = _idx + 1;
				}
				_idx = -1;
				action.accept(this.src.subSequence(_off, this.end).toString());
			} finally {
				this.r = _r;
				this.idx = _idx;
				this.off = _off;
			}
		}

		@Override
		public String next() {
			int _idx = this.idx;
			if (_idx < 0) {
				// all lines processed
				throw new NoSuchElementException();
			}
			if (_idx == this.end) {
				// empty last line (input ends on \r or \n)
				this.idx = -1;
				return "";	// empty line
			}
			boolean _r = this.r;
			int _off = this.off;
			try {
				for (; _idx < this.end; _idx++) {
					switch (this.src.charAt(_idx)) {
						case '\r':
							_r = true;
							break;
						case '\n':
							if (!_r) {
								break;
							}
							_off++;	// skip \n
						// FALL-THROUGH
						default:
							_r = false;
							continue;
					}
					String s = this.src.subSequence(_off, _idx).toString();
					_off = ++_idx;
					return s;
				}
				_idx = -1;
				return this.src.subSequence(_off, this.end).toString();
			} finally {
				this.r = _r;
				this.idx = _idx;
				this.off = _off;
			}
		}
	}

	private static final class LineSpliterator extends LineItrBase implements Spliterator<String> {

		public LineSpliterator(CharSequence src, int start, int end) {
			super(src, start, end);
		}

		@Override
		public boolean tryAdvance(Consumer<? super String> action) {
			Objects.requireNonNull(action);
			int _idx = this.idx;
			if (_idx < 0) {
				return false;
			}
			if (_idx == this.end) {
				this.idx = -1;
				action.accept("");
				return true;
			}
			boolean _r = this.r;
			int _off = this.off;
			try {
				for (; _idx < this.end; _idx++) {
					switch (this.src.charAt(_idx)) {
						case '\r':
							_r = true;
							break;
						case '\n':
							if (!_r) {
								break;
							}
							_off++;	// skip \n
						// FALL-THROUGH
						default:
							_r = false;
							continue;
					}
					action.accept(this.src.subSequence(_off, _idx).toString());
					_off = ++_idx;
					return true;
				}
				_idx = -1;
				action.accept(this.src.subSequence(_off, this.end).toString());
				return true;
			} finally {
				this.r = _r;
				this.idx = _idx;
				this.off = _off;
			}
		}

		@Override
		public Spliterator<String> trySplit() {
			// find the middle of the remaining source sequence
			// search left and right for end-of-line,
			// return spliterator left of nearest separator, while this spliterator skips it
			int _idx = this.idx;
			if (_idx < 0 || _idx == this.end) {
				// no additional lines available
				return null;
			}
			// start searching from the middle of this sequence
			int pivot = _idx + ((this.end - _idx) >>> 1);
			switch (this.src.charAt(pivot)) {
				case '\r':
					// skip over \r or \r\n
					this.r = false;
					this.off = this.idx
							= (++pivot < this.end && this.src.charAt(pivot) == '\n')
							? pivot + 1 : pivot;
					// return split left of \r
					return new LineSpliterator(this.src, _idx, pivot - 1);
				case '\n':
					// skip over \n
					this.r = false;
					this.off = this.idx = pivot + 1;
					// return split left of \n or \r\n
					return new LineSpliterator(this.src, _idx,
							(--pivot >= _idx && this.src.charAt(pivot) == '\r')
							? pivot : pivot + 1);
				default: {
					// find next \r or \n
					for (int i = pivot + 1; i < this.end;) {
						switch (this.src.charAt(i++)) {
							case '\r':
								if (i < this.end && this.src.charAt(i) == '\n') {
									// \r\n combo
									this.r = false;
									this.off = this.idx = i + 1;
									return new LineSpliterator(this.src, _idx, i - 1);
								} //	single \r
							// FALL-THROUGH
							case '\n':
								this.r = false;
								this.off = this.idx = i;
								return new LineSpliterator(this.src, _idx, i - 1);
							default:
								break;
						}
					}
					// pivot part of last line
					// find previous \r or \n
					for (int i = pivot - 1; i >= _idx;) {
						switch (this.src.charAt(i--)) {
							case '\n':
								if (i >= _idx && this.src.charAt(i) == '\r') {
									// \r\n combo						
									this.r = false;
									this.off = this.idx = i + 2;
									return new LineSpliterator(this.src, _idx, i);
								} //	single \n
							// FALL-THROUGH
							case '\r':
								this.r = false;
								this.off = this.idx = i + 2;
								return new LineSpliterator(this.src, _idx, i + 1);
							default:
								break;
						}
					}
					// remaining range is a single line
					// cannot split
					return null;
				}
			}
		}

		@Override
		public long estimateSize() {
			// too expensive to calculate
			return Long.MAX_VALUE;
		}

		@Override
		public int characteristics() {
			return Spliterator.ORDERED | Spliterator.IMMUTABLE;
		}
	}
}
