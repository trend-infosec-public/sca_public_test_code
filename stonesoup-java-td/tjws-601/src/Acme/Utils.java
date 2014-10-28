// Utils - assorted static utility routines
//
// Copyright (C)1996,1998 by Jef Poskanzer <jef@acme.com>.  All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.
//
// Visit the ACME Labs Java page for up-to-date versions of this and other
// fine Java utilities: http://www.acme.com/java/
//
// Base64 code borrowed from public domain supported by Robert Harder
// Please visit <a href="http://iharder.net/base64">http://iharder.net/base64</a>
// periodically to check for updates or to contribute improvements.
//
// $Id: Utils.java,v 1.10 2005/12/23 10:20:11 drogatkin Exp $

package Acme;

import java.util.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;

/// Assorted static utility routines.
// <P>
// Whenever I come up with a static routine that might be of general use,
// I put it here.  So far the class includes:
// <UL>
// <LI> some string routines that were left out of java.lang.String
// <LI> a general array-to-string routine
// <LI> a fixed version of java.io.InputStream's byte-array read routine
// <LI> a bunch of URL-hacking routines
// <LI> some easy-to-use wrappers for Runtime.exec
// <LI> a debugging routine to dump the current call stack
// <LI> a URLDecoder to match java.net.URLEncoder
// </UL>
// and lots more.
// <P>
// <A HREF="/resources/classes/Acme/Utils.java">Fetch the software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.Z">Fetch the entire Acme package.</A>

public class Utils {

	// / Returns a date string formatted in Unix ls style - if it's within
	// six months of now, Mmm dd hh:ss, else Mmm dd yyyy.
	static final SimpleDateFormat shortfmt = new SimpleDateFormat("MMM dd HH:mm");

	static final SimpleDateFormat longfmt = new SimpleDateFormat("MMM dd yyyy");

	public static String lsDateStr(Date date) {
		if (Math.abs(System.currentTimeMillis() - date.getTime()) < 183L * 24L * 60L * 60L * 1000L)
			return shortfmt.format(date);
		else
			return longfmt.format(date);
	}

	public static Hashtable parseQueryString(String query, String encoding) {
		Hashtable result = new Hashtable();
		StringTokenizer st = new StringTokenizer(query, "&");
		while (st.hasMoreTokens()) {
			String pair = st.nextToken();
			int ep = pair.indexOf('=');
			String key = ep > 0 ? pair.substring(0, ep) : pair;
			String value = ep > 0 ? pair.substring(ep + 1) : null;
			if (encoding == null)
				encoding = "UTF-8";
			try {
				key = /*URLDecoder.*/decode(key, encoding);
				if (value != null)
					value = /*URLDecoder.*/decode(value, encoding);
			} catch (UnsupportedEncodingException uee) {
			}
			String[] values = (String[]) result.get(key);
			String[] newValues;
			if (values == null) {
				newValues = new String[1];
				newValues[0] = value;
			} else {
				newValues = new String[values.length + 1];
				System.arraycopy(values, 0, newValues, 0, values.length);
				newValues[values.length] = value;
			}
			result.put(key, newValues);
		}
		return result;
	}

	public static Map parsePostData(long len, InputStream is, String encoding, String[] cachedStream)
			throws IOException {
		// TODO: handle parsing data over 2 GB
		byte[] buf = new byte[(int) len];
		int fp = 0;
		while (fp < len) {
			int c = is.read(buf, fp, buf.length - fp);
			if (c < 0)
				break;
			fp += c;
		}
		if (cachedStream != null && cachedStream.length > 0)
			return parseQueryString(cachedStream[0] = new String(buf, 0, fp, "iso-8859-1"), encoding);
		else
			return parseQueryString(new String(buf, 0, fp, "iso-8859-1"), encoding);
	}

