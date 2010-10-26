package com.affymetrix.genometryImpl.util;

public final class NibbleIterator implements SearchableCharIterator {
	private final int length;
	private final byte[] nibble_array;

	public NibbleIterator(byte[] nibs, int len) {
		this.length = len;
		this.nibble_array = nibs;
	}

	private static final char[] nibble2char = {
		'A', 'C', 'G', 'T',
		'N', 'M', 'R', 'W',
		'S', 'Y', 'K', 'V',
		'H', 'D', 'B', 'U'};
	private static final byte[] char2nibble = new byte[256];

	static {
		char2nibble['A'] = char2nibble['a'] = 0;
		char2nibble['C'] = char2nibble['c'] = 1;
		char2nibble['G'] = char2nibble['g'] = 2;
		char2nibble['T'] = char2nibble['t'] = 3;
		char2nibble['N'] = char2nibble['n'] = 4;
		char2nibble['M'] = char2nibble['m'] = 5;
		char2nibble['R'] = char2nibble['r'] = 6;
		char2nibble['W'] = char2nibble['w'] = 7;
		char2nibble['S'] = char2nibble['s'] = 8;
		char2nibble['Y'] = char2nibble['y'] = 9;
		char2nibble['K'] = char2nibble['k'] = 10;
		char2nibble['V'] = char2nibble['v'] = 11;
		char2nibble['H'] = char2nibble['h'] = 12;
		char2nibble['D'] = char2nibble['d'] = 13;
		char2nibble['B'] = char2nibble['b'] = 14;
		char2nibble['U'] = char2nibble['u'] = 15;
	}

	// 0x0f   -->  0000 0000 0000 0000 0000 0000 0000 1111
	// 0xf0   -->  0000 0000 0000 0000 0000 0000 1111 0000

	// AND with hifilter to filter out 4 hi bits (only lo bits retained)
	private static final int hifilter = 0x0f;
	// AND with lofilter to filter out 4 lo bits (only hi bits retained)
	private static final int lofilter = 0xf0;

	// number of bits to shift when converting hinibble and lonibble
	//   (4 bits for hinibble, 0 bits for lonibble)
	private static final int offsets[] = {4, 0};

	private static final int filters[] = {lofilter, hifilter};
	private static final int one_mask = 1;

	/**
	 *  BEGIN
	 *  CharacterIterator implementation
	 */
	private char charAt(int pos) {
		return charAt(nibble_array,pos);
	}
	private static char charAt(byte[]nibbles, int pos) {
		int index = pos & one_mask;  // either 0 or 1, index into offsets and filters arrays
		int offset = offsets[index];
		int filter = filters[index];
		byte by = nibbles[pos >> 1];

		int arrIndex = (by & filter) >>> offset;
		return nibble2char[arrIndex];
	}

	public String substring(int start, int end) {
		return nibblesToString(nibble_array, start, end);
	}

	public int getLength() {
		return length;
	}

	public int indexOf(String str, int fromIndex) {
		char querychars[] = str.toCharArray();
		int max = length - str.length();
		if (fromIndex >= length) {
			if (length == 0 && fromIndex == 0 && str.length() == 0) {
				/* There is an empty string at index 0 in an empty string. */
				return 0;
			}
			/* Note: fromIndex might be near -1>>>1 */
			return -1;
		}
		if (fromIndex < 0) {
			fromIndex = 0;
		}
		if (str.length() == 0) {
			return fromIndex;
		}

		int strOffset = 0;
		char first = querychars[strOffset];
		int i = fromIndex;

		startSearchForFirstChar:
		while (true) {

			/* Look for first character. */
			while (i <= max && this.charAt(i) != first) {
				i++;
			}
			if (i > max) {
				return -1;
			}

			/* Found first character, now look at the rest of querychars */
			int j = i + 1;
			int end = j + str.length() - 1;
			int k = strOffset + 1;
			while (j < end) {
				if (this.charAt(j++) != querychars[k++]) {
					i++;
					/* Look for str's first char again. */
					continue startSearchForFirstChar;
				}
			}
			return i;
		}
	}

	public static byte[] stringToNibbles(String str, int start, int end) {
		if (start >= end) {
			System.out.println("in NibbleIterator.stringToNibbles(), " +
							"start >= end NOT YET IMPLEMENTED");
			return null;
		}

		int length = end - start;
		int extra_nibble = length & one_mask;
		byte[] nibbles = new byte[(length / 2) + extra_nibble];

		for (int i = 0; i < length - 1; i++) {
			int byte_index = i >> 1;
			char ch1 = str.charAt(i + start);
			i++;
			char ch2 = str.charAt(i + start);

			byte hinib = char2nibble[ch1];
			byte lonib = char2nibble[ch2];
			byte two_nibbles = (byte) ((hinib << 4) + lonib);
			nibbles[byte_index] = two_nibbles;
		}
		if (extra_nibble > 0) {
			int byte_index = (length - 1) >> 1;
			char ch1 = str.charAt(length - 1 + start);
			byte hinib = char2nibble[ch1];
			byte singlet_nibble = (byte) (hinib << 4);
			nibbles[byte_index] = singlet_nibble;
		}
		return nibbles;
	}

	public static String nibblesToString(byte[] nibbles, int start, int end) {
		String residues = null;
		boolean forward = (start <= end);
		int min = Math.min(start, end);
		int max = Math.max(start, end);
		char[] charArr = nibblesToCharArr(nibbles,min,max);
		residues = new String(charArr);
		charArr = null;
		if (!forward) {
			residues = DNAUtils.reverseComplement(residues);
		}
		return residues;
	}

	public static char[] nibblesToCharArr(byte[] nibbles, int start, int end) {
		char[]charArr = new char[end-start];
		for (int pos = start; pos < end; pos++) {
			charArr[pos-start] = charAt(nibbles,pos);
		}
		return charArr;
	}
}
