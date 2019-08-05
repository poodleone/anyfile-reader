package com.github.poodleone.anyfileviewer.record;

/**
 * レコード項目を表すインターフェース.
 */
public interface RecordItem {
	/**
	 * @return 式評価前の文字列の値
	 */
	String toRawString();
	
	/**
	 * @return HEX表記の値
	 */
	String toHexString();
}