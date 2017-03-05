/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util;

import java.util.List;
import org.bitsandpieces.util.collection.primitive.PrimitiveList.IntList;

/**
 * Boyer-Moore-Hoorspool exact string matching. Adapted to use work tables of
 * size 256 instead of 65536.
 *
 * @author Jan Kebernik
 */
final class Horspool {

	private Horspool() {
	}

	private static int transformBackward(char c, int index) {
		return (c >>> ((index & 1) << 3)) & 0xff;
	}

	private static int getByteBackward(char[] chars, int index) {
		return transformBackward(chars[index >>> 1], index);
	}

	private static int getByteBackward(CharSequence chars, int index) {
		return transformBackward(chars.charAt(index >>> 1), index);
	}

	private static int transformForward(char c, int index) {
		return (c >>> (((index & 1) ^ 1) << 3)) & 0xff;
	}

	private static int getByteForward(char[] chars, int index) {
		return transformForward(chars[index >>> 1], index);
	}

	private static int getByteForward(CharSequence chars, int index) {
		return transformForward(chars.charAt(index >>> 1), index);
	}

	final static List<Integer> backward(char[] haystack, int hoff, int hlen, char[] needle, int noff, int nlen, int[] work, IntList results) {
		// needle- and haystack-bytes are simply considered in reverse order
		// pre-process needle
		int nMaxIdx = noff + nlen - 1;
		int nBytes = nlen << 1;
		int hBytes = hlen << 1;
		int hByteOff = hoff << 1;
		int nByteOff = noff << 1;
		int nMaxByteIdx = nBytes - 1;
		int hMaxByteIdx = hByteOff + hBytes - 1;
		for (int i = nMaxIdx, j = 0; i > noff; i--) {
			int c = needle[i];
			work[c >>> 8] = ++j;
			work[c & 0xff] = ++j;
		}
		work[needle[0] >>> 8] = nMaxByteIdx;
		// search
		for (int max = hBytes - nBytes, i = 0; i <= max;) {
			int j = nMaxByteIdx;
			while (j >= 0 && getByteBackward(needle, (nByteOff + nMaxByteIdx - j)) == getByteBackward(haystack, hMaxByteIdx - i - j)) {
				j--;
			}
			if (j < 0) {
				results.add(((hMaxByteIdx - i) >>> 1) - (nlen - 1));
			}
			i += nMaxByteIdx;
			i -= work[getByteBackward(haystack, hMaxByteIdx - i)] - 1;
		}
		return results;
	}

	final static List<Integer> backward(char[] haystack, int hoff, int hlen, CharSequence needle, int noff, int nlen, int[] work, IntList results) {
		// needle- and haystack-bytes are simply considered in reverse order
		// pre-process needle
		int nMaxIdx = noff + nlen - 1;
		int nBytes = nlen << 1;
		int hBytes = hlen << 1;
		int hByteOff = hoff << 1;
		int nByteOff = noff << 1;
		int nMaxByteIdx = nBytes - 1;
		int hMaxByteIdx = hByteOff + hBytes - 1;
		for (int i = nMaxIdx, j = 0; i > noff; i--) {
			int c = needle.charAt(i);
			work[c >>> 8] = ++j;
			work[c & 0xff] = ++j;
		}
		work[needle.charAt(0) >>> 8] = nMaxByteIdx;
		// search
		for (int max = hBytes - nBytes, i = 0; i <= max;) {
			int j = nMaxByteIdx;
			while (j >= 0 && getByteBackward(needle, (nByteOff + nMaxByteIdx - j)) == getByteBackward(haystack, hMaxByteIdx - i - j)) {
				j--;
			}
			if (j < 0) {
				results.add(((hMaxByteIdx - i) >>> 1) - (nlen - 1));
			}
			i += nMaxByteIdx;
			i -= work[getByteBackward(haystack, hMaxByteIdx - i)] - 1;
		}
		return results;
	}