	/**
	 * Decodes URL encoded string including newly introduced JavaScript encoding with %uxxxx chars
	 * 
	 * @param s
	 *            encoded string
	 * @param enc
	 *            source encoding
	 * @return decoded string or original if no decoding required
	 * @throws UnsupportedEncodingException
	 */
	public static String decode(String s, String enc) throws UnsupportedEncodingException {
		if (enc == null || enc.length() == 0) {
			throw new UnsupportedEncodingException("decode: no source char encoding provided.");
		}
		boolean decoded = false;
		int l = s.length();
		StringBuffer sb = new StringBuffer(l > 1024 ? l / 3 : l);

		int state = sText;
		int i = 0;
		int code = 0;
		char c;
		int pos = 0;
		int ofs = 0;
		byte[] buf = null;
		boolean processDig = false;
		while (i < l) {
			c = s.charAt(i);
			switch (c) {
			case '+':
				decoded = true;
				if (state == sText)
					sb.append(' ');
				else if (state == s2Dig) {
					sb.append(new String(buf, 0, pos+1, enc));
					state = sText;
					sb.append(' ');
				} else
					new IllegalArgumentException("decode: unexpected + at pos: " + i + ", of : " + s);
				break;
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				ofs = '0';
				processDig = true;
				break;
			case 'a':
			case 'b':
			case 'c':
			case 'd':
			case 'e':
			case 'f':
				ofs = 'a' - 10;
				processDig = true;
				break;
			case 'A':
			case 'B':
			case 'C':
			case 'D':
			case 'E':
			case 'F':
				ofs = 'A' - 10;
				processDig = true;
				break;
			case '%':
				decoded = true;
				if (state == sText) {
					state = sEscape;
					if (buf == null)
						buf = new byte[(l - i) / 3];
					pos = 0;
				} else if (state == s2Dig) {
					state = sEscape;
					pos++;
				} else
					new IllegalArgumentException("decode: unexpected escape % at pos: " + i + ", of : " + s);
				break;
			case 'u':
				if (state == sEscape) {
					if (pos > 0) {
						sb.append(new String(buf, 0, pos, enc));
						pos = 0;
					}
					state = sU1;
				} else if (state == sText) {
					sb.append(c);
				} else if (state == s2Dig) {
					sb.append(new String(buf, 0, pos+1, enc));
					state = sText;
					sb.append(c);
				} else
					new IllegalArgumentException("decode: unexpected char in hex at pos: " + i + ", of : " + s);
				break;
			default:
				if (state == sText)
					sb.append(c);
				else if (state == s2Dig) {
					sb.append(new String(buf, 0, pos+1, enc));
					state = sText;
					sb.append(c);
				} else
					new IllegalArgumentException("decode: unexpected char in hex at pos: " + i + ", of : " + s);

				break;
			}
			i++;
			if (processDig) {
				if (state == sEscape) {
					code = c - ofs;
					state = s1Dig;
				} else if (state == s1Dig) {
					buf[pos] = (byte) (code * 16 + (c - ofs));
					state = s2Dig;
				} else if (state == s2Dig) { // escape finished
					sb.append(new String(buf, 0, pos+1, enc));
					state = sText;
					sb.append(c);
				} else if (state == sU1) {
					code = c - ofs;
					state = sU2;
				} else if (state == sU2) {
					code = code * 16 + c - ofs;
					state = sU3;
				} else if (state == sU3) {
					code = code * 16 + c - ofs;
					state = sU4;
				} else if (state == sU4) {
					sb.append((char) (code * 16 + c - ofs));
					state = sText;
				} else
					sb.append(c);
				processDig = false;
			}
		}
		if (state == s2Dig)
			sb.append(new String(buf, 0, pos+1, enc));
		return (decoded ? sb.toString() : s);
	}

	private static final int sText = 0;

	private static final int s1Dig = 1;

	private static final int s2Dig = 2;

	private static final int sEscape = 3;

	private static final int sU1 = 4;

	private static final int sU2 = 5;

	private static final int sU3 = 6;

	private static final int sU4 = 7;
	
