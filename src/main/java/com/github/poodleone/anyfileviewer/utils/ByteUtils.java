package com.github.poodleone.anyfileviewer.utils;

import javax.xml.bind.DatatypeConverter;

/**
 * byteデータの操作ユーティリティ.
 *
 */
public class ByteUtils {
	private static final char[] hexCode = "0123456789ABCDEF".toCharArray();

	/**
	 * byte配列を文字列に変換します.
	 * 
	 * @param data   byte配列
	 * @param offset 変換対象のオフセット
	 * @param length 変換対象の長さ
	 * @return HEX表記の文字列
	 */
	public static String printHexBinary(byte[] data, int offset, int length) {
		StringBuilder r = new StringBuilder(data.length * 2);
		for (int i = offset; i < offset + length; i++) {
			r.append(hexCode[(data[i] >> 4) & 0xF]);
			r.append(hexCode[(data[i] & 0xF)]);
		}
		return r.toString();
	}

	/**
	 * HEX表記のバイナリデータをbyte配列にパースします.
	 * 
	 * @param hexBinary HEX表記のバイナリデータ
	 * @return byte配列
	 */
	public static byte[] parseHexBinary(String hexBinary) {
		return DatatypeConverter.parseHexBinary(hexBinary);
	}
}