	final static List<Integer> backward(CharSequence haystack, int hoff, int hlen, char[] needle, int noff, int nlen, int[] work, IntList results) {
		// needle- and haystack-bytes are simply considered in reverse order
		// pre-process needle
		int nMaxIdx = noff + nlen - 1;
		int nBytes = nlen << 1;
		int hBytes = hlen << 1;
		int hByteOff = hoff << 1;
		int nByteOff = noff << 1;
		int nMaxByteIdx = nBytes - 1;
		int hMaxByteIdx = hByteOff + hBytes - 1;
		for (int i = nMaxIdx, j = 0; i > noff; i--) {
			int c = needle[i];
			work[c >>> 8] = ++j;
			work[c & 0xff] = ++j;
		}
		work[needle[0] >>> 8] = nMaxByteIdx;
		// search
		for (int max = hBytes - nBytes, i = 0; i <= max;) {
			int j = nMaxByteIdx;
			while (j >= 0 && getByteBackward(needle, (nByteOff + nMaxByteIdx - j)) == getByteBackward(haystack, hMaxByteIdx - i - j)) {
				j--;
			}
			if (j < 0) {
				results.add(((hMaxByteIdx - i) >>> 1) - (nlen - 1));
			}
			i += nMaxByteIdx;
			i -= work[getByteBackward(haystack, hMaxByteIdx - i)] - 1;
		}
		return results;
	}

	final static List<Integer> backward(CharSequence haystack, int hoff, int hlen, CharSequence needle, int noff, int nlen, int[] work, IntList results) {
		// needle- and haystack-bytes are simply considered in reverse order
		// pre-process needle
		int nMaxIdx = noff + nlen - 1;
		int nBytes = nlen << 1;
		int hBytes = hlen << 1;
		int hByteOff = hoff << 1;
		int nByteOff = noff << 1;
		int nMaxByteIdx = nBytes - 1;
		int hMaxByteIdx = hByteOff + hBytes - 1;
		for (int i = nMaxIdx, j = 0; i > noff; i--) {
			int c = needle.charAt(i);
			work[c >>> 8] = ++j;
			work[c & 0xff] = ++j;
		}
		work[needle.charAt(0) >>> 8] = nMaxByteIdx;
		// search
		for (int max = hBytes - nBytes, i = 0; i <= max;) {
			int j = nMaxByteIdx;
			while (j >= 0 && getByteBackward(needle, (nByteOff + nMaxByteIdx - j)) == getByteBackward(haystack, hMaxByteIdx - i - j)) {
				j--;
			}
			if (j < 0) {
				results.add(((hMaxByteIdx - i) >>> 1) - (nlen - 1));
			}
			i += nMaxByteIdx;
			i -= work[getByteBackward(haystack, hMaxByteIdx - i)] - 1;
		}
		return results;
	}

	// returns indices relative to hoff
	final static List<Integer> forward(char[] haystack, int hoff, int hlen, char[] needle, int noff, int nlen, int[] work, IntList results) {
		// pre-process needle
		int nMaxIdx = noff + nlen - 1;
		int nBytes = nlen << 1;
		int hBytes = hlen << 1;
		int hByteOff = hoff << 1;
		int nByteOff = noff << 1;
		int nMaxByteIdx = nBytes - 1;
		for (int i = noff, j = 0; i < nMaxIdx; i++) {
			int c = needle[i];
			work[c >>> 8] = ++j;
			work[c & 0xff] = ++j;
		}
		work[needle[nMaxIdx] >>> 8] = nMaxByteIdx;
		// search
		for (int max = hByteOff + hBytes - nBytes, i = hByteOff; i <= max;) {
			int j = nMaxByteIdx;
			while (j >= 0 && getByteForward(needle, nByteOff + j) == getByteForward(haystack, i + j)) {
				j--;
			}
			if (j < 0) {
				results.add(i >>> 1);
			}
			i += nMaxByteIdx;
			i -= work[getByteForward(haystack, i)] - 1;
		}
		return results;
	}

	// returns indices relative to hoff
	final static List<Integer> forward(char[] haystack, int hoff, int hlen, CharSequence needle, int noff, int nlen, int[] work, IntList results) {
		// pre-process needle
		int nMaxIdx = noff + nlen - 1;
		int nBytes = nlen << 1;
		int hBytes = hlen << 1;
		int hByteOff = hoff << 1;
		int nByteOff = noff << 1;
		int nMaxByteIdx = nBytes - 1;
		for (int i = noff, j = 0; i < nMaxIdx; i++) {
			int c = needle.charAt(i);
			work[c >>> 8] = ++j;
			work[c & 0xff] = ++j;
		}
		work[needle.charAt(nMaxIdx) >>> 8] = nMaxByteIdx;
		// search
		for (int max = hByteOff + hBytes - nBytes, i = hByteOff; i <= max;) {
			int j = nMaxByteIdx;
			while (j >= 0 && getByteForward(needle, nByteOff + j) == getByteForward(haystack, i + j)) {
				j--;
			}
			if (j < 0) {
				results.add(i >>> 1);
			}
			i += nMaxByteIdx;
			i -= work[getByteForward(haystack, i)] - 1;
		}
		return results;
	}

