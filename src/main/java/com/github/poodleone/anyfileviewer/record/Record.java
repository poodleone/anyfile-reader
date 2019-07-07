package com.github.poodleone.anyfileviewer.record;

import java.util.Map;

/**
 * レコードのインターフェース.
 */
public interface Record {

	/**
	 * @return 内部処理用の項目のMap
	 */
	Map<String, Object> getInnerItems();

	/**
	 * @return メタデータのMap
	 */
	Map<String, Object> getMetaItems();

	/**
	 * @return 項目のMap
	 */
	Map<String, Object> getItems();

	/**
	 * @return レコードの長さ
	 */
	int getLength();

	/**
	 * @return レコードの生データ
	 */
	<T> T getRawData();

	/**
	 * レコードの項目の値を取得します.
	 * 
	 * @param name 項目名
	 * @return 項目の値
	 */
	String getValue(String name);

	/**
	 * レコードの項目の値をHEX形式で取得します.
	 * 
	 * @param name 項目名
	 * @return 項目の値
	 */
	String getHexValue(String name);
}