	// / Returns the length of the initial segment of str which consists
	// entirely of characters from charSet.
	public static int strSpan(String str, String charSet) {
		return strSpan(str, charSet, 0);
	}

	// / Returns the length of the initial segment of str which consists
	// entirely of characters from charSet, starting at the given index.
	public static int strSpan(String str, String charSet, int fromIdx) {
		int i;
		for (i = fromIdx; i < str.length(); ++i)
			if (charSet.indexOf(str.charAt(i)) == -1)
				break;
		return i - fromIdx;
	}

	// / Returns the length of the initial segment of str which consists
	// entirely of characters NOT from charSet.
	public static int strCSpan(String str, String charSet) {
		return strCSpan(str, charSet, 0);
	}

	// / Returns the length of the initial segment of str which consists
	// entirely of characters NOT from charSet, starting at the given index.
	public static int strCSpan(String str, String charSet, int fromIdx) {
		int i;
		for (i = fromIdx; i < str.length(); ++i)
			if (charSet.indexOf(str.charAt(i)) != -1)
				break;
		return i - fromIdx;
	}

	// / Checks whether a string matches a given wildcard pattern.
	// Only does ? and *, and multiple patterns separated by |.
	public static boolean match(String pattern, String string) {
		for (int p = 0;; ++p) {
			for (int s = 0;; ++p, ++s) {
				boolean sEnd = (s >= string.length());
				boolean pEnd = (p >= pattern.length() || pattern.charAt(p) == '|');
				if (sEnd && pEnd)
					return true;
				if (sEnd || pEnd)
					break;
				if (pattern.charAt(p) == '?')
					continue;
				if (pattern.charAt(p) == '*') {
					int i;
					++p;
					for (i = string.length(); i >= s; --i)
						if (match(pattern.substring(p), string.substring(i))) /*
																				 * not quite right
																				 */
							return true;
					break;
				}
				if (pattern.charAt(p) != string.charAt(s))
					break;
			}
			p = pattern.indexOf('|', p);
			if (p == -1)
				return false;
		}
	}

	// / Finds the maximum length of a string that matches a given wildcard
	// pattern. Only does ? and *, and multiple patterns separated by |.
	public static int matchSpan(String pattern, String string) {
		int result = 0;
		StringTokenizer st = new StringTokenizer(pattern, "|");

		while (st.hasMoreTokens()) {
			int len = matchSpan1(st.nextToken(), string);
			if (len > result)
				result = len;
		}
		return result;
	}

	static int matchSpan1(String pattern, String string) {
		int p = 0;
		for (; p < string.length() && p < pattern.length(); p++) {
			if (pattern.charAt(p) == string.charAt(p))
				continue;
			if (pattern.charAt(p) == '*')
				return p - 1;
			return 0;
		}
		return p < (pattern.length() - 1) ? -1 : p;
	}

	// / Returns the length of the initial segment of str1 that equals str2.
	public static int sameSpan(String str1, String str2) {
		int i;
		for (i = 0; i < str1.length() && i < str2.length() && str1.charAt(i) == str2.charAt(i); ++i)
			;
		return i;
	}

	// / Turns a String into an array of Strings, by using StringTokenizer
	// to split it up at whitespace.
	public static String[] splitStr(String str) {
		StringTokenizer st = new StringTokenizer(str);
		int n = st.countTokens();
		String[] strs = new String[n];
		for (int i = 0; i < n; ++i)
			strs[i] = st.nextToken();
		return strs;
	}

	// / Turns a String into an array of Strings, by splitting it at
	// the specified character. This does not use StringTokenizer,
	// and therefore can handle empty fields.
	public static String[] splitStr(String str, char delim) {
		int n = 1;
		int index = -1;
		while (true) {
			index = str.indexOf(delim, index + 1);
			if (index == -1)
				break;
			++n;
		}
		String[] strs = new String[n];
		index = -1;
		for (int i = 0; i < n - 1; ++i) {
			int nextIndex = str.indexOf(delim, index + 1);
			strs[i] = str.substring(index + 1, nextIndex);
			index = nextIndex;
		}
		strs[n - 1] = str.substring(index + 1);
		return strs;
	}