	// returns indices relative to hoff
	final static List<Integer> forward(CharSequence haystack, int hoff, int hlen, char[] needle, int noff, int nlen, int[] work, IntList results) {
		// pre-process needle
		int nMaxIdx = noff + nlen - 1;
		int nBytes = nlen << 1;
		int hBytes = hlen << 1;
		int hByteOff = hoff << 1;
		int nByteOff = noff << 1;
		int nMaxByteIdx = nBytes - 1;
		for (int i = noff, j = 0; i < nMaxIdx; i++) {
			int c = needle[i];
			work[c >>> 8] = ++j;
			work[c & 0xff] = ++j;
		}
		work[needle[nMaxIdx] >>> 8] = nMaxByteIdx;
		// search
		for (int max = hByteOff + hBytes - nBytes, i = hByteOff; i <= max;) {
			int j = nMaxByteIdx;
			while (j >= 0 && getByteForward(needle, nByteOff + j) == getByteForward(haystack, i + j)) {
				j--;
			}
			if (j < 0) {
				results.add(i >>> 1);
			}
			i += nMaxByteIdx;
			i -= work[getByteForward(haystack, i)] - 1;
		}
		return results;
	}

	// returns indices relative to hoff
	final static List<Integer> forward(CharSequence haystack, int hoff, int hlen, CharSequence needle, int noff, int nlen, int[] work, IntList results) {
		// pre-process needle
		int nMaxIdx = noff + nlen - 1;
		int nBytes = nlen << 1;
		int hBytes = hlen << 1;
		int hByteOff = hoff << 1;
		int nByteOff = noff << 1;
		int nMaxByteIdx = nBytes - 1;
		for (int i = noff, j = 0; i < nMaxIdx; i++) {
			int c = needle.charAt(i);
			work[c >>> 8] = ++j;
			work[c & 0xff] = ++j;
		}
		work[needle.charAt(nMaxIdx) >>> 8] = nMaxByteIdx;
		// search
		for (int max = hByteOff + hBytes - nBytes, i = hByteOff; i <= max;) {
			int j = nMaxByteIdx;
			while (j >= 0 && getByteForward(needle, nByteOff + j) == getByteForward(haystack, i + j)) {
				j--;
			}
			if (j < 0) {
				results.add(i >>> 1);
			}
			i += nMaxByteIdx;
			i -= work[getByteForward(haystack, i)] - 1;
		}
		return results;
	}

	final static int lastIndexOf(char[] haystack, int hoff, int hlen, char[] needle, int noff, int nlen, int[] work) {
		// needle- and haystack-bytes are simply considered in reverse order
		// pre-process needle
		int nMaxIdx = noff + nlen - 1;
		int nBytes = nlen << 1;
		int hBytes = hlen << 1;
		int hByteOff = hoff << 1;
		int nByteOff = noff << 1;
		int nMaxByteIdx = nBytes - 1;
		int hMaxByteIdx = hByteOff + hBytes - 1;
		for (int i = nMaxIdx, j = 0; i > noff; i--) {
			int c = needle[i];
			work[c >>> 8] = ++j;
			work[c & 0xff] = ++j;
		}
		work[needle[0] >>> 8] = nMaxByteIdx;
		// search
		for (int max = hBytes - nBytes, i = 0; i <= max;) {
			int j = nMaxByteIdx;
			while (j >= 0 && getByteBackward(needle, (nByteOff + nMaxByteIdx - j)) == getByteBackward(haystack, hMaxByteIdx - i - j)) {
				j--;
			}
			if (j < 0) {
				return ((hMaxByteIdx - i) >>> 1) - (nlen - 1);
			}
			i += nMaxByteIdx;
			i -= work[getByteBackward(haystack, hMaxByteIdx - i)] - 1;
		}
		return -1;
	}

