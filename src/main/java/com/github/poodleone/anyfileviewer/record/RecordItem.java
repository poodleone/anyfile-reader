package com.github.poodleone.anyfileviewer.record;

/**
 * レコード項目を表すインターフェース.
 */
public interface RecordItem {
	/**
	 * @return HEX表記の値
	 */
	String toHexString();
}