	// / Turns an array of Strings into a single String, with the components
	// separated by spaces.
	public static String flattenStrarr(String[] strs) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < strs.length; ++i) {
			if (i > 0)
				sb.append(' ');
			sb.append(strs[i]);
		}
		return sb.toString();
	}

	// / Sorts an array of Strings.
	// Java currently has no general sort function. Sorting Strings is
	// common enough that it's worth making a special case.
	public static void sortStrings(String[] strings) {
		// Just does a bubblesort.
		for (int i = 0; i < strings.length - 1; ++i) {
			for (int j = i + 1; j < strings.length; ++j) {
				if (strings[i].compareTo(strings[j]) > 0) {
					String t = strings[i];
					strings[i] = strings[j];
					strings[j] = t;
				}
			}
		}
	}

	// / Locates a String in an array of Strings.
	// Returns -1 if the String is not found.
	public static int indexOfString(String[] strings, String string) {
		for (int i = 0; i < strings.length; ++i)
			if (string.equals(strings[i]))
				return i;
		return -1;
	}

	// / Locates a String in an array of Strings, ignoring case.
	// Returns -1 if the String is not found.
	public static int indexOfStringIgnoreCase(String[] strings, String string) {
		for (int i = 0; i < strings.length; ++i)
			if (string.equalsIgnoreCase(strings[i]))
				return i;
		return -1;
	}

	// / Compares two arrays of Strings for equality.
	public static boolean equalsStrings(String[] strings1, String[] strings2) {
		if (strings1.length != strings2.length)
			return false;
		for (int i = 0; i < strings1.length; ++i)
			if (!strings1[i].equals(strings2[i]))
				return false;
		return true;
	}

	// / Copy the input to the output until EOF.
	public static void copyStream(InputStream in, OutputStream out) throws IOException {
		byte[] buf = new byte[4096];
		int len;
		while ((len = in.read(buf)) != -1)
			out.write(buf, 0, len);
	}

	// / Copy the input to the output until EOF.
	public static void copyStream(Reader in, Writer out) throws IOException {
		char[] buf = new char[4096];
		int len;
		while ((len = in.read(buf)) != -1)
			out.write(buf, 0, len);
	}

	// / Copy the input to the output until EOF.
	public static void copyStream(InputStream in, Writer out) throws IOException {
		byte[] buf1 = new byte[4096];
		char[] buf2 = new char[4096];
		int len, i;
		while ((len = in.read(buf1)) != -1) {
			for (i = 0; i < len; ++i)
				buf2[i] = (char) buf1[i];
			out.write(buf2, 0, len);
		}
	}

	// / Copy the input to the output until EOF.
	public static void copyStream(Reader in, OutputStream out) throws IOException {
		char[] buf1 = new char[4096];
		byte[] buf2 = new byte[4096];
		int len, i;
		while ((len = in.read(buf1)) != -1) {
			for (i = 0; i < len; ++i)
				buf2[i] = (byte) buf1[i];
			out.write(buf2, 0, len);
		}
	}

	protected final static String ISO_8859_1 = "ISO-8859-1";

	protected final static char BASE64ARRAY[] = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
			'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
			'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3',
			'4', '5', '6', '7', '8', '9', '+', '/' };

	/**
	 * base 64 encoding, string converted to bytes using specified encoding
	 * 
	 * @param String
	 *            <val>_s</val> original string to encode
	 * @param String
	 *            encoding, can be null, then iso-8859-1 used
	 * @return String result of encoding as iso-8859-1 string<br> return null in case of invalid encoding or original string null
	 * @exception no
	 *                exceptions
	 */
	public final static String base64Encode(String _s, String _enc) {
		if (_s == null)
			return null;
		if (_enc == null)
			_enc = ISO_8859_1;
		try {
			return base64Encode(_s.getBytes(_enc));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * base 64 encoding, array of bytes converted to bytes using specified encoding
	 * 
	 * @param String
	 *            <val>_s</val> original string to encode
	 * @param String
	 *            encoding, can be null, then iso-8859-1 used
	 * @return String result of encoding as iso-8859-1 string<br>
	 * 
	 * @exception <code>NullPointerException</code> if input parameter is null
	 */
	public final static String base64Encode(byte[] _bytes) {
		StringBuffer encodedBuffer = new StringBuffer((int) (_bytes.length * 1.5));
		int i = 0;
		int pad = 0;
		while (i < _bytes.length) {
			int b1 = (0xFF & _bytes[i++]);
			int b2;
			int b3;
			if (i >= _bytes.length) {
				b2 = 0;
				b3 = 0;
				pad = 2;
			} else {
				b2 = 0xFF & _bytes[i++];
				if (i >= _bytes.length) {
					b3 = 0;
					pad = 1;
				} else
					b3 = (0xFF & _bytes[i++]);
			}
			byte c1 = (byte) (b1 >> 2);
			byte c2 = (byte) (((b1 & 0x3) << 4) | (b2 >> 4));
			byte c3 = (byte) (((b2 & 0xf) << 2) | (b3 >> 6));
			byte c4 = (byte) (b3 & 0x3f);
			encodedBuffer.append(BASE64ARRAY[c1]).append(BASE64ARRAY[c2]);
			switch (pad) {
			case 0:
				encodedBuffer.append(BASE64ARRAY[c3]).append(BASE64ARRAY[c4]);
				break;
			case 1:
				encodedBuffer.append(BASE64ARRAY[c3]).append('=');
				break;
			case 2:
				encodedBuffer.append("==");
				break;
			}
		}
		return encodedBuffer.toString();
	}

	/**
	 * Translates a Base64 value to either its 6-bit reconstruction value or a negative number indicating some other meaning.
	 */
	protected final static byte[] DECODABET = { -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 0 - 8
			-5, -5, // Whitespace: Tab and Linefeed
			-9, -9, // Decimal 11 - 12
			-5, // Whitespace: Carriage Return
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 14 -
			// 26
			-9, -9, -9, -9, -9, // Decimal 27 - 31
			-5, // Whitespace: Space
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 33 - 42
			62, // Plus sign at decimal 43
			-9, -9, -9, // Decimal 44 - 46
			63, // Slash at decimal 47
			52, 53, 54, 55, 56, 57, 58, 59, 60, 61, // Numbers zero through nine
			-9, -9, -9, // Decimal 58 - 60
			-1, // Equals sign at decimal 61
			-9, -9, -9, // Decimal 62 - 64
			0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, // Letters 'A'
			// through 'N'
			14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, // Letters 'O'
			// through 'Z'
			-9, -9, -9, -9, -9, -9, // Decimal 91 - 96
			26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, // Letters 'a'
			// through 'm'
			39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, // Letters 'n'
			// through 'z'
			-9, -9, -9, -9 // Decimal 123 - 126
	};

	protected final static byte WHITE_SPACE_ENC = -5; // Indicates white space

	// in encoding

	protected final static byte EQUALS_SIGN_ENC = -1; // Indicates equals sign

	// in encoding

	/** The equals sign (=) as a byte. */
	protected final static byte EQUALS_SIGN = (byte) '=';

	/**
	 * base 64 decoding
	 * 
	 * @param encoded
	 *            string
	 * @param encoding
	 *            used to get string bytes
	 * @return result of encoding, or null if encoding invalid or string null, or string is invalid base 64 encoding
	 */
	public final static String base64Decode(String _s, String _enc) {
		if (_s == null)
			return null;
		if (_enc == null)
			_enc = ISO_8859_1;
		try {
			return new String(decode64(_s), _enc);
		} catch (UnsupportedEncodingException uee) {
		}
		return null;
	}

	/**
	 * Decodes four bytes from array <var>source</var> and writes the resulting bytes (up to three of them) to <var>destination</var>. The source and
	 * destination arrays can be manipulated anywhere along their length by specifying <var>srcOffset</var> and <var>destOffset</var>. This method does not
	 * check to make sure your arrays are large enough to accomodate <var>srcOffset</var> + 4 for the <var>source</var> array or <var>destOffset</var> + 3
	 * for the <var>destination</var> array. This method returns the actual number of bytes that were converted from the Base64 encoding.
	 * 
	 * 
	 * @param source
	 *            the array to convert
	 * @param srcOffset
	 *            the index where conversion begins
	 * @param destination
	 *            the array to hold the conversion
	 * @param destOffset
	 *            the index where output will be put
	 * @return the number of decoded bytes converted
	 * @since 1.3
	 */
	private static int decode4to3(byte[] source, int srcOffset, byte[] destination, int destOffset) {
		// Example: Dk==
		if (source[srcOffset + 2] == EQUALS_SIGN) {
			// Two ways to do the same thing. Don't know which way I like best.
			// int outBuff = ( ( DECODABET[ source[ srcOffset ] ] << 24 ) >>> 6
			// )
			// | ( ( DECODABET[ source[ srcOffset + 1] ] << 24 ) >>> 12 );
			int outBuff = ((DECODABET[source[srcOffset]] & 0xFF) << 18)
					| ((DECODABET[source[srcOffset + 1]] & 0xFF) << 12);

			destination[destOffset] = (byte) (outBuff >>> 16);
			return 1;
		}

		// Example: DkL=
		else if (source[srcOffset + 3] == EQUALS_SIGN) {
			// Two ways to do the same thing. Don't know which way I like best.
			// int outBuff = ( ( DECODABET[ source[ srcOffset ] ] << 24 ) >>> 6
			// )
			// | ( ( DECODABET[ source[ srcOffset + 1 ] ] << 24 ) >>> 12 )
			// | ( ( DECODABET[ source[ srcOffset + 2 ] ] << 24 ) >>> 18 );
			int outBuff = ((DECODABET[source[srcOffset]] & 0xFF) << 18)
					| ((DECODABET[source[srcOffset + 1]] & 0xFF) << 12)
					| ((DECODABET[source[srcOffset + 2]] & 0xFF) << 6);

			destination[destOffset] = (byte) (outBuff >>> 16);
			destination[destOffset + 1] = (byte) (outBuff >>> 8);
			return 2;
		}

		// Example: DkLE
		else {
			try {
				// Two ways to do the same thing. Don't know which way I like
				// best.
				// int outBuff = ( ( DECODABET[ source[ srcOffset ] ] << 24 )
				// >>> 6 )
				// | ( ( DECODABET[ source[ srcOffset + 1 ] ] << 24 ) >>> 12 )
				// | ( ( DECODABET[ source[ srcOffset + 2 ] ] << 24 ) >>> 18 )
				// | ( ( DECODABET[ source[ srcOffset + 3 ] ] << 24 ) >>> 24 );
				int outBuff = ((DECODABET[source[srcOffset]] & 0xFF) << 18)
						| ((DECODABET[source[srcOffset + 1]] & 0xFF) << 12)
						| ((DECODABET[source[srcOffset + 2]] & 0xFF) << 6)
						| ((DECODABET[source[srcOffset + 3]] & 0xFF));

				destination[destOffset] = (byte) (outBuff >> 16);
				destination[destOffset + 1] = (byte) (outBuff >> 8);
				destination[destOffset + 2] = (byte) (outBuff);

				return 3;
			} catch (Exception e) {
				System.out.println("" + source[srcOffset] + ": " + (DECODABET[source[srcOffset]]));
				System.out.println("" + source[srcOffset + 1] + ": " + (DECODABET[source[srcOffset + 1]]));
				System.out.println("" + source[srcOffset + 2] + ": " + (DECODABET[source[srcOffset + 2]]));
				System.out.println("" + source[srcOffset + 3] + ": " + (DECODABET[source[srcOffset + 3]]));
				return -1;
			} // e nd catch
		}
	} // end decodeToBytes

	/**
	 * Very low-level access to decoding ASCII characters in the form of a byte array. Does not support automatically gunzipping or any other "fancy" features.
	 * 
	 * @param source
	 *            The Base64 encoded data
	 * @param off
	 *            The offset of where to begin decoding
	 * @param len
	 *            The length of characters to decode
	 * @return decoded data
	 * @since 1.3
	 */
	public static byte[] decode(byte[] source, int off, int len) {
		int len34 = len * 3 / 4;
		byte[] outBuff = new byte[len34]; // Upper limit on size of output
		int outBuffPosn = 0;

		byte[] b4 = new byte[4];
		int b4Posn = 0;
		int i = 0;
		byte sbiCrop = 0;
		byte sbiDecode = 0;
		for (i = off; i < off + len; i++) {
			sbiCrop = (byte) (source[i] & 0x7f); // Only the low seven bits
			sbiDecode = DECODABET[sbiCrop];

			if (sbiDecode >= WHITE_SPACE_ENC) // Whitesp ace,Eq ualssi gnor be
			// tter
			{
				if (sbiDecode >= EQUALS_SIGN_ENC) {
					b4[b4Posn++] = sbiCrop;
					if (b4Posn > 3) {
						outBuffPosn += decode4to3(b4, 0, outBuff, outBuffPosn);
						b4Posn = 0;

						// If that was the equals sign, break out of 'for' loop
						if (sbiCrop == EQUALS_SIGN)
							break;
					} // end if: quartet built

				} // end if: equals sign or better

			} // end if: white space, equals sign or better
			else {
				System.err.println( "Bad Base64 input character at " + i + ": " + source[i] + "(decimal)" );
				return null;
			} // end else:
		} // each input character

		byte[] out = new byte[outBuffPosn];
		System.arraycopy(outBuff, 0, out, 0, outBuffPosn);
		return out;
	} // end decode

	/**
	 * Decodes data from Base64 notation, automatically detecting gzip-compressed data and decompressing it.
	 * 
	 * @param s
	 *            the string to decode
	 * @return the decoded data
	 * @since 1.4
	 */
	public static byte[] decode64(String s) {
		byte[] bytes;
		try {
			bytes = s.getBytes(ISO_8859_1);
		} // end try
		catch (java.io.UnsupportedEncodingException uee) {
			bytes = s.getBytes();
		} // end catch
		// </change>

		// Decode
		bytes = decode(bytes, 0, bytes.length);

		// Check to see if it's gzip-compressed
		// GZIP Magic Two-Byte Number: 0x8b1f (35615)
		if (bytes != null && bytes.length >= 4) {

			int head = ((int) bytes[0] & 0xff) | ((bytes[1] << 8) & 0xff00);
			if (java.util.zip.GZIPInputStream.GZIP_MAGIC == head) {
				java.io.ByteArrayInputStream bais = null;
				java.util.zip.GZIPInputStream gzis = null;
				java.io.ByteArrayOutputStream baos = null;
				byte[] buffer = new byte[2048];
				int length = 0;

				try {
					baos = new java.io.ByteArrayOutputStream();
					bais = new java.io.ByteArrayInputStream(bytes);
					gzis = new java.util.zip.GZIPInputStream(bais);

					while ((length = gzis.read(buffer)) >= 0) {
						baos.write(buffer, 0, length);
					} // end while: reading input

					// No error? Get new bytes.
					bytes = baos.toByteArray();

				} // end try
				catch (java.io.IOException e) {
					// Just return originally-decoded bytes
				} // end catch
				finally {
					try {
						baos.close();
					} catch (Exception e) {
					}
					try {
						gzis.close();
					} catch (Exception e) {
					}
					try {
						bais.close();
					} catch (Exception e) {
					}
				} // end finally

			} // end if: gzipped
		} // end if: bytes.length >= 2

		return bytes;
	} // end decode
}