	final static int lastIndexOf(char[] haystack, int hoff, int hlen, CharSequence needle, int noff, int nlen, int[] work) {
		// needle- and haystack-bytes are simply considered in reverse order
		// pre-process needle
		int nMaxIdx = noff + nlen - 1;
		int nBytes = nlen << 1;
		int hBytes = hlen << 1;
		int hByteOff = hoff << 1;
		int nByteOff = noff << 1;
		int nMaxByteIdx = nBytes - 1;
		int hMaxByteIdx = hByteOff + hBytes - 1;
		for (int i = nMaxIdx, j = 0; i > noff; i--) {
			int c = needle.charAt(i);
			work[c >>> 8] = ++j;
			work[c & 0xff] = ++j;
		}
		work[needle.charAt(0) >>> 8] = nMaxByteIdx;
		// search
		for (int max = hBytes - nBytes, i = 0; i <= max;) {
			int j = nMaxByteIdx;
			while (j >= 0 && getByteBackward(needle, (nByteOff + nMaxByteIdx - j)) == getByteBackward(haystack, hMaxByteIdx - i - j)) {
				j--;
			}
			if (j < 0) {
				return ((hMaxByteIdx - i) >>> 1) - (nlen - 1);
			}
			i += nMaxByteIdx;
			i -= work[getByteBackward(haystack, hMaxByteIdx - i)] - 1;
		}
		return -1;
	}

	final static int lastIndexOf(CharSequence haystack, int hoff, int hlen, char[] needle, int noff, int nlen, int[] work) {
		// needle- and haystack-bytes are simply considered in reverse order
		// pre-process needle
		int nMaxIdx = noff + nlen - 1;
		int nBytes = nlen << 1;
		int hBytes = hlen << 1;
		int hByteOff = hoff << 1;
		int nByteOff = noff << 1;
		int nMaxByteIdx = nBytes - 1;
		int hMaxByteIdx = hByteOff + hBytes - 1;
		for (int i = nMaxIdx, j = 0; i > noff; i--) {
			int c = needle[i];
			work[c >>> 8] = ++j;
			work[c & 0xff] = ++j;
		}
		work[needle[0] >>> 8] = nMaxByteIdx;
		// search
		for (int max = hBytes - nBytes, i = 0; i <= max;) {
			int j = nMaxByteIdx;
			while (j >= 0 && getByteBackward(needle, (nByteOff + nMaxByteIdx - j)) == getByteBackward(haystack, hMaxByteIdx - i - j)) {
				j--;
			}
			if (j < 0) {
				return ((hMaxByteIdx - i) >>> 1) - (nlen - 1);
			}
			i += nMaxByteIdx;
			i -= work[getByteBackward(haystack, hMaxByteIdx - i)] - 1;
		}
		return -1;
	}

	final static int lastIndexOf(CharSequence haystack, int hoff, int hlen, CharSequence needle, int noff, int nlen, int[] work) {
		// needle- and haystack-bytes are simply considered in reverse order
		// pre-process needle
		int nMaxIdx = noff + nlen - 1;
		int nBytes = nlen << 1;
		int hBytes = hlen << 1;
		int hByteOff = hoff << 1;
		int nByteOff = noff << 1;
		int nMaxByteIdx = nBytes - 1;
		int hMaxByteIdx = hByteOff + hBytes - 1;
		for (int i = nMaxIdx, j = 0; i > noff; i--) {
			int c = needle.charAt(i);
			work[c >>> 8] = ++j;
			work[c & 0xff] = ++j;
		}
		work[needle.charAt(0) >>> 8] = nMaxByteIdx;
		// search
		for (int max = hBytes - nBytes, i = 0; i <= max;) {
			int j = nMaxByteIdx;
			while (j >= 0 && getByteBackward(needle, (nByteOff + nMaxByteIdx - j)) == getByteBackward(haystack, hMaxByteIdx - i - j)) {
				j--;
			}
			if (j < 0) {
				return ((hMaxByteIdx - i) >>> 1) - (nlen - 1);
			}
			i += nMaxByteIdx;
			i -= work[getByteBackward(haystack, hMaxByteIdx - i)] - 1;
		}
		return -1;
	}

	final static int indexOf(char[] haystack, int hoff, int hlen, char[] needle, int noff, int nlen, int[] work) {
		// pre-process needle
		int nMaxIdx = noff + nlen - 1;
		int nBytes = nlen << 1;
		int hBytes = hlen << 1;
		int hByteOff = hoff << 1;
		int nByteOff = noff << 1;
		int nMaxByteIdx = nBytes - 1;
		for (int i = noff, j = 0; i < nMaxIdx; i++) {
			int c = needle[i];
			work[c >>> 8] = ++j;
			work[c & 0xff] = ++j;
		}
		work[needle[nMaxIdx] >>> 8] = nMaxByteIdx;
		// search
		for (int max = hByteOff + hBytes - nBytes, i = hByteOff; i <= max;) {
			int j = nMaxByteIdx;
			while (j >= 0 && getByteForward(needle, nByteOff + j) == getByteForward(haystack, i + j)) {
				j--;
			}
			if (j < 0) {
				return i >>> 1;
			}
			i += nMaxByteIdx;
			i -= work[getByteForward(haystack, i)] - 1;
		}
		return -1;
	}

	final static int indexOf(char[] haystack, int hoff, int hlen, CharSequence needle, int noff, int nlen, int[] work) {
		// pre-process needle
		int nMaxIdx = noff + nlen - 1;
		int nBytes = nlen << 1;
		int hBytes = hlen << 1;
		int hByteOff = hoff << 1;
		int nByteOff = noff << 1;
		int nMaxByteIdx = nBytes - 1;
		for (int i = noff, j = 0; i < nMaxIdx; i++) {
			int c = needle.charAt(i);
			work[c >>> 8] = ++j;
			work[c & 0xff] = ++j;
		}
		work[needle.charAt(nMaxIdx) >>> 8] = nMaxByteIdx;
		// search
		for (int max = hByteOff + hBytes - nBytes, i = hByteOff; i <= max;) {
			int j = nMaxByteIdx;
			while (j >= 0 && getByteForward(needle, nByteOff + j) == getByteForward(haystack, i + j)) {
				j--;
			}
			if (j < 0) {
				return i >>> 1;
			}
			i += nMaxByteIdx;
			i -= work[getByteForward(haystack, i)] - 1;
		}
		return -1;
	}

	final static int indexOf(CharSequence haystack, int hoff, int hlen, char[] needle, int noff, int nlen, int[] work) {
		// pre-process needle
		int nMaxIdx = noff + nlen - 1;
		int nBytes = nlen << 1;
		int hBytes = hlen << 1;
		int hByteOff = hoff << 1;
		int nByteOff = noff << 1;
		int nMaxByteIdx = nBytes - 1;
		for (int i = noff, j = 0; i < nMaxIdx; i++) {
			int c = needle[i];
			work[c >>> 8] = ++j;
			work[c & 0xff] = ++j;
		}
		work[needle[nMaxIdx] >>> 8] = nMaxByteIdx;
		// search
		for (int max = hByteOff + hBytes - nBytes, i = hByteOff; i <= max;) {
			int j = nMaxByteIdx;
			while (j >= 0 && getByteForward(needle, nByteOff + j) == getByteForward(haystack, i + j)) {
				j--;
			}
			if (j < 0) {
				return i >>> 1;
			}
			i += nMaxByteIdx;
			i -= work[getByteForward(haystack, i)] - 1;
		}
		return -1;
	}

	final static int indexOf(CharSequence haystack, int hoff, int hlen, CharSequence needle, int noff, int nlen, int[] work) {
		// pre-process needle
		int nMaxIdx = noff + nlen - 1;
		int nBytes = nlen << 1;
		int hBytes = hlen << 1;
		int hByteOff = hoff << 1;
		int nByteOff = noff << 1;
		int nMaxByteIdx = nBytes - 1;
		for (int i = noff, j = 0; i < nMaxIdx; i++) {
			int c = needle.charAt(i);
			work[c >>> 8] = ++j;
			work[c & 0xff] = ++j;
		}
		work[needle.charAt(nMaxIdx) >>> 8] = nMaxByteIdx;
		// search
		for (int max = hByteOff + hBytes - nBytes, i = hByteOff; i <= max;) {
			int j = nMaxByteIdx;
			while (j >= 0 && getByteForward(needle, nByteOff + j) == getByteForward(haystack, i + j)) {
				j--;
			}
			if (j < 0) {
				return i >>> 1;
			}
			i += nMaxByteIdx;
			i -= work[getByteForward(haystack, i)] - 1;
		}
		return -1;
	}
